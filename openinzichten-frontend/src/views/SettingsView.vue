<script setup lang="ts">
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button3D from "@/components/Button3D.vue";
import IconUser from "@/components/icons/IconUser.vue";
import IconProfile from "@/components/icons/IconProfile.vue";
import IconSecurity from "@/components/icons/IconSecurity.vue";
import SearchDropdown from "@/components/SearchDropdown.vue";
import { useAuthStore } from "@/stores/auth";
import {
    useAccountStore,
    type AccountUpdateResponse,
    type UpdateAccountPayload,
    type UserAccountResponse,
} from "@/stores/account";
import InfoToolTipButton from "@/components/InfoTooltipButton.vue";
import { useToastStore } from "@/stores/toast";
import type { ZipcodeEntry } from "@/utils/ZipCode";
import type { Condition } from "@/utils/Condition";
import { useConditionsStore } from "@/stores/conditions";
import { useZipCodeStore } from "@/stores/zipcode";
import type { ErrorResponse } from "@/utils/Error";

type AccountFormSnapshot = {
    username: string;
    email: string;
    zipcode: string;
    conditions: string[];
    hasCondition: boolean;
};

const showDeleteModal = ref(false);
const deletePassword = ref("");
const authStore = useAuthStore();
const accountStore = useAccountStore();
const toast = useToastStore();
const router = useRouter();
const route = useRoute();
const activeTab = ref<"profile" | "security">(route.query.tab === "security" ? "security" : "profile");

const username = ref(authStore.getCurrentUser()?.sub || "");
const email = ref(authStore.getCurrentUser()?.email || "");
const zipcode = ref("");
const hasCondition = ref(false);
const selectedConditions = ref<string[]>([]);
const conditionQuery = ref("");
const conditionDropdownOpen = ref(false);
const conditionFieldRef = ref<HTMLElement | null>(null);
const conditionInputRef = ref<HTMLInputElement | null>(null);

const zipcodesList = ref<ZipcodeEntry[]>([]);
const allConditions = ref<Condition[]>([]);
const isLoadingAccount = ref(true);
const isSaving = ref(false);
const loadError = ref<string | null>(null);
const snapshot = ref<AccountFormSnapshot | null>(null);

const conditionsStore = useConditionsStore();
const zipcodesStore = useZipCodeStore();

const allConditionNames = computed(() => allConditions.value.map((c) => c.name));

const indicatorStyles = computed(() => ({
    transform: activeTab.value === "profile" ? "translateX(0%)" : "translateX(100%)",
}));

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

const filteredConditions = computed(() => {
    const q = conditionQuery.value.trim().toLowerCase();
    const pool = allConditionNames.value.filter((n) => !selectedConditions.value.includes(n));
    if (!q) return pool.slice(0, 50);
    return pool.filter((n) => n.toLowerCase().includes(q)).slice(0, 50);
});

const hasChanges = computed(() => {
    if (!snapshot.value) return false;
    const current = currentSnapshot();
    return (
        current.username !== snapshot.value.username ||
        current.email !== snapshot.value.email ||
        current.zipcode !== snapshot.value.zipcode ||
        current.hasCondition !== snapshot.value.hasCondition ||
        !arraysEqual(current.conditions, snapshot.value.conditions)
    );
});

function arraysEqual(a: string[], b: string[]): boolean {
    if (a.length !== b.length) return false;
    const left = [...a].sort();
    const right = [...b].sort();
    return left.every((val, idx) => val === right[idx]);
}

function formatGemeenteOrGemeenten(z: ZipcodeEntry) {
    if (z.gemeente) return z.gemeente as string;
    if (Array.isArray(z.gemeenten)) return z.gemeenten.join(", ");
    if (typeof z.gemeenten === "string") return z.gemeenten;
    return "";
}

function isErrorResponse(value: unknown): value is ErrorResponse {
    return Boolean(value && typeof value === "object" && "error" in (value as Record<string, unknown>));
}

function openDeleteModal() {
    showDeleteModal.value = true;
}

function closeDeleteModal() {
    showDeleteModal.value = false;
    deletePassword.value = "";
}

async function confirmDelete() {
    const userId = authStore.getCurrentUser()?.id;
    if (!userId) {
        toast.error("Je bent niet (meer) ingelogd.");
        return;
    }
    const result = await accountStore.deleteAccount(deletePassword.value, userId);

    if (!result) {
        toast.error(
            "Het verwijderen van je account is mislukt. Controleer je wachtwoord en probeer het opnieuw.",
        );
        return;
    }

    toast.success("Je account is succesvol verwijderd. Je wordt zo doorgestuurd...");
    closeDeleteModal();

    window.setTimeout(() => {
        localStorage.clear();
        authStore.reset();
        router.push({ name: "home" });
    }, 3000);
}

function logoutAndRedirect() {
    localStorage.clear();
    authStore.reset();
    router.push({ name: "home" });
}

async function loadConditions() {
    if (allConditions.value.length > 0) {
        return;
    }

    const res = await conditionsStore.fetchAll();
    if (Array.isArray(res)) {
        allConditions.value = res;
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

function currentSnapshot(): AccountFormSnapshot {
    return {
        username: username.value.trim(),
        email: email.value.trim(),
        zipcode: zipcode.value.trim(),
        conditions: [...selectedConditions.value].sort(),
        hasCondition: hasCondition.value,
    };
}

function applyAccountData(data: UserAccountResponse) {
    username.value = data.username || "";
    email.value = data.email || "";
    zipcode.value = data.zipcode || "";
    hasCondition.value = Boolean(data.hasCondition);
    selectedConditions.value = Array.isArray(data.conditions) ? [...data.conditions] : [];
    conditionQuery.value = "";
    snapshot.value = currentSnapshot();
}

async function refreshAccount() {
    isLoadingAccount.value = true;
    loadError.value = null;
    const res = await accountStore.fetchAccount();
    if (isErrorResponse(res)) {
        loadError.value = res.error;
        if (res.status === 401) logoutAndRedirect();
    } else {
        applyAccountData(res);
    }
    isLoadingAccount.value = false;
}

function buildPayload(): UpdateAccountPayload {
    const payload: UpdateAccountPayload = {};
    const base = snapshot.value;
    const current = currentSnapshot();

    if (!base || current.username !== base.username) payload.username = current.username;
    if (!base || current.email !== base.email) payload.email = current.email;
    if (!base || current.zipcode !== base.zipcode) payload.zipcode = current.zipcode;
    if (!base || current.hasCondition !== base.hasCondition) payload.hasCondition = current.hasCondition;
    if (!base || !arraysEqual(current.conditions, base.conditions)) {
        payload.conditions = [...selectedConditions.value];
    }

    return payload;
}

function validatePayload(payload: UpdateAccountPayload): string | null {
    if (payload.username !== undefined) {
        if (!payload.username.trim()) return "Gebruikersnaam mag niet leeg zijn.";
        const usernameRegex = /^[A-Za-z0-9]{2,40}$/;
        if (!usernameRegex.test(payload.username)) {
            return "Gebruikersnaam moet 2 – 40 tekens zijn en mag alleen letters en cijfers.";
        }
    }

    if (payload.email !== undefined) {
        const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
        if (!emailRegex.test(payload.email)) {
            return "Voer een geldig e-mailadres in.";
        }
    }

    if (payload.zipcode && zipcodeItems.value.length > 0) {
        const match = zipcodeItems.value.some((item) => item.value === payload.zipcode);
        if (!match) return "Kies een geldige postcode uit de lijst.";
    }

    return null;
}

async function saveSettings() {
    if (isSaving.value || isLoadingAccount.value) return;

    const payload = buildPayload();
    if (Object.keys(payload).length === 0) {
        toast.info("Geen wijzigingen om op te slaan.");
        return;
    }

    const validationError = validatePayload(payload);
    if (validationError) {
        toast.error(validationError);
        return;
    }

    isSaving.value = true;
    const res = await accountStore.updateAccount(payload);
    if (isErrorResponse(res)) {
        toast.error(res.error || "Opslaan mislukt.");
        if (res.status === 401) logoutAndRedirect();
        isSaving.value = false;
        return;
    }

    const success = res as AccountUpdateResponse;
    if (success.token) {
        authStore.setToken(success.token);
    }
    if (success.account) {
        applyAccountData(success.account);
    }

    toast.success(success.message ?? "Instellingen zijn opgeslagen.");
    isSaving.value = false;
}

function resetChanges() {
    if (!snapshot.value) return;
    username.value = snapshot.value.username;
    email.value = snapshot.value.email;
    zipcode.value = snapshot.value.zipcode;
    hasCondition.value = snapshot.value.hasCondition;
    selectedConditions.value = [...snapshot.value.conditions];
    conditionQuery.value = "";
}

function addCondition(name: string) {
    const clean = name.trim();
    if (!clean || selectedConditions.value.includes(clean)) return;
    // Only allow known condition names or items already present in the account
    if (!allConditionNames.value.includes(clean) && !snapshot.value?.conditions.includes(clean)) return;
    selectedConditions.value.push(clean);
    conditionQuery.value = "";
}

function addFirstFiltered() {
    const first = filteredConditions.value[0];
    if (first) addCondition(first);
}

function removeCondition(name: string) {
    selectedConditions.value = selectedConditions.value.filter((n) => n !== name);
}

function handleKeydown(e: KeyboardEvent) {
    if (conditionDropdownOpen.value && e.key === "Escape") {
        conditionDropdownOpen.value = false;
        if (!showDeleteModal.value) return;
    }

    if (!showDeleteModal.value) return;
    if (e.key === "Escape") {
        closeDeleteModal();
    } else if (e.key === "Delete") {
        void confirmDelete();
    }
}

watch(
    () => activeTab.value,
    (tab) => {
        router.replace({ query: { ...route.query, tab } });
    },
);

function toggleConditionDropdown() {
    conditionDropdownOpen.value = !conditionDropdownOpen.value;
    if (conditionDropdownOpen.value) {
        nextTick(() => conditionInputRef.value?.focus());
    }
}

function closeConditionDropdown() {
    conditionDropdownOpen.value = false;
}

function handleClickOutside(e: MouseEvent) {
    if (!conditionDropdownOpen.value) return;
    const target = e.target as Node | null;
    if (conditionFieldRef.value && target && !conditionFieldRef.value.contains(target)) {
        closeConditionDropdown();
    }
}

onMounted(async () => {
    await Promise.all([loadZipcodes(), loadConditions()]);
    await refreshAccount();
    window.addEventListener("keydown", handleKeydown);
    document.addEventListener("mousedown", handleClickOutside);
});

onBeforeUnmount(() => {
    window.removeEventListener("keydown", handleKeydown);
    document.removeEventListener("mousedown", handleClickOutside);
});
</script>

<template>
    <div class="container mx-auto px-4 py-8 max-w-6xl">
        <div class="mb-6">
            <p class="text-sm font-semibold text-brand-purple-dark uppercase tracking-wide">Instellingen</p>
            <h1 class="text-3xl font-bold">Beheer je account</h1>
            <p class="text-gray-600">Werk je profiel, voorwaarden en account-veiligheid bij.</p>
        </div>

        <div class="tabs-card mb-6">
            <div class="tab-indicator" :style="indicatorStyles" />
            <button
                id="profile"
                type="button"
                class="tab-button"
                :class="{ 'tab-active': activeTab === 'profile' }"
                @click="activeTab = 'profile'"
            >
                <IconProfile :class="'tab-icon' + (activeTab === 'profile' ? ' tab-active' : ' text-white')" />
                <span>Profiel</span>
            </button>

            <button
                id="security"
                type="button"
                class="tab-button"
                :class="{ 'tab-active': activeTab === 'security' }"
                @click="activeTab = 'security'"
            >
                <IconSecurity
                    :class="'tab-icon' + (activeTab === 'security' ? ' tab-active' : ' text-white')"
                />
                <span>Beveiliging</span>
            </button>
        </div>

        <div v-if="isLoadingAccount" class="bg-white border border-gray-200 rounded-2xl shadow-sm p-6">
            <p class="text-gray-700 font-medium">Accountgegevens laden...</p>
        </div>

        <div
            v-else-if="loadError"
            class="bg-red-50 border border-red-200 text-red-700 rounded-2xl shadow-sm p-6 flex flex-col gap-3"
        >
            <p class="font-semibold">Kon je account niet ophalen</p>
            <p class="text-sm text-red-800">{{ loadError }}</p>
            <div class="flex gap-3">
                <Button3D variant="primary" @click="refreshAccount">Opnieuw proberen</Button3D>
                <Button3D variant="secondary" @click="logoutAndRedirect">Terug naar start</Button3D>
            </div>
        </div>

        <template v-else>
            <template v-if="activeTab === 'profile'">
                <div class="space-y-4">
                    <section class="settings-card">
                        <div class="card-heading">
                            <div>
                                <h2>Profielinformatie</h2>
                                <p class="muted">Pas je persoonlijke gegevens aan</p>
                            </div>
                        </div>

                        <div class="form-stack">
                            <div class="form-group">
                                <div class="label-row">
                                    <label for="new_username">Gebruikersnaam</label>
                                    <InfoToolTipButton
                                        tooltipText="2–40 tekens, alleen letters en cijfers. Geen spaties."
                                    />
                                </div>
                                <input
                                    type="text"
                                    id="new_username"
                                    v-model="username"
                                    :disabled="isSaving"
                                    class="input"
                                    placeholder="Jouw zichtbare naam"
                                />
                            </div>

                            <div class="form-group">
                                <label for="new_email">E-mailadres</label>
                                <input
                                    type="email"
                                    id="new_email"
                                    v-model="email"
                                    :disabled="isSaving"
                                    class="input"
                                    placeholder="naam@domein.be"
                                />
                            </div>

                            <div class="form-group">
                                <div class="label-row">
                                    <label for="conditionQuery">Aandoening of diagnose</label>
                                    <InfoToolTipButton
                                        tooltipText="Je gegevens worden veilig en vertrouwelijk verwerkt. Deze worden alleen gebruikt om een heatmap van aandoeningen te tonen."
                                    />
                                </div>
                                <div class="condition-group" ref="conditionFieldRef">
                                    <button
                                        type="button"
                                        class="select-box"
                                        @click="toggleConditionDropdown"
                                        :aria-expanded="conditionDropdownOpen"
                                    >
                                        <div class="chip-row">
                                            <span
                                                v-for="name in selectedConditions"
                                                :key="name"
                                                class="chip"
                                            >
                                                {{ name }}
                                                <button
                                                    type="button"
                                                    class="chip-close"
                                                    @click.stop="removeCondition(name)"
                                                >
                                                    ×
                                                </button>
                                            </span>
                                            <span v-if="!selectedConditions.length" class="muted text-sm">
                                                Zoek en kies aandoeningen
                                            </span>
                                        </div>
                                        <span class="chevron">⌄</span>
                                    </button>

                                    <div v-if="conditionDropdownOpen" class="condition-dropdown" @click.stop>
                                        <input
                                            ref="conditionInputRef"
                                            id="conditionQuery"
                                            v-model="conditionQuery"
                                            type="text"
                                            placeholder="Zoek en voeg aandoeningen toe"
                                            class="input"
                                            @keydown.enter.prevent="addFirstFiltered"
                                        />
                                        <p class="muted text-sm mt-1 mb-2">
                                            Klik een item om het toe te voegen. Je selectie wordt exact opgeslagen.
                                        </p>
                                        <div class="suggestions dropdown-list">
                                            <button
                                                v-for="name in filteredConditions"
                                                :key="name"
                                                type="button"
                                                class="pill"
                                                @click="addCondition(name)"
                                            >
                                                {{ name }}
                                            </button>
                                            <span v-if="filteredConditions.length === 0" class="muted text-sm"
                                                >Geen resultaten</span
                                            >
                                        </div>
                                    </div>
                                </div>
                                <label class="checkbox-row">
                                    <input type="checkbox" v-model="hasCondition" class="checkbox" />
                                    <span>Ik leef met deze aandoening</span>
                                </label>
                            </div>

                            <div class="form-group">
                                <div class="label-row">
                                    <label for="zipcode">Postcode</label>
                                    <InfoToolTipButton
                                        tooltipText="Je gegevens worden veilig en vertrouwelijk verwerkt. Deze worden alleen gebruikt om lotgenoten in uw buurt te tonen."
                                    />
                                </div>
                                <SearchDropdown
                                    id="zipcode"
                                    v-model="zipcode"
                                    :items="zipcodeItems"
                                    placeholder="Kies postcode"
                                />
                            </div>
                        </div>

                        <div class="actions-row">
                            <Button3D
                                variant="primary"
                                class="w-full sm:w-auto"
                                @click="saveSettings"
                                :class="{ 'opacity-60 cursor-not-allowed': (!hasChanges && !isSaving) || isSaving }"
                                :disabled="(!hasChanges && !isSaving) || isSaving"
                            >
                                <span v-if="isSaving">Opslaan...</span>
                                <span v-else>Wijzigingen opslaan</span>
                            </Button3D>
                            <Button3D
                                variant="secondary"
                                class="w-full sm:w-auto"
                                @click="resetChanges"
                                :class="{ 'opacity-60 cursor-not-allowed': !hasChanges || isSaving }"
                                :disabled="!hasChanges || isSaving"
                            >
                                Herstellen
                            </Button3D>
                        </div>
                    </section>

                    <section class="settings-card">
                        <div class="card-heading with-icon">
                            <IconUser class="w-6 h-6 text-brand-purple" />
                            <div>
                                <h2>Mijn verhaal</h2>
                                <p class="muted">Beheer je persoonlijke verhaal</p>
                            </div>
                        </div>
                        <Button3D variant="primary" class="w-full sm:w-auto" @click="router.push({ name: 'write' })">
                            Verhaal bewerken
                        </Button3D>
                    </section>
                </div>
            </template>

            <template v-else>
                <div class="grid gap-6 lg:grid-cols-2">
                    <section class="bg-white border-2 border-brand-purple rounded-2xl shadow-sm p-6 flex flex-col gap-4">
                        <div class="flex items-start justify-between gap-4">
                            <div>
                                <p class="text-sm text-gray-500">Beveiliging</p>
                                <h2 class="text-xl font-semibold text-gray-900">Toegang & wachtwoord</h2>
                            </div>
                            <IconSecurity class="w-6 h-6 text-brand-purple" />
                        </div>
                        <p class="text-gray-700">
                            We ondersteunen wachtwoord reset via e-mail. Klik op de knop hieronder om een resetlink aan te vragen.
                        </p>
                        <div class="flex flex-col gap-3 w-full">
                            <Button3D variant="secondary" class="w-full" @click="router.push({ name: 'forgot-password' })">
                                Wachtwoord resetten
                            </Button3D>
                            <Button3D variant="primary" class="w-full" @click="logoutAndRedirect">Uitloggen</Button3D>
                        </div>
                    </section>

                    <section class="bg-red-50 border border-red-200 rounded-2xl shadow-sm p-6 flex flex-col gap-4">
                        <div class="flex items-center gap-3">
                            <IconUser class="text-red-600" />
                            <div>
                                <p class="text-sm text-red-700">Gevaarzone</p>
                                <h2 class="text-xl font-semibold text-red-800">Account verwijderen</h2>
                            </div>
                        </div>
                        <p class="text-red-800">
                            Verwijder je account permanent, inclusief verhalen en reacties. Dit kan niet ongedaan gemaakt worden.
                        </p>
                        <Button3D variant="danger" @click="openDeleteModal" id="DeleteAccountButton">
                            Verwijder account
                        </Button3D>
                    </section>
                </div>
            </template>
        </template>

        <div v-if="showDeleteModal" class="fixed inset-0 z-50 flex items-center justify-center">
            <div class="absolute inset-0 bg-black opacity-40" @click.self="closeDeleteModal"></div>

            <div class="relative z-10 w-full max-w-lg mx-4">
                <div role="dialog" aria-modal="true" class="bg-white rounded-lg shadow-lg p-6">
                    <h2 class="text-lg font-semibold mb-3">Je staat op het punt je account te verwijderen.</h2>
                    <p class="mb-2">Dat betekent dat al je verhalen, reacties en gegevens permanent verdwijnen.</p>
                    <p class="mb-4 font-medium text-red-700">Ben je zeker?</p>

                    <div class="mb-4">
                        <label for="delete-password" class="block text-sm font-medium text-gray-700 mb-2"
                            >Vul je wachtwoord in ter bevestiging</label
                        >
                        <input
                            id="DeletePasswordInput"
                            type="password"
                            v-model="deletePassword"
                            class="w-full border border-gray-300 rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-brand-purple"
                        />
                    </div>

                    <div class="flex flex-col sm:flex-row gap-3">
                        <Button3D variant="secondary" class="w-full sm:w-1/2" @click="closeDeleteModal" id="CancelButton">
                            Annuleer
                        </Button3D>
                        <Button3D
                            variant="danger"
                            class="w-full sm:w-1/2"
                            @click="confirmDelete"
                            id="ComfirmDeleteAccountButton"
                        >
                            Verwijder mijn account
                        </Button3D>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.tabs-card {
    position: relative;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    border: 1px solid var(--color-brand-purple);
    border-radius: 12px;
    overflow: hidden;
    background: var(--color-brand-purple);
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.05);
}

.tab-indicator {
    position: absolute;
    inset: 0;
    width: 50%;
    background: #ffffff;
    border-radius: 10px;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
    transition: transform 0.25s ease-in-out;
}

.tab-button {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 12px 16px;
    font-size: 1rem;
    font-weight: 500;
    background: transparent;
    color: #ffffff;
    transition: color 0.2s ease;
    position: relative;
    z-index: 1;
}

.tab-button:not(.tab-active):hover {
    color: rgba(255, 255, 255, 0.85);
}

.tab-icon {
    width: 22px;
    height: 22px;
}

.tab-active {
    color: var(--color-brand-purple);
}

.settings-card {
    background: #ffffff;
    border: 2px solid var(--color-brand-purple);
    border-radius: 12px;
    padding: 20px;
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.04);
}

.settings-card.danger {
    border-color: #ef4444;
}

.card-heading {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 14px;
}

.card-heading.with-icon {
    gap: 10px;
}

.card-heading h2 {
    font-size: 1.125rem;
    font-weight: 700;
}

.muted {
    color: #5f6368;
    font-size: 0.95rem;
}

.form-stack {
    display: flex;
    flex-direction: column;
    gap: 12px;
}

.form-group {
    display: flex;
    flex-direction: column;
    gap: 6px;
}

.label-row {
    display: flex;
    align-items: center;
    gap: 6px;
}

.input {
    border: 1px solid #d1d5db;
    border-radius: 8px;
    padding: 10px 12px;
    font-size: 0.95rem;
    outline: none;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.input:focus {
    border-color: var(--color-brand-purple);
    box-shadow: 0 0 0 3px rgba(125, 93, 246, 0.15);
}

.chip-row {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.chip {
    display: inline-flex;
    align-items: center;
    gap: 6px;
    background: var(--color-brand-purple);
    color: #ffffff;
    padding: 6px 10px;
    border-radius: 9999px;
    font-size: 0.85rem;
}

.chip-close {
    background: rgba(255, 255, 255, 0.2);
    border: none;
    color: #ffffff;
    border-radius: 9999px;
    width: 18px;
    height: 18px;
    line-height: 18px;
    text-align: center;
    cursor: pointer;
}

.suggestions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    max-height: 110px;
    overflow-y: auto;
    padding-right: 4px;
}

.dropdown-list {
    max-height: 220px;
}

.pill {
    border: 1px solid var(--color-brand-purple);
    color: var(--color-brand-purple);
    background: rgba(125, 93, 246, 0.1);
    border-radius: 9999px;
    padding: 6px 10px;
    font-size: 0.85rem;
    transition: background-color 0.15s ease;
}

.pill:hover {
    background: rgba(125, 93, 246, 0.2);
}

.checkbox-row {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 0.95rem;
    margin-top: 2px;
    color: #111827;
}

.checkbox {
    width: 16px;
    height: 16px;
}

.actions-row {
    margin-top: 12px;
    display: flex;
    gap: 12px;
    flex-wrap: wrap;
}

.condition-group {
    position: relative;
}

.select-box {
    width: 100%;
    border: 1px solid #d1d5db;
    border-radius: 8px;
    padding: 10px 12px;
    display: flex;
    align-items: center;
    justify-content: space-between;
    gap: 10px;
    background: #ffffff;
    cursor: pointer;
    transition: border-color 0.15s ease, box-shadow 0.15s ease;
}

.select-box:hover {
    border-color: var(--color-brand-purple);
}

.select-box:focus-visible {
    outline: none;
    border-color: var(--color-brand-purple);
    box-shadow: 0 0 0 3px rgba(125, 93, 246, 0.15);
}

.chevron {
    color: #6b7280;
    font-size: 0.9rem;
}

.condition-dropdown {
    position: absolute;
    top: 100%;
    left: 0;
    right: 0;
    margin-top: 6px;
    z-index: 30;
    background: #ffffff;
    border: 1px solid #d1d5db;
    border-radius: 10px;
    box-shadow: 0 12px 30px rgba(0, 0, 0, 0.12);
    padding: 12px;
}

.condition-dropdown .input {
    width: 100%;
}
</style>
