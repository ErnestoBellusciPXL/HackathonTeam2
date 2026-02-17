<template>
    <div class="mx-auto max-w-5xl p-6 min-h-screen">
        <h1 class="text-2xl font-bold">Mijn Connecties</h1>
        <p class="mt-2 text-gray-600">Beheer je connecties met andere community leden</p>

        <!-- Search -->
        <div class="mt-4">
            <input
                v-model="searchQuery"
                type="text"
                placeholder="Zoek connecties"
                class="w-full border rounded px-3 py-2"
            />
        </div>

        <!-- Main tabs (full width, equal split) -->
        <div class="mt-4 flex gap-2 w-full">
            <button
                class="flex-1 text-center"
                :class="[
                    activeMainTab === 'connections'
                        ? 'bg-brand-purple text-white'
                        : 'bg-brand-purple/10 text-brand-purple',
                    'px-4 py-2 rounded',
                ]"
                @click="activeMainTab = 'connections'"
            >
                Connecties ({{ connections.length }})
            </button>
            <button
                class="flex-1 text-center"
                :class="[
                    activeMainTab === 'requests'
                        ? 'bg-brand-purple text-white'
                        : 'bg-brand-purple/10 text-brand-purple',
                    'px-4 py-2 rounded',
                ]"
                @click="activeMainTab = 'requests'"
            >
                Verzoeken ({{ receivedRequests.length + sentRequests.length }})
            </button>
        </div>

        <!-- List area with transitions -->
        <div class="mt-6 space-y-4">
            <!-- Main tab switch -->
            <Transition name="fade-slide" mode="out-in" appear>
                <div v-if="activeMainTab === 'connections'" key="connections">
                    <div v-if="connections.length === 0" class="text-gray-600">
                        Je hebt nog geen connecties.
                    </div>

                    <!-- Animate connection list changes -->
                    <TransitionGroup name="list" tag="div" class="space-y-4">
                        <div
                            v-for="c in filteredConnections"
                            :key="c.id"
                            class="border rounded p-4 flex items-center justify-between"
                        >
                            <div>
                                <div class="font-semibold">{{ c.username }}</div>
                                <div v-if="c.hasCondition" class="text-sm text-gray-600 mt-1">
                                    <span class="inline-block bg-gray-100 px-2 py-1 rounded text-xs">{{
                                        c.conditions?.[0]
                                    }}</span>
                                </div>
                            </div>
                            <div class="flex gap-3">
                                <Button3D
                                    variant="primary"
                                    class="px-3"
                                    :disabled="!c.chatId || processingRef[c.id]"
                                    :title="c.chatId ? 'Open chat' : 'Geen chat'"
                                    :class="
                                        !c.chatId || processingRef[c.id]
                                            ? 'opacity-50 cursor-not-allowed'
                                            : ''
                                    "
                                    @click="openChatId = c.chatId ?? null"
                                    id="OpenChatButton"
                                >
                                    Chat
                                </Button3D>
                                <Button3D
                                    variant="danger"
                                    class="px-3"
                                    :disabled="processingRef[c.id]"
                                    :class="processingRef[c.id] ? 'opacity-50 cursor-not-allowed' : ''"
                                    @click="disconnect(c.id)"
                                    id="DeleteConnectionButton"
                                >
                                    Verwijderen
                                </Button3D>
                            </div>
                        </div>
                    </TransitionGroup>
                </div>
            </Transition>

            <!-- Requests tab -->
            <Transition name="fade-slide" mode="out-in" appear>
                <div v-if="activeMainTab === 'requests'" key="requests">
                    <!-- Sub-tabs for requests: received / sent -->
                    <div class="mt-2 mb-4 flex gap-2 w-full">
                        <button
                            class="flex-1 text-center"
                            :class="[
                                requestsSubTab === 'received'
                                    ? 'bg-brand-purple text-white'
                                    : 'bg-brand-purple/10 text-brand-purple',
                                'px-3 py-1 rounded',
                            ]"
                            @click="requestsSubTab = 'received'"
                        >
                            Ontvangen ({{ receivedRequests.length }})
                        </button>
                        <button
                            class="flex-1 text-center"
                            :class="[
                                requestsSubTab === 'sent'
                                    ? 'bg-brand-purple text-white'
                                    : 'bg-brand-purple/10 text-brand-purple',
                                'px-3 py-1 rounded',
                            ]"
                            @click="requestsSubTab = 'sent'"
                        >
                            Verzonden ({{ sentRequests.length }})
                        </button>
                    </div>

                    <div v-if="currentRequests.length === 0" class="text-gray-600">
                        Geen verzoeken gevonden.
                    </div>

                    <!-- Animate list within sub-tabs without layout jump (height animation) -->
                    <Transition
                        name="fade"
                        @before-enter="beforeEnterHeight"
                        @enter="enterHeight"
                        @after-enter="afterEnterHeight"
                        @before-leave="beforeLeaveHeight"
                        @leave="leaveHeight"
                    >
                        <div :key="requestsSubTab" class="subtab-wrapper">
                            <TransitionGroup name="list" tag="div" class="space-y-4">
                                <div
                                    v-for="r in currentRequests"
                                    :key="r.requesterId + '-' + r.state"
                                    class="relative border rounded p-4 flex items-center justify-between"
                                >
                                    <!-- small [x] close control -->
                                    <button
                                        class="absolute right-2 top-2 h-7 w-7 flex items-center justify-center rounded hover:bg-gray-100 text-gray-400 hover:text-gray-600 transition-colors disabled:opacity-50"
                                        :disabled="processingRef[r.requesterId]"
                                        :title="isReceived(r) ? 'Weiger verzoek' : 'Annuleer verzoek'"
                                        aria-label="Sluit verzoek"
                                        @click="
                                            isReceived(r) ? decline(r.requesterId) : cancel(r.requesterId)
                                        "
                                    ></button>
                                    <div>
                                        <div class="font-semibold">{{ getRequestUsername(r) }}</div>
                                        <div class="text-sm text-gray-600 mt-1">Aandoening</div>
                                    </div>
                                    <div class="flex gap-3">
                                        <template v-if="isReceived(r)">
                                            <Button3D
                                                variant="primary"
                                                class="px-3"
                                                :disabled="processingRef[r.requesterId]"
                                                :class="
                                                    processingRef[r.requesterId]
                                                        ? 'opacity-50 cursor-not-allowed'
                                                        : ''
                                                "
                                                @click="accept(r.requesterId)"
                                            >
                                                Accepteer
                                            </Button3D>
                                            <Button3D
                                                variant="secondary"
                                                class="px-3"
                                                :disabled="processingRef[r.requesterId]"
                                                :class="
                                                    processingRef[r.requesterId]
                                                        ? 'opacity-50 cursor-not-allowed'
                                                        : ''
                                                "
                                                @click="decline(r.requesterId)"
                                            >
                                                Weiger
                                            </Button3D>
                                        </template>
                                        <template v-else>
                                            <Button3D
                                                variant="secondary"
                                                class="px-3"
                                                :disabled="processingRef[r.requesterId]"
                                                :class="
                                                    processingRef[r.requesterId]
                                                        ? 'opacity-50 cursor-not-allowed'
                                                        : ''
                                                "
                                                @click="cancel(r.requesterId)"
                                            >
                                                Annuleer
                                            </Button3D>
                                        </template>
                                    </div>
                                </div>
                            </TransitionGroup>
                        </div>
                    </Transition>
                </div>
            </Transition>
        </div>
        <!-- Floating chat panel -->
        <div v-if="openChatId" class="fixed bottom-6 right-6 z-50">
            <ChatComponent :chatId="openChatId" @close="openChatId = null" />
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, watch } from "vue";
import { storeToRefs } from "pinia";
import { useAuthStore } from "@/stores/auth";
import Button3D from "@/components/Button3D.vue";
import ChatComponent from "@/components/chat/ChatComponent.vue";
import { useConnectionsStore } from "@/stores/connections";

const authStore = useAuthStore();
const connectionsStore = useConnectionsStore();

const searchQuery = ref("");
const activeMainTab = ref("connections");
const requestsSubTab = ref("received");
const processing: Record<string, boolean> = {};
const processingRef = ref<Record<string, boolean>>(processing);
const openChatId = ref<string | null>(null);

interface ConnectionItem {
    id: string;
    username?: string;
    hasCondition?: boolean;
    conditions?: string[];
    chatId?: string;
}

interface RequestItem {
    requesterId: string;
    state: string;
    username?: string;
}

// Keep reactivity from the Pinia store on hard refresh by using storeToRefs
const { connections, requests } = storeToRefs(connectionsStore);

// make userId reactive so we fetch when auth becomes available (covers F5 reload)
const userId = computed(() => authStore.getCurrentUser()?.id ?? null);

async function fetchData() {
    await connectionsStore.fetchForUser(userId.value);
}

// When the component mounts and whenever the userId becomes available, fetch data.
// This handles the case where auth initializes asynchronously after a full page reload.
watch(
    userId,
    (id) => {
        if (id) fetchData();
    },
    { immediate: true },
);

const filteredConnections = computed<ConnectionItem[]>(() => {
    const conn = connections.value as ConnectionItem[];
    if (!searchQuery.value) return conn;
    return conn.filter((c: ConnectionItem) =>
        c.username?.toLowerCase().includes(searchQuery.value.toLowerCase()),
    );
});

const receivedRequests = computed(() => requests.value.filter((r: RequestItem) => r.state === "RECEIVED"));
const sentRequests = computed(() => requests.value.filter((r: RequestItem) => r.state === "SENT"));

const currentRequests = computed(() =>
    requestsSubTab.value === "received" ? receivedRequests.value : sentRequests.value,
);

function isReceived(r: RequestItem) {
    return r.state === "RECEIVED";
}

function getRequestUsername(r: RequestItem) {
    return r.username ?? r.requesterId;
}

async function accept(requesterId: string) {
    processingRef.value[requesterId] = true;
    try {
        if (!userId.value) return false;
        const ok = await connectionsStore.accept(requesterId, userId.value);
        // Ensure a fresh fetch after the action
        await fetchData();
        return ok;
    } finally {
        processingRef.value[requesterId] = false;
    }
}

async function decline(requesterId: string) {
    processingRef.value[requesterId] = true;
    try {
        if (!userId.value) return false;
        const ok = await connectionsStore.reject(requesterId, userId.value);
        await fetchData();
        return ok;
    } finally {
        processingRef.value[requesterId] = false;
    }
}

async function cancel(requesterId: string) {
    processingRef.value[requesterId] = true;
    try {
        if (!userId.value) return false;
        const ok = await connectionsStore.cancelSent(requesterId, userId.value);
        await fetchData();
        return ok;
    } finally {
        processingRef.value[requesterId] = false;
    }
}

async function disconnect(toUserId: string) {
    processingRef.value[toUserId] = true;
    try {
        if (!userId.value) return false;
        const ok = await connectionsStore.disconnect(userId.value, toUserId);
        await fetchData();
        return ok;
    } finally {
        processingRef.value[toUserId] = false;
    }
}

// Smooth height transition hooks for sub-tab switch to avoid layout jump
function beforeEnterHeight(el: Element) {
    const e = el as HTMLElement;
    e.style.overflow = "hidden";
    e.style.height = "0px";
    e.style.opacity = "0";
}

function enterHeight(el: Element, done: () => void) {
    const e = el as HTMLElement;
    e.style.transition = "height 220ms cubic-bezier(0.2, 0, 0, 1), opacity 160ms ease";
    // Next frame to ensure the starting style is applied
    requestAnimationFrame(() => {
        e.style.height = e.scrollHeight + "px";
        e.style.opacity = "1";
    });
    const onEnd = (ev: TransitionEvent) => {
        if (ev.propertyName === "height") {
            e.removeEventListener("transitionend", onEnd);
            done();
        }
    };
    e.addEventListener("transitionend", onEnd);
}

function afterEnterHeight(el: Element) {
    const e = el as HTMLElement;
    e.style.height = "auto";
    e.style.transition = "";
    e.style.overflow = "";
}

function beforeLeaveHeight(el: Element) {
    const e = el as HTMLElement;
    e.style.overflow = "hidden";
    e.style.height = e.scrollHeight + "px";
    e.style.opacity = "1";
}

function leaveHeight(el: Element, done: () => void) {
    const e = el as HTMLElement;
    e.style.transition = "height 220ms cubic-bezier(0.2, 0, 0, 1), opacity 140ms ease";
    requestAnimationFrame(() => {
        e.style.height = "0px";
        e.style.opacity = "0";
    });
    const onEnd = (ev: TransitionEvent) => {
        if (ev.propertyName === "height") {
            e.removeEventListener("transitionend", onEnd);
            done();
        }
    };
    e.addEventListener("transitionend", onEnd);
}
</script>

<style scoped>
/* Smooth fade + slight slide for tab switches */
.fade-slide-enter-active,
.fade-slide-leave-active {
    transition:
        opacity 220ms cubic-bezier(0.2, 0, 0, 1),
        transform 220ms cubic-bezier(0.2, 0, 0, 1);
}
.fade-slide-enter-from,
.fade-slide-leave-to {
    opacity: 0;
    transform: translateY(-6px);
}
.fade-slide-enter-to,
.fade-slide-leave-from {
    opacity: 1;
    transform: translateY(0);
}

/* Simple fade for sub-tab content */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 160ms ease;
}
.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

/* List item add/remove/move animations */
.list-enter-active,
.list-leave-active {
    transition:
        opacity 180ms ease,
        transform 180ms ease;
}
.list-enter-from,
.list-leave-to {
    opacity: 0;
    transform: translateY(4px) scale(0.995);
}
.list-move {
    transition: transform 180ms ease;
}

/* Prevent content jump during sub-tab switch */
.subtab-wrapper {
    overflow: hidden;
}
</style>
