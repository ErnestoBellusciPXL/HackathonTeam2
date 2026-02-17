<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAdminStore } from "@/stores/admin";
import { useToastStore } from "@/stores/toast";
import Button3D from "@/components/Button3D.vue";
import { sanitizeHtml } from "@/utils/HTMLSanitizer";
import type { AdminUser } from "@/utils/Admin";

const route = useRoute();
const router = useRouter();
const toast = useToastStore();
const adminStore = useAdminStore();

const userId = String(route.params.id ?? "");
const user = ref<AdminUser | null>(null);
const loading = ref(false);
const showConfirm = ref(false);
const confirming = ref(false);
const reason = ref("");
const stories = ref<any[]>([]);
const showSendEmailPrompt = ref(false);

function formatDisabledAt(ts: unknown) {
    if (!ts) return "";
    try {
        const d = new Date(String(ts));
        if (Number.isNaN(d.getTime())) return String(ts);
        return d.toLocaleString("nl-BE", {
            year: "numeric",
            month: "short",
            day: "2-digit",
            hour: "2-digit",
            minute: "2-digit",
        });
    } catch (e) {
        return String(ts);
    }
}

async function loadUser() {
    if (!userId) return;
    loading.value = true;
    try {
        const data = await adminStore.getUserById(userId);
        user.value = data as AdminUser;
        // load stories for this user (show the most recent)
        try {
            stories.value = await adminStore.getStoriesByUserId(userId);
        } catch (err) {
            // non-fatal
            console.warn("Failed to load user stories", err);
        }
    } catch (err) {
        const message = err instanceof Error ? err.message : String(err);
        if (message.includes("Token ontbreekt")) {
            toast.error("Niet ingelogd. Log opnieuw in.");
            router.push({ name: "login" });
            return;
        }
        toast.error("Kon gebruiker niet laden");
    } finally {
        loading.value = false;
    }
}

async function handleResetPassword() {
    if (!user.value) return;
    try {
        await adminStore.adminResetPassword(user.value.id);
        toast.success("Reset-wachtwoordlink verzonden (of action uitgevoerd)");
    } catch (err) {
        toast.error("Kon wachtwoord niet resetten");
        console.error(err);
    }
}

function handleSendEmail() {
    if (!user.value || !user.value.email) {
        toast.error("Geen e-mailadres beschikbaar voor deze gebruiker");
        return;
    }

    const to = encodeURIComponent(user.value.email);
    const subject = encodeURIComponent("Belangrijke mededeling OpenInzicht");
    const body = encodeURIComponent("Beste gebruiker,\n\n");

    // Open user's default mail client (Outlook, etc.) via mailto
    window.location.href = `mailto:${to}?subject=${subject}&body=${body}`;
}

function requestToggleDisabled() {
    showConfirm.value = true;
}

function goToReports() {
    if (user.value?.username) {
        router.push({ name: "admin", query: { tab: "tickets", reportee: user.value.username } });
    } else {
        router.push({ name: "admin", query: { tab: "tickets" } });
    }
}

async function confirmToggleDisabled() {
    if (!user.value) return;
    confirming.value = true;
    const target = !user.value.disabled;
    try {
        // when disabling, validate reason (not only spaces, min 5, max 250)
        if (target) {
            const raw = reason.value;
            const trimmed = raw.trim();
            if (trimmed.length === 0) {
                toast.error("Gelieve een geldige reden op te geven (niet alleen spaties).");
                confirming.value = false;
                return;
            }
            if (trimmed.length < 5) {
                toast.error("Reden moet minimaal 5 tekens bevatten.");
                confirming.value = false;
                return;
            }
            if (raw.length > 250) {
                toast.error("Reden mag maximaal 250 tekens bevatten.");
                confirming.value = false;
                return;
            }
        }

        await adminStore.setUserDisabled(user.value.id, target, reason.value.trim());
        toast.success(`Gebruiker ${target ? "gedeactiveerd" : "geactiveerd"}`);
        // reload details
        await loadUser();
    } catch (err) {
        toast.error("Kon status niet bijwerken");
        console.log(err);
    } finally {
        confirming.value = false;
        showConfirm.value = false;
        reason.value = "";
    }
}

onMounted(() => {
    loadUser();
});
</script>

<template>
    <div class="container mx-auto px-4 py-8 max-w-4xl min-h-screen">
        <div v-if="loading" class="text-center">Laden...</div>

        <div v-else-if="user">
            <div class="mb-8">
                <div class="mb-3">
                    <h2 class="text-2xl font-bold">{{ user.username }}</h2>
                    <p class="text-sm text-gray-500">Details en acties voor gebruiker</p>
                    <div class="mt-3">
                        <button
                            type="button"
                            @click="router.back()"
                            class="text-sm text-gray-600 hover:text-gray-800 flex items-center gap-2"
                            aria-label="Terug"
                        >
                            <span class="text-xl leading-none">&lt;</span>
                            <span>Terug</span>
                        </button>
                    </div>
                </div>
            </div>

            <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
                <!-- Left: user details -->
                <div class="md:col-span-2 bg-white border rounded-xl p-6">
                    <div class="grid grid-cols-1 gap-4">
                        <div>
                            <div class="text-sm text-gray-500">Gebruikersnaam</div>
                            <span class="font-medium text-lg" id="username">{{ user.username }}</span>
                        </div>

                        <div class="grid grid-cols-2 gap-4">
                            <div>
                                <div class="text-sm text-gray-500">E-mail</div>
                                <span class="font-medium" id="email">{{ user.email ?? "-" }}</span>
                            </div>
                        </div>

                        <div>
                            <div class="text-sm text-gray-500">Aandoeningen</div>
                            <div class="flex flex-wrap gap-2 mt-3">
                                <span
                                    v-for="c in user.conditions ?? []"
                                    :key="c"
                                    class="condition-item inline-block bg-brand-purple text-white text-xs px-3 py-1 rounded-full"
                                >
                                    {{ c }}
                                </span>
                                <span v-if="(user.conditions ?? []).length === 0" class="text-gray-400"
                                    >Geen aandoeningen</span
                                >
                            </div>
                            <hr class="w-full border-t border-gray-200 mt-10" />
                            <div v-if="stories.length > 0" class="mt-8 md:col-span-2 bg-white rounded-xl">
                                <h3 class="text-xl font-semibold mb-3" id="user-story">
                                    Verhaal van gebruiker
                                </h3>
                                <div v-for="s in stories" :key="s.id" class="prose max-w-full">
                                    <h4 class="text-lg font-bold" id="user-story-title">{{ s.title }}</h4>
                                    <div v-html="sanitizeHtml(s.content)"></div>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- Right: status / actions -->
                <aside class="bg-white border rounded-xl p-6 flex flex-col gap-4">
                    <div>
                        <div class="text-sm text-gray-500">Status:</div>
                        <div class="mt-2">
                            <span
                                id="status"
                                :class="
                                    user.disabled
                                        ? 'inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-100 text-red-700 text-sm'
                                        : 'inline-flex items-center gap-2 px-3 py-1 rounded-full bg-green-100 text-green-700 text-sm'
                                "
                            >
                                {{ user.disabled ? "Gedeactiveerd" : "Actief" }}
                            </span>
                        </div>
                        <div v-if="user.disabled" class="mt-3 text-sm text-gray-700">
                            <div class="font-semibold">Reden voor deactivering</div>
                            <div class="mt-1 text-sm">
                                {{ (user as any).disabledReason ?? "(geen reden opgegeven)" }}
                            </div>
                            <div v-if="(user as any).disabledAt" class="mt-1 text-xs text-gray-500">
                                {{ formatDisabledAt((user as any).disabledAt) }}
                            </div>
                        </div>
                    </div>

                    <div>
                        <div class="text-sm text-gray-500">Acties:</div>
                        <div class="mt-3 grid grid-cols-1 gap-2">
                            <Button3D variant="secondary" @click="goToReports">Bekijk reports</Button3D>
                            <Button3D variant="secondary" @click="handleResetPassword"
                                >Wachtwoord resetten</Button3D
                            >
                            <Button3D variant="secondary" @click="handleSendEmail">E-mail versturen</Button3D>
                            <Button3D
                                :variant="user.disabled ? 'primary' : 'danger'"
                                @click="requestToggleDisabled"
                                id="toggle-user-activation"
                            >
                                {{ user.disabled ? "Activeer gebruiker" : "Deactiveer gebruiker" }}
                            </Button3D>
                        </div>
                    </div>

                    <div class="mt-auto text-xs text-gray-500">
                        ID: <span class="font-mono text-sm">{{ user.id }}</span>
                    </div>
                </aside>
            </div>
        </div>

        <div v-else class="text-gray-600">Gebruiker niet gevonden.</div>

        <!-- Show user's story if available -->

        <!-- Confirmation modal -->
        <div
            v-if="showConfirm"
            class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
            role="dialog"
            aria-modal="true"
        >
            <div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6 space-y-4">
                <h2 class="text-xl font-semibold text-gray-900">Bevestig actie</h2>
                <p class="text-gray-700">
                    Weet je zeker dat je deze gebruiker wilt
                    <strong> {{ user?.disabled ? "activeren" : "deactiveren" }}</strong
                    >?
                </p>
                <div v-if="!user?.disabled" class="mt-2">
                    <label class="block text-sm text-gray-600 mb-1">Reden voor deactivering</label>
                    <textarea
                        v-model="reason"
                        rows="4"
                        maxlength="250"
                        class="w-full border border-gray-300 rounded-md p-2 focus:outline-none focus:ring-2 focus:ring-brand-purple"
                        placeholder="Geef kort de reden op waarom deze gebruiker gedeactiveerd wordt"
                    />
                </div>
                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <Button3D
                        asTag="button"
                        variant="secondary"
                        class="w-full"
                        @click.prevent="showConfirm = false"
                    >
                        Annuleren
                    </Button3D>
                    <Button3D
                        asTag="button"
                        variant="danger"
                        class="w-full"
                        :disabled="confirming"
                        @click.prevent="confirmToggleDisabled"
                    >
                        {{ confirming ? "Bezig..." : user?.disabled ? "Activeer" : "Deactiveer" }}
                    </Button3D>
                </div>
            </div>
        </div>
    </div>
</template>

<style scoped>
.container {
}

/* small refinements to match dashboard look */
.bg-white.border {
    border-color: rgba(0, 0, 0, 0.04);
}

@media (min-width: 768px) {
    .max-w-4xl {
        max-width: 64rem;
    }
}
</style>
