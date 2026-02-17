<script setup lang="ts">
import { useAdminStore } from "@/stores/admin";
import { ref, onMounted, computed, watch } from "vue";
import { useRouter, useRoute } from "vue-router";
import type { Content, AllUsersPagedResponse, AdminTicketsPagedResponse } from "@/utils/Admin";
import StatsCard from "@/components/StatsCard.vue";
import UserTable from "@/components/UserTable.vue";
import IconUsers from "@/components/icons/IconUsers.vue";
import IconUsersTwo from "@/components/icons/IconUsersTwo.vue";
import IconAttentionTriangle from "@/components/icons/IconAttentionTriangle.vue";
import IconCheck from "@/components/icons/IconCheck.vue";
import IconDeactivated from "@/components/icons/IconDeactivated.vue";
import AdminTicketTable from "@/components/AdminTicketTable.vue";

const adminStore = useAdminStore();
const router = useRouter();
const route = useRoute();
const pageSize = 25;
const ticketPageSize = 10;

const currentPage = ref(1);
const usersData = ref<AllUsersPagedResponse | null>(null);
const searchQuery = ref("");
const conditionFilter = ref("");
const statusFilter = ref("");
const activeTab = ref<"users" | "tickets">(route.query.tab === "tickets" ? "tickets" : "users");
const ticketPage = ref(1);
const ticketsData = ref<AdminTicketsPagedResponse | null>(null);
const isLoadingTickets = ref(false);
const ticketError = ref("");
const indicatorStyles = computed(() => ({
    transform: activeTab.value === "users" ? "translateX(0%)" : "translateX(100%)",
}));

const totalUsers = computed(() => usersData.value?.totalElements || 0);
const activeUsers = computed(() => {
    // Count users that are NOT disabled
    return usersData.value?.content.filter((user) => !user.disabled).length || 0;
});
const deactivatedUsers = computed(() => {
    // Count users that ARE disabled
    return usersData.value?.content.filter((user) => !!user.disabled).length || 0;
});

async function loadUsers() {
    try {
        usersData.value = await adminStore.getAllUsersPaged(currentPage.value - 1, pageSize);
    } catch (error) {
        console.error("Failed to load users:", error);
    }
}

function handlePageChange(page: number) {
    currentPage.value = page;
    loadUsers();
}

function handleManageUser(user: Content) {
    router.push({ name: "admin-user-detail", params: { id: user.id } });
}

// Separate lists to keep pagination stable per filter
const openTickets = computed(() => {
    const list = ticketsData.value?.content ?? [];
    return list.filter((t) => (t.state ?? "").toUpperCase() === "OPEN");
});
const otherTickets = computed(() => {
    const list = ticketsData.value?.content ?? [];
    return list.filter((t) => (t.state ?? "").toUpperCase() !== "OPEN");
});

// Total pages computed from the selected filtered list
const ticketTotalPages = computed(() => {
    const base =
        ticketStateFilter.value === "open"
            ? openTickets.value
            : ticketStateFilter.value === "closed"
              ? otherTickets.value.filter((t) => (t.state ?? "").toUpperCase() === "CLOSED")
              : filteredTickets.value;
    const pages = Math.ceil(base.length / ticketPageSize);
    return pages > 0 ? pages : 1;
});
const ticketStateFilter = ref<"all" | "open" | "closed">((route.query.reportee as string) ? "all" : "open");
const ticketSearchQuery = ref((route.query.reportee as string) ?? "");
const ticketStateOptions = [
    { label: "Open", value: "open" as const },
    { label: "Gesloten", value: "closed" as const },
    { label: "Alle", value: "all" as const },
];
const stateIndicatorStyle = computed(() => {
    const idx = ticketStateOptions.findIndex((opt) => opt.value === ticketStateFilter.value);
    const safeIdx = idx >= 0 ? idx : 0;
    const count = ticketStateOptions.length || 1;
    const width = 100 / count;
    const gap = 0;
    return {
        width: `calc(${width}% - ${gap}px)`,
        left: `calc(${safeIdx * width}% + ${gap / 2}px)`,
    };
});
const filteredTickets = computed(() => {
    let list: (typeof ticketsData.value extends null
        ? never
        : NonNullable<typeof ticketsData.value>["content"][number])[] = ticketsData.value?.content ?? [];
    if (ticketStateFilter.value === "open") {
        list = openTickets.value;
    } else if (ticketStateFilter.value === "closed") {
        list = otherTickets.value.filter((t) => (t.state ?? "").toUpperCase() === "CLOSED");
    }

    const term = ticketSearchQuery.value.trim().toLowerCase();
    if (term) {
        list = list.filter((ticket) => (ticket.reporteeUsername ?? "").toLowerCase().includes(term));
    }

    const sorted = [...list].sort((a, b) => {
        const aTime = new Date(a.createdAt).getTime();
        const bTime = new Date(b.createdAt).getTime();
        if (Number.isNaN(aTime) && Number.isNaN(bTime)) return 0;
        if (Number.isNaN(aTime)) return 1;
        if (Number.isNaN(bTime)) return -1;
        return bTime - aTime;
    });

    return sorted;
});

// Slice per page for display stability
const pagedTickets = computed(() => {
    const start = (ticketPage.value - 1) * ticketPageSize;
    const end = start + ticketPageSize;
    return filteredTickets.value.slice(start, end);
});

async function loadTickets() {
    try {
        ticketError.value = "";
        isLoadingTickets.value = true;
        // Load a sufficiently large page once; then paginate client-side per filter
        ticketsData.value = await adminStore.getAllTicketsPaged(0, 1000);
    } catch (error) {
        console.error("Failed to load tickets:", error);
        ticketError.value = error instanceof Error ? error.message : "Kon meldingen niet laden.";
    } finally {
        isLoadingTickets.value = false;
    }
}

function handleTicketPageChange(page: number) {
    ticketPage.value = page;
    loadTickets();
}

function handleViewTicket(ticketId: string) {
    router.push({ name: "admin-ticket-detail", params: { id: ticketId } });
}

onMounted(async () => {
    await loadUsers();
});

// If the route contains a reportee query, sync it to the ticket search field
watch(
    () => route.query.reportee,
    (val) => {
        ticketSearchQuery.value = String(val ?? "");
        ticketPage.value = 1;
        // When coming from a specific user, show all tickets by default
        ticketStateFilter.value = "all";
        if (activeTab.value !== "tickets") activeTab.value = "tickets";
    }
);

watch(
    () => activeTab.value,
    (tab) => {
        if (tab === "tickets" && !ticketsData.value && !isLoadingTickets.value) {
            loadTickets();
        }
        router.replace({ query: { ...route.query, tab } });
    },
    { immediate: true },
);

// Reset to first page when filter or search changes
watch([ticketStateFilter, ticketSearchQuery], () => {
    ticketPage.value = 1;
});
</script>

<template>
    <div class="container mx-auto px-4 py-8 max-w-7xl">
        <h1 class="text-4xl font-bold mb-2">Dashboard</h1>
        <p class="text-gray-600 mb-8">Overzicht van alle gebruikers en hun status</p>

        <div class="tabs-card mb-8">
            <div class="tab-indicator" :style="indicatorStyles" />
            <button
                id="users"
                type="button"
                class="tab-button"
                :class="{ 'tab-active': activeTab === 'users' }"
                @click="activeTab = 'users'"
            >
                <IconUsersTwo class="tab-icon" />
                <span>Gebruikers</span>
            </button>

            <button
                id="reports"
                type="button"
                class="tab-button"
                :class="{ 'tab-active': activeTab === 'tickets' }"
                @click="activeTab = 'tickets'"
            >
                <IconAttentionTriangle class="tab-icon" />
                <span>Gerapporteerde verhalen</span>
            </button>
        </div>

        <template v-if="activeTab === 'users'">
            <div class="grid grid-cols-1 md:grid-cols-3 gap-6 mb-8">
                <StatsCard title="Totaal gebruikers" :value="totalUsers" icon-color-class="text-blue-500">
                    <template #icon>
                        <IconUsers />
                    </template>
                </StatsCard>

                <StatsCard title="Actieve gebruikers" :value="activeUsers" icon-color-class="text-green-500">
                    <template #icon>
                        <IconCheck />
                    </template>
                </StatsCard>

                <StatsCard title="Gedeactiveerd" :value="deactivatedUsers" icon-color-class="text-red-500">
                    <template #icon>
                        <IconDeactivated />
                    </template>
                </StatsCard>
            </div>

            <UserTable
                v-if="usersData"
                :users="usersData.content"
                :current-page="currentPage"
                :total-pages="usersData.totalPages"
                v-model:search-query="searchQuery"
                v-model:condition-filter="conditionFilter"
                v-model:status-filter="statusFilter"
                @update:current-page="handlePageChange"
                @manage-user="handleManageUser"
            />
        </template>
        <template v-else>
            <div class="space-y-4">
                <div class="flex flex-col md:flex-row md:items-center gap-3">
                    <div class="flex items-center gap-2">
                        <span class="text-sm text-gray-600">Filter:</span>
                        <div class="state-toggle">
                            <div class="state-toggle-indicator" :style="stateIndicatorStyle" />
                            <button
                                v-for="option in ticketStateOptions"
                                :key="option.value"
                                type="button"
                                class="state-toggle-btn"
                                :class="{ 'state-active': ticketStateFilter === option.value }"
                                @click="ticketStateFilter = option.value"
                            >
                                {{ option.label }}
                            </button>
                        </div>
                    </div>

                    <div class="flex-1">
                        <div class="relative w-full md:w-96">
                            <svg
                                class="absolute left-3 top-1/2 -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none"
                                fill="none"
                                stroke="currentColor"
                                viewBox="0 0 24 24"
                            >
                                <path
                                    stroke-linecap="round"
                                    stroke-linejoin="round"
                                    stroke-width="2"
                                    d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                                />
                            </svg>
                            <input
                                v-model="ticketSearchQuery"
                                type="text"
                                placeholder="Zoek op gerapporteerde gebruiker"
                                class="w-full h-10 border border-gray-300 rounded-xl pl-10 pr-4 focus:outline-none focus:border-brand-purple transition placeholder:text-gray-400"
                            />
                        </div>
                    </div>
                </div>

                <AdminTicketTable
                    :tickets="pagedTickets"
                    :current-page="ticketPage"
                    :total-pages="ticketTotalPages"
                    :is-loading="isLoadingTickets"
                    :error-message="ticketError"
                    show-page-badge
                    show-pagination
                    @update:current-page="handleTicketPageChange"
                    @view-ticket="handleViewTicket"
                />
            </div>
        </template>
    </div>
</template>

<style scoped>
.tabs-card {
    position: relative;
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    border: 2px solid var(--color-brand-purple);
    border-radius: 16px;
    overflow: hidden;
    background: var(--color-brand-purple);
    box-shadow: 0 6px 18px rgba(0, 0, 0, 0.05);
}

.tab-indicator {
    position: absolute;
    inset: 0;
    width: 50%;
    background: #ffffff;
    border-radius: 14px;
    box-shadow: 0 8px 20px rgba(0, 0, 0, 0.06);
    transition: transform 0.25s ease-in-out;
}

.tab-button {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 12px;
    padding: 14px 20px;
    font-size: 1.125rem;
    font-weight: 600;
    background: transparent;
    color: #ffffff;
    transition: color 0.2s ease;
    position: relative;
    z-index: 1;
}

@media (max-width: 640px) {
    .tab-button {
        padding: 10px 12px;
        font-size: 0.875rem;
        gap: 8px;
    }
}

.tab-button:not(.tab-active):hover {
    color: rgba(255, 255, 255, 0.85);
}

.tab-icon {
    width: 22px;
    height: 22px;
}

@media (max-width: 640px) {
    .tab-icon {
        width: 18px;
        height: 18px;
    }
}

.tab-active {
    color: var(--color-brand-purple);
}

.state-toggle {
    position: relative;
    display: grid;
    grid-template-columns: repeat(3, minmax(0, 1fr));
    border: 2px solid var(--color-brand-purple);
    border-radius: 10px;
    overflow: hidden;
    background: #ffffff;
    padding: 0;
    height: 40px;
    width: fit-content;
}

@media (max-width: 768px) {
    .state-toggle {
        width: 100%;
    }
}

.state-toggle-indicator {
    position: absolute;
    top: 0;
    bottom: 0;
    left: 0;
    background: var(--color-brand-purple);
    opacity: 1;
    transition:
        left 0.25s ease-in-out,
        width 0.25s ease-in-out;
    border-radius: 10px;
}

.state-toggle-btn {
    position: relative;
    z-index: 1;
    padding: 0 14px;
    height: 40px;
    display: inline-flex;
    align-items: center;
    justify-content: center;
    font-size: 0.875rem;
    color: var(--color-brand-purple);
    transition: color 0.2s ease;
}

.state-toggle-btn.state-active {
    color: #ffffff;
}
</style>
