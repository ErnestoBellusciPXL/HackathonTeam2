<script setup lang="ts">
import Button3D from "@/components/Button3D.vue";
import MovingInput from "@/components/MovingInput.vue";
import SearchDropdown from "@/components/SearchDropdown.vue";
// InfoTooltipButton is used inside input components; not needed here
import { useZipCodeStore } from "@/stores/zipcode";
import { useAuthStore } from "@/stores/auth";
import { useConditionsStore } from "@/stores/conditions";
import type { Condition } from "@/utils/Condition";
import { useToastStore } from "@/stores/toast";
import { useRouter } from "vue-router";
import { computed, ref } from "vue";
import type { ZipcodeEntry } from "@/utils/ZipCode";
import SidePanelImage from "@/components/SidePanelImage.vue";

const authStore = useAuthStore();
const conditionsStore = useConditionsStore();
const allConditions = ref<Condition[]>([]);
const zipcodesStore = useZipCodeStore();
const zipcodesList = ref<ZipcodeEntry[]>([]);
const toast = useToastStore();
const router = useRouter();

const username = ref("");
const email = ref("");
const password = ref("");
const passwordRepeat = ref("");
const termsCheck = ref(false);
const errorMessage = ref<string | null>(null);
const showSecondForm = ref(false);

const zipcode = ref("");
const selectedConditions = ref<string[]>([]);
const hasCondition = ref(false);
const conditionQuery = ref("");
const randomSuggestions = ref<string[]>([]);
const secondFormError = ref<string | null>(null);

function formatGemeenteOrGemeenten(z: ZipcodeEntry) {
    if (z.gemeente) return z.gemeente as string;
    if (Array.isArray(z.gemeenten)) return z.gemeenten.join(", ");
    if (typeof z.gemeenten === "string") return z.gemeenten;
    return "";
}

const zipcodeItems = computed(() => {
    const seen = new Set<string>();
    const items: { value: string; label: string }[] = [];
    for (const z of zipcodesList.value) {
        const place = formatGemeenteOrGemeenten(z);
        const label = place ? `${place} (${z.code})` : `${z.code}`;
        const key = `${z.code}|${label}`;
        if (!seen.has(key)) {
            seen.add(key);
            items.push({ value: z.code, label });
        }
    }
    return items;
});

const allConditionNames = computed(() => allConditions.value.map((c) => c.name));

const filteredConditions = computed(() => {
    const q = conditionQuery.value.trim().toLowerCase();
    // pool: all condition names that are not already selected
    const pool = allConditionNames.value.filter((n) => !selectedConditions.value.includes(n));
    // If there's no query, show the full pool (all available conditions)
    if (!q) {
        return pool;
    }
    // Otherwise filter by query
    return pool.filter((n) => n.toLowerCase().includes(q));
});

function validateForm() {
    if (!username.value || !email.value || !password.value || !passwordRepeat.value) {
        errorMessage.value = "Vul alle velden in.";
        toast.error(errorMessage.value!);
        return false;
    }

    // username: only letters, digits, underscore and hyphen; length between 2 and 40
    const usernameRegex = /^[A-Za-z0-9]{2,40}$/;
    if (!usernameRegex.test(username.value)) {
        errorMessage.value = "Gebruikersnaam moet 2 – 40 tekens zijn en mag alleen letters en cijfers.";
        toast.error(errorMessage.value!);
        return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailRegex.test(email.value)) {
        errorMessage.value = "Voer een geldig e-mailadres in.";
        toast.error(errorMessage.value!);
        return false;
    }

    if (email.value.length > 320) {
        errorMessage.value = "E-mailadres mag maximaal 320 tekens bevatten.";
        toast.error(errorMessage.value!);
        return false;
    }

    if (password.value !== passwordRepeat.value) {
        errorMessage.value = "Wachtwoorden komen niet overeen.";
        toast.error(errorMessage.value!);
        return false;
    }

    if (password.value.length < 8) {
        errorMessage.value = "Wachtwoord moet minstens 8 tekens lang zijn.";
        toast.error(errorMessage.value!);
        return false;
    }

    const passwordRegex = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*?,./+=\\\-_])/;
    if (!passwordRegex.test(password.value)) {
        errorMessage.value =
            "Wachtwoord moet minstens één hoofdletter, één cijfer en één speciaal teken bevatten.";
        toast.error(errorMessage.value!);
        return false;
    }

    if (!termsCheck.value) {
        errorMessage.value = "Je moet akkoord gaan met de privacyverklaring.";
        toast.error(errorMessage.value!);
        return false;
    }

    errorMessage.value = null;
    return true;
}

function generateRandomSuggestions(conditionNames: string[], count: number): string[] {
    const max = Math.min(count, conditionNames.length);
    const pool = [...conditionNames];
    const picks: string[] = [];

    while (picks.length < max && pool.length) {
        const idx = Math.floor(Math.random() * pool.length);
        const removed = pool.splice(idx, 1);
        const pick = removed[0];
        if (pick !== undefined) picks.push(pick);
    }

    return picks;
}

async function loadConditions() {
    if (allConditions.value.length > 0) {
        if (randomSuggestions.value.length === 0) {
            randomSuggestions.value = generateRandomSuggestions(allConditionNames.value, 12);
        }
        return;
    }

    const res = await conditionsStore.fetchAll();
    if (Array.isArray(res)) {
        allConditions.value = res;
        randomSuggestions.value = generateRandomSuggestions(allConditionNames.value, 12);
    } else {
        console.error("Failed to load conditions:", res);
    }
}

async function loadZipcodes() {
    if (zipcodesList.value.length > 0) return;

    const res = await zipcodesStore.fetchAll();
    if (Array.isArray(res)) {
        zipcodesList.value = res;
    } else {
        console.error("Failed to load zipcodes", res);
    }
}

async function handleSubmit() {
    if (!validateForm()) return;

    const exists = await authStore.checkIfUsernameOrEmailExists(username.value, email.value);

    if (exists) {
        errorMessage.value = "Gebruiker met deze gegevens bestaat al.";
        toast.error(errorMessage.value!);
        return;
    }

    showSecondForm.value = true;
    await Promise.all([loadConditions(), loadZipcodes()]);
    secondFormError.value = null;
}

function validateSecondForm() {
    const zip = zipcode.value.trim();
    const listHasItems = zipcodeItems.value.length > 0;
    // If we have zipcode items available, require the zipcode to be one of those items.
    // Otherwise fallback to the previous loose regex validation.
    const zipOk = listHasItems
        ? zipcodeItems.value.some((item) => item.value === zip)
        : /^[A-Za-z0-9\s-]{3,10}$/.test(zip);

    // show a single generic message when required fields are missing
    if (!zipOk || !selectedConditions.value.length) {
        secondFormError.value = "Vul alle velden in.";
        toast.error(secondFormError.value!);
        return false;
    }

    secondFormError.value = null;
    return true;
}

async function handleSecondSubmit() {
    if (!validateSecondForm()) return;

    const payload = {
        zipcode: zipcode.value.trim(),
        conditions: selectedConditions.value,
        hasCondition: hasCondition.value,
    };

    try {
        const registerRes = await authStore.register({
            username: username.value,
            email: email.value,
            password: password.value,
        });

        if (!registerRes || registerRes.error || !registerRes.token) {
            secondFormError.value = registerRes?.error || "Registratie mislukt.";
            toast.error(secondFormError.value!);
            return;
        }

        localStorage.setItem("token", registerRes.token);
        authStore.isUserLoggedInRef = true;

        const res = await authStore.completeProfile(payload);
        if (res && typeof res === "object" && "error" in res && res.error) {
            secondFormError.value = res.error;
            toast.error(secondFormError.value!);
        } else {
            secondFormError.value = null;
            toast.success("Instellingen zijn opgeslagen.");

            if (zipcodesList.value.length === 0) {
                const zres = await zipcodesStore.fetchAll();
                if (Array.isArray(zres)) zipcodesList.value = zres;
            }

            setTimeout(() => router.push({ name: "home" }), 1000);
        }
    } catch {
        secondFormError.value = "Opslaan mislukt. Probeer opnieuw.";
        toast.error(secondFormError.value!);
    }
}

function addCondition(name: string) {
    if (!selectedConditions.value.includes(name)) {
        selectedConditions.value.push(name);
    }
}

function removeCondition(name: string) {
    selectedConditions.value = selectedConditions.value.filter((n) => n !== name);
}
</script>

<template>
    <main class="min-h-screen min-w-full grid grid-cols-1 xl:grid-cols-2 gap-4">
        <section class="min-h-screen sm:min-h-full flex flex-col items-center pt-10">
            <h1 class="font-bold text-5xl">Welkom</h1>
            <br />
            <span class="text-center px-5"
                >Deel je verhaal, vind lotgenoten en krijg toegang tot informatie die echt bij jou past.</span
            >

            <transition name="fade-slide" mode="out-in">
                <form
                    v-if="!showSecondForm"
                    @submit.prevent
                    class="flex flex-col items-center w-full max-w-[360px] sm:max-w-[560px]"
                    key="first"
                >
                    <div class="flex flex-col gap-4 mt-10 relative items-center">
                        <div class="mt-1 w-full sm:w-[560px]">
                            <MovingInput
                                id="username"
                                label="Gebruikersnaam"
                                v-model="username"
                                inputStyle="w-full"
                                :showTooltip="true"
                                tooltipText="2–40 tekens, alleen letters en cijfers. Geen spaties."
                            />
                            <div class="text-xs text-left text-gray-600 mb-1" id="username_public_info">
                                Je gebruikersnaam is publiek zichtbaar.
                            </div>
                        </div>

                        <div class="mt-1 w-full sm:w-[560px]">
                            <MovingInput
                                id="email"
                                label="E-mail"
                                v-model="email"
                                inputStyle="w-full"
                                :showTooltip="false"
                                tooltipText="Geldig formaat (bijv. naam@domein.nl), max 320 tekens."
                            />
                            <div class="text-xs text-gray-600 mb-1" id="email_private_info">
                                Je e-mailadres is enkel zichtbaar voor beheerders.
                            </div>
                        </div>

                        <div class="mt-1 w-full sm:w-[560px]">
                            <MovingInput
                                id="password"
                                label="Wachtwoord"
                                v-model="password"
                                type="password"
                                inputStyle="w-full"
                                :showTooltip="true"
                                tooltipText="Min. 8 tekens, minstens 1 hoofdletter, 1 cijfer en 1 speciaal teken (!@#$%^&*?)."
                            />
                        </div>

                        <div class="mt-1 w-full sm:w-[560px]">
                            <MovingInput
                                id="passwordRepeat"
                                label="Wachtwoord herhalen"
                                v-model="passwordRepeat"
                                type="password"
                                inputStyle="w-full"
                                :showTooltip="false"
                                tooltipText="Herhaal exact hetzelfde wachtwoord ter bevestiging."
                            />
                        </div>

                        <div class="flex items-center justify-center">
                            <input
                                type="checkbox"
                                name="terms"
                                id="terms"
                                class="me-3 w-4 h-4"
                                v-model="termsCheck"
                            />
                            <label for="terms" class="text-sm text-gray-600">
                                Ik ga akkoord met de
                                <RouterLink
                                    :to="{ name: 'privacy' }"
                                    class="text-brand-purple align-middle"
                                    target="_blank"
                                    rel="noopener noreferrer"
                                    id="NavigatePrivacyPolicyLink"
                                >
                                    privacyverklaring
                                </RouterLink>
                            </label>
                        </div>
                    </div>

                    <div class="w-3/4 mt-10 mb-10 flex flex-col">
                        <div class="flex justify-end">
                            <Button3D
                                variant="primary"
                                class="w-full block"
                                @click="handleSubmit"
                                id="loginButton"
                            >
                                Volgende <span class="ms-3 text-2xl">&rarr;</span>
                            </Button3D>
                        </div>
                    </div>

                    <div class="text-center mt-2">
                        <RouterLink
                            :to="{ name: 'login' }"
                            class="text-sm text-brand-purple"
                            id="NavigateLoginLink1"
                        >
                            Heb je al een account? Log hier in
                        </RouterLink>
                    </div>
                </form>

                <!-- Tweede form: zelfde mobile layout als eerste form -->
                <form
                    v-else
                    key="second"
                    @submit.prevent="handleSecondSubmit"
                    class="flex flex-col items-center w-full max-w-[360px] sm:max-w-[560px]"
                >
                    <h2 class="text-2xl font-semibold mb-4 self-center text-center">
                        Aanvullende instellingen
                    </h2>

                    <div class="mt-1 w-full sm:w-[560px]">
                        <SearchDropdown
                            id="zipcode"
                            v-model="zipcode"
                            :items="zipcodeItems"
                            label="Postcode / Gemeente"
                            :showTooltip="true"
                            tooltipText="Je gegevens worden veilig en vertrouwelijk verwerkt. Deze worden alleen gebruikt om lotgenoten in uw buurt te tonen."
                        />
                    </div>

                    <div class="mt-1 w-full sm:w-[560px]">
                        <MovingInput
                            id="conditionQuery"
                            label="Zoek naar aandoening..."
                            v-model="conditionQuery"
                            inputStyle="w-full"
                            :showTooltip="true"
                            tooltipText="Je gegevens worden veilig en vertrouwelijk verwerkt. Deze worden alleen gebruikt om een heatmap van aandoeningen te tonen."
                        />
                    </div>
                    <div class="text-xs text-left text-gray-600 mb-1">
                        Selecteer de aandoeningen die jou raken of waarmee jij (of iemand die je kent)
                        ervaring hebt.
                    </div>

                    <!-- Gekozen aandoeningen -->
                    <div class="mt-3 border border-gray-300 rounded-[10px] p-3 min-h-32 w-full sm:w-[560px]">
                        <div class="text-xs text-gray-700 mb-2">Gekozen aandoening(en)</div>
                        <div class="flex flex-wrap gap-2 min-h-6">
                            <span
                                v-for="name in selectedConditions"
                                :key="name"
                                class="inline-flex items-center bg-brand-purple text-white px-3 py-1 rounded-[10px] text-sm cursor-default"
                            >
                                {{ name }}
                                <button
                                    type="button"
                                    class="ml-2 w-5 h-5 inline-flex items-center justify-center bg-white/20 rounded-[10px] hover:bg-white/30"
                                    aria-label="Verwijder"
                                    @click="removeCondition(name)"
                                >
                                    ×
                                </button>
                            </span>
                            <span v-if="!selectedConditions.length" class="text-gray-500 text-sm"
                                >Nog geen aandoeningen gekozen</span
                            >
                        </div>
                    </div>

                    <!-- Suggesties -->
                    <div
                        class="mt-2 border border-gray-300 rounded-[10px] p-3 h-48 w-full sm:w-[560px] overflow-y-auto scroll-stable"
                    >
                        <div class="flex flex-wrap gap-2">
                            <button
                                v-for="name in filteredConditions"
                                :key="name"
                                type="button"
                                class="px-3 py-1 rounded-[10px] text-sm border border-brand-purple text-brand-purple bg-brand-purple/10 hover:bg-brand-purple/20"
                                @click="addCondition(name)"
                                id="FilterConditionButton"
                            >
                                {{ name }}
                            </button>
                            <span v-if="filteredConditions.length === 0" class="text-gray-500 text-sm"
                                >Geen resultaten</span
                            >
                        </div>
                    </div>

                    <div class="flex items-center self-start w-full sm:w-[560px] mt-4">
                        <input
                            type="checkbox"
                            id="hasCondition"
                            class="me-3 w-4 h-4"
                            v-model="hasCondition"
                        />
                        <label for="hasCondition" class="text-sm text-gray-600"
                            >Ik leef zelf met deze aandoening</label
                        >
                    </div>

                    <div class="w-full sm:w-[560px] mt-10 flex flex-col">
                        <div class="flex justify-end">
                            <Button3D
                                variant="primary"
                                class="w-full block"
                                @click="handleSecondSubmit"
                                id="CompleteProfileButton"
                            >
                                Profiel afronden <span class="ms-3 text-2xl">&rarr;</span>
                            </Button3D>
                        </div>
                    </div>
                    <div class="text-center mb-30 w-full sm:w-[560px]">
                        <RouterLink :to="{ name: 'login' }" class="text-sm text-brand-purple">
                            Heb je al een account? Log hier in
                        </RouterLink>
                    </div>
                </form>
            </transition>
        </section>

        <section class="relative hidden xl:flex items-stretch bg-brand-vanilla">
            <SidePanelImage class="h-full w-full" />
        </section>
    </main>
</template>

<style scoped>
.fade-slide-enter-active,
.fade-slide-leave-active {
    transition: all 300ms ease;
}

.fade-slide-enter-from {
    opacity: 0;
    transform: translateY(10px);
}

.fade-slide-enter-to {
    opacity: 1;
    transform: translateY(0);
}

.fade-slide-leave-from {
    opacity: 1;
    transform: translateY(0);
}

.fade-slide-leave-to {
    opacity: 0;
    transform: translateY(-10px);
}
</style>
