<template>
    <div class="bg-white border-2 border-brand-purple rounded-xl p-6 overflow-y-auto">
        <div class="flex items-start justify-between mb-4 gap-4">
            <div>
                <h2 class="text-2xl font-bold mb-1">{{ title }}</h2>
                <p v-if="subtitle" class="text-gray-600">{{ subtitle }}</p>
            </div>
            <div
                v-if="showPageBadge"
                class="px-3 py-1 text-sm font-medium rounded-full bg-gray-100 text-gray-700 border border-gray-200"
            >
                Pagina {{ currentPage }} / {{ Math.max(totalPages, 1) }}
            </div>
        </div>

        <div v-if="isLoading" class="py-10 text-center text-gray-500">Laden...</div>
        <div
            v-else-if="errorMessage"
            class="py-6 px-4 border border-red-200 bg-red-50 text-red-700 rounded-lg"
        >
            {{ errorMessage }}
        </div>
        <div v-else-if="tickets.length === 0" class="py-6 text-center text-gray-500">
            Geen meldingen gevonden.
        </div>
        <div v-else class="border-2 border-gray-200 rounded-xl overflow-y-auto max-h-96">
            <table class="w-full">
                <thead class="bg-gray-50 border-b-2 border-gray-200">
                    <tr>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">Datum</th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">
                            Gerapporteerde
                        </th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">Verhaal</th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">Reden</th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">Status</th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700" />
                    </tr>
                </thead>
                <tbody>
                    <tr
                        v-for="(ticket, index) in tickets"
                        :key="ticket.id"
                        class="border-b border-gray-200 hover:bg-gray-50 transition"
                        :class="{ 'bg-gray-50': index % 2 === 1 }"
                    >
                        <td class="px-6 py-4 text-sm text-gray-700">{{ formatDate(ticket.createdAt) }}</td>
                        <td class="px-6 py-4 text-sm">
                            <div class="text-gray-900">
                                {{ formatUser(ticket.reporteeUsername) }}
                            </div>
                        </td>
                        <td class="px-6 py-4 text-sm">
                            <div class="text-gray-900">
                                {{
                                    formatStory(ticket.storyTitle, ticket.storyId, ticket.storyTitleSnapshot)
                                }}
                            </div>
                        </td>
                        <td class="px-6 py-4 text-sm max-w-[18rem] align-top">
                            <div class="font-medium text-gray-900 wrap-break-word whitespace-pre-wrap">{{ formatReason(ticket) }}</div>
                            <div
                                v-if="ticket.reportReason === 'OTHER' && ticket.otherReason"
                                class="text-xs text-gray-500 wrap-break-word whitespace-pre-wrap"
                            >
                                {{ ticket.otherReason }}
                            </div>
                        </td>
                        <td class="px-6 py-4 text-sm">
                            <span
                                class="inline-flex items-center px-3 py-1 rounded-full border text-xs font-semibold"
                                :class="stateClass(ticket.state)"
                            >
                                {{ stateLabel(ticket.state) }}
                            </span>
                        </td>
                        <td class="px-6 py-4 text-sm">
                            <Button3D
                                variant="primary"
                                class="px-6 py-2 text-sm rounded-full"
                                @click="emit('view-ticket', ticket.id)"
                            >
                                Bekijken
                            </Button3D>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <PaginationControls
            v-if="showPagination && totalPages > 1"
            :current-page="currentPage"
            :total-pages="totalPages"
            @update:current-page="onPageChange"
        />
    </div>
</template>

<script setup lang="ts">
import type { AdminTicket } from "@/utils/Admin";
import PaginationControls from "./PaginationControls.vue";
import Button3D from "./Button3D.vue";

withDefaults(
    defineProps<{
        tickets: AdminTicket[];
        currentPage: number;
        totalPages: number;
        isLoading?: boolean;
        errorMessage?: string;
        title?: string;
        subtitle?: string;
        showPageBadge?: boolean;
        showPagination?: boolean;
    }>(),
    {
        title: "Gerapporteerde verhalen",
        subtitle: "Overzicht van alle meldingen uit het systeem",
        showPageBadge: true,
        showPagination: true,
    },
);

const emit = defineEmits<{
    "update:currentPage": [page: number];
    "view-ticket": [ticketId: string];
}>();

function formatDate(value: string): string {
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "-" : date.toLocaleString("nl-NL");
}

function formatUser(user?: string | null): string {
    if (!user) return "Onbekend";
    return user;
}

function formatStory(title?: string | null, storyId?: string, snapshotTitle?: string | null): string {
    if (title) return truncate(title, 30);
    if (snapshotTitle) return truncate(snapshotTitle, 30);
    if (storyId) return `Verhaal ${storyId}`;
    return "Onbekend verhaal";
}

function formatReason(ticket: AdminTicket): string {
    const reasonLabels: Record<string, string> = {
        SPAM: "Spam",
        HARASSMENT: "Pesterij",
        INAPPROPRIATE_CONTENT: "Ongepaste inhoud",
        MISINFORMATION: "Misleidende info",
        OTHER: "Andere reden",
    };
    return reasonLabels[ticket.reportReason] ?? ticket.reportReason ?? "Onbekend";
}

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

function onPageChange(page: number) {
    emit("update:currentPage", page);
}

function truncate(text: string, max: number): string {
    if (text.length <= max) return text;
    return text.slice(0, max - 1) + "…";
}
</script>
