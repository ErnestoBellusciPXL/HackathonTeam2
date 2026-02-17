<script setup lang="ts">
import { ref, onMounted, onUnmounted } from "vue";
import ChatOverviewComponent from "./ChatOverviewComponent.vue";
import ChatComponent from "./ChatComponent.vue";
import IconChatBubble from "../icons/IconChatBubble.vue";
import { useChatStore } from "@/stores/chat";

declare global {
    interface GlobalThis {
        __localStorageTokenPatched?: boolean;
    }
}

const globalScope = globalThis as typeof globalThis & { __localStorageTokenPatched?: boolean };

const showOverview = ref(false);
const chatStore = useChatStore();
const selectedChatId = ref<string | null>(null);

function openChat(id: string) {
    selectedChatId.value = id;
    showOverview.value = false;
}

const hasToken = ref<boolean>(localStorage.getItem("token") !== null);

function toggleOverview() {
    // Lazy load chats when first opened
    if (!showOverview.value && chatStore.chats.length === 0) {
        chatStore.loadChats();
    }
    showOverview.value = !showOverview.value;
}

// React to storage changes (cross-tab) and to same-tab changes via a patched localStorage
const storageHandler = () => {
    hasToken.value = localStorage.getItem("token") !== null;
    if (!hasToken.value) {
        showOverview.value = false;
        selectedChatId.value = null;
    }
};
globalScope.addEventListener("storage", storageHandler);

// Patch localStorage in this window to dispatch a custom event when token changes.
function patchLocalStorageForTokenEvents() {
    if (globalScope.__localStorageTokenPatched) return;
    const originalSetItem = Storage.prototype.setItem;
    const originalRemoveItem = Storage.prototype.removeItem;

    Storage.prototype.setItem = function (key: string, value: string) {
        originalSetItem.apply(this, [key, value]);
        try {
            globalScope.dispatchEvent(
                new CustomEvent("localstorage-changed", { detail: { key, newValue: value } }),
            );
        } catch {}
    };

    Storage.prototype.removeItem = function (key: string) {
        originalRemoveItem.apply(this, [key]);
        try {
            globalScope.dispatchEvent(
                new CustomEvent("localstorage-changed", { detail: { key, newValue: null } }),
            );
        } catch {}
    };

    globalScope.__localStorageTokenPatched = true;
}

// Handler for same-tab localStorage changes
const localChangeHandler = (e: Event) => {
    const ev = e as CustomEvent<{ key: string; newValue: string | null }>;
    if (!ev?.detail) return;
    if (ev.detail.key === "token") {
        hasToken.value = ev.detail.newValue !== null;
        if (!hasToken.value) {
            showOverview.value = false;
            selectedChatId.value = null;
        }
    }
};

// Simple poll in case token set programmatically without storage event
onMounted(() => {
    const interval = setInterval(() => {
        const present = localStorage.getItem("token") !== null;
        if (present !== hasToken.value) {
            hasToken.value = present;
            if (!present) {
                showOverview.value = false;
                selectedChatId.value = null;
            }
        }
    }, 4000);

    // Patch localStorage to emit same-tab events and listen to them.
    patchLocalStorageForTokenEvents();
    globalScope.addEventListener("localstorage-changed", localChangeHandler as EventListener);

    onUnmounted(() => {
        clearInterval(interval);
        globalScope.removeEventListener("storage", storageHandler);
        globalScope.removeEventListener("localstorage-changed", localChangeHandler as EventListener);
    });
});

function closeChat() {
    selectedChatId.value = null;
}

function backToOverview() {
    selectedChatId.value = null;
    showOverview.value = true;
}
</script>

<template>
    <!-- Floating Chat Button -->
    <button
        v-if="hasToken"
        @click="toggleOverview"
        class="fixed bottom-6 z-40 right-6 bg-indigo-600 hover:bg-indigo-500 text-white rounded-full shadow-lg w-14 h-14 flex items-center justify-center"
        aria-label="Open chats"
        id="ChatIconButton"
    >
        <IconChatBubble class="w-7 h-7" />
    </button>

    <!-- Overview Panel (anchored bottom-right to the launcher button) -->
    <div
        v-if="showOverview"
        class="fixed inset-0 md:inset-auto md:bottom-6 md:right-6 md:left-auto md:top-auto z-50 flex items-stretch justify-stretch md:items-end md:justify-end"
    >
        <div class="flex-1 md:flex-none md:relative md:bottom-0 md:right-0">
            <ChatOverviewComponent @close="showOverview = false" @open="openChat" />
        </div>
    </div>

    <div
        v-if="selectedChatId"
        class="fixed inset-0 md:inset-auto md:bottom-6 md:right-6 md:left-auto md:top-auto z-50 flex items-stretch justify-stretch md:items-end md:justify-end"
    >
        <div class="flex-1 md:flex-none md:relative md:bottom-0 md:right-0">
            <ChatComponent :chatId="selectedChatId" @close="closeChat" @back="backToOverview" />
        </div>
    </div>
</template>
