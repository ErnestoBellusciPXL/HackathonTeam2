<template>
    <div class="container mx-auto px-4 py-8 flex flex-col items-center">
        <div class="w-full max-w-6xl">
            <button type="button" class="text-sm text-gray-600 hover:text-gray-800 mb-4" @click="goBack">
                &larr; Terug
            </button>

            <h1 class="text-3xl md:text-4xl font-bold mb-2">Details gerapporteerd verhaal</h1>
            <p class="text-gray-600 mb-8">
                Overzicht van het gerapporteerde verhaal. Controleer dit verhaal en voer een actie uit.
            </p>

            <div
                v-if="errorMessage"
                class="mb-6 rounded-lg border border-red-200 bg-red-50 px-4 py-3 text-red-700"
            >
                {{ errorMessage }}
            </div>
            <div
                v-else-if="loadingTicket"
                class="mb-6 rounded-lg border border-gray-200 bg-white px-4 py-3 text-gray-700"
            >
                Gegevens laden...
            </div>
            <div
                v-else-if="!ticket"
                class="mb-6 rounded-lg border border-yellow-200 bg-yellow-50 px-4 py-3 text-yellow-700"
            >
                Ticket niet gevonden.
            </div>
            <div v-else class="grid md:grid-cols-3 gap-6">
                <div class="md:col-span-2">
                    <section class="detail-card space-y-4">
                        <div>
                            <h3 class="detail-label">Gebruikersnaam</h3>
                            <p class="detail-value">{{ reporteeName }}</p>
                        </div>

                        <div>
                            <h3 class="detail-label">Aandoening</h3>
                            <div class="flex flex-wrap gap-2 mt-1">
                                <span
                                    v-for="condition in conditionTags"
                                    :key="condition"
                                    class="inline-flex items-center bg-brand-purple text-white text-xs px-3 py-1 rounded-full"
                                >
                                    {{ condition }}
                                </span>
                                <span v-if="conditionTags.length === 0" class="text-gray-500 text-sm">
                                    Geen aandoening opgegeven
                                </span>
                            </div>
                        </div>

                        <div>
                            <h3 class="detail-label">Titel verhaal</h3>
                            <p class="detail-value">{{ storyTitle }}</p>
                        </div>

                        <div>
                            <div class="border border-gray-200 bg-gray-50 rounded-md px-4 py-3">
                                <div v-if="storyLoading" class="text-gray-500 text-sm">
                                    Verhaal wordt geladen...
                                </div>
                                <p v-else-if="storyBody" class="detail-body" v-html="sanitizedStory"></p>
                                <p v-else class="text-gray-500 text-sm">Geen verhaaltekst beschikbaar.</p>
                            </div>
                        </div>
                    </section>
                </div>

                <div class="space-y-4">
                    <section class="detail-card space-y-3">
                        <div>
                            <h3 class="detail-label">Gerapporteerd door</h3>
                            <p class="detail-value">{{ reporterName }}</p>
                        </div>
                        <div>
                            <h3 class="detail-label">Datum melding</h3>
                            <p class="detail-value">{{ formattedDate }}</p>
                        </div>
                        <div>
                            <h3 class="detail-label">Reden</h3>
                            <p class="detail-value">{{ reasonLabel }}</p>
                            <p
                                v-if="ticket.reportReason === 'OTHER' && ticket.otherReason"
                                class="text-gray-600 text-sm mt-1"
                            >
                                {{ ticket.otherReason }}
                            </p>
                        </div>
                        <div>
                            <h3 class="detail-label">Status</h3>
                            <span
                                class="inline-flex items-center px-3 py-1 rounded-full border text-xs font-semibold"
                                :class="stateClass(ticket.state)"
                            >
                                {{ stateLabel(ticket.state) }}
                            </span>
                        </div>
                    </section>

                    <section v-if="!isClosed" class="detail-card space-y-3">
                        <h3 class="detail-label">Acties</h3>
                        <Button3D
                            id="approve_button"
                            class="w-full justify-center"
                            :disabled="!ticket || actionLoading === 'approve'"
                            @click="approveTicket"
                        >
                            ✓ Verhaal goedkeuren
                        </Button3D>
                        <Button3D
                            id="delete_button"
                            variant="danger"
                            class="w-full justify-center"
                            :disabled="!ticket || actionLoading === 'remove'"
                            @click="removeStory"
                        >
                            ✕ Verhaal verwijderen
                        </Button3D>
                    </section>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { useAdminStore } from "@/stores/admin";
import { useStoryDetailStore } from "@/stores/storyDetail";
import Button3D from "@/components/Button3D.vue";
import { useToastStore } from "@/stores/toast";
import type { AdminTicket } from "@/utils/Admin";
import { sanitizeHtml } from "@/utils/HTMLSanitizer";

const route = useRoute();
const router = useRouter();
const adminStore = useAdminStore();
const storyDetailStore = useStoryDetailStore();
const toast = useToastStore();

const { story, loading: storyLoading } = storeToRefs(storyDetailStore);

const ticket = ref<AdminTicket | null>(null);
const loadingTicket = ref(false);
const errorMessage = ref("");
const actionLoading = ref<null | "approve" | "remove">(null);

const reasonLabels: Record<string, string> = {
    SPAM: "Spam",
    HARASSMENT: "Pesterij",
    INAPPROPRIATE_CONTENT: "Ongepaste inhoud",
    MISINFORMATION: "Misleidende info",
    OTHER: "Andere reden",
};

const reporteeName = computed(() => ticket.value?.reporteeUsername || "Onbekend");
const reporterName = computed(() => ticket.value?.reporterUsername || "Onbekend");
const storyTitle = computed(
    () =>
        story.value?.title ||
        ticket.value?.storyTitle ||
        ticket.value?.storyTitleSnapshot ||
        "Onbekend verhaal",
);
const storyBody = computed(() => story.value?.content ?? ticket.value?.storyContentSnapshot ?? "");
const conditionTags = computed(() => {
    if (story.value?.conditionNames?.length) return story.value.conditionNames;
    const snapshot = ticket.value?.storyConditionsSnapshot;
    if (!snapshot) return [];
    if (Array.isArray(snapshot)) return snapshot;
    // Accept comma-separated text or single value
    return snapshot
        .split(",")
        .map((c) => c.trim())
        .filter(Boolean);
});
const isClosed = computed(() => (ticket.value?.state ?? "").toUpperCase() === "CLOSED");
const formattedDate = computed(() => (ticket.value ? formatDate(ticket.value.createdAt) : "-"));
const reasonLabel = computed(
    () => reasonLabels[ticket.value?.reportReason ?? ""] ?? ticket.value?.reportReason ?? "Onbekend",
);

function stateLabel(state: string): string {
    const normalized = state?.toUpperCase();
    if (normalized === "CLOSED") return "Gesloten";
    if (normalized === "OPEN") return "Open";
    return state || "Onbekend";
}

function stateClass(state: string): string {
    const normalized = state?.toUpperCase();
    if (normalized === "CLOSED") {
        return "bg-green-100 text-green-700 border-green-200";
    }
    if (normalized === "OPEN") {
        return "bg-amber-100 text-amber-700 border-amber-200";
    }
    return "bg-gray-100 text-gray-700 border-gray-200";
}

function formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("nl-NL");
}

async function loadTicket() {
    const id = route.params.id as string;
    if (!id) {
        errorMessage.value = "Geen ticket-id opgegeven.";
        return;
    }

    loadingTicket.value = true;
    errorMessage.value = "";
    ticket.value = null;
    try {
        const data = await adminStore.getTicketById(id);
        ticket.value = data;
        if (data.storyId) {
            await storyDetailStore.fetchStory(data.storyId);
        } else {
            story.value = null;
        }
    } catch (error) {
        console.error("Failed to load ticket:", error);
        errorMessage.value = error instanceof Error ? error.message : "Kon ticket niet laden.";
    } finally {
        loadingTicket.value = false;
    }
}

function goBack() {
    router.push({ name: "admin", query: { tab: "tickets" } });
}

async function approveTicket() {
    if (!ticket.value) return;
    actionLoading.value = "approve";
    try {
        const updated = await adminStore.closeTicket(ticket.value.id, false);
        ticket.value = updated;
        toast.success("Melding is goedgekeurd en gesloten.");
    } catch (error) {
        console.error("Failed to approve ticket:", error);
        toast.error(error instanceof Error ? error.message : "Kon melding niet sluiten.");
    } finally {
        actionLoading.value = null;
    }
}

async function removeStory() {
    if (!ticket.value) return;
    actionLoading.value = "remove";
    try {
        const updated = await adminStore.closeTicket(ticket.value.id, true);
        ticket.value = updated;
        story.value = null;
        toast.success("Verhaal verwijderd en melding gesloten.");
    } catch (error) {
        console.error("Failed to remove story:", error);
        toast.error(error instanceof Error ? error.message : "Kon verhaal niet verwijderen.");
    } finally {
        actionLoading.value = null;
    }
}

onMounted(loadTicket);

const sanitizedStory = computed(() => {
    if (!storyBody.value) return "";
    return sanitizeHtml(storyBody.value);
});

watch(
    () => route.params.id,
    () => {
        loadTicket();
    },
);
</script>

<style scoped>
.detail-card {
    border: 2px solid var(--color-brand-purple);
    border-radius: 12px;
    padding: 14px 16px;
    background: #ffffff;
}

.detail-label {
    font-weight: 700;
    color: #111827;
    margin-bottom: 6px;
}

.detail-value {
    color: #1f2937;
}

.detail-body {
    color: #1f2937;
    line-height: 1.7;
    white-space: pre-line;
}
</style>
