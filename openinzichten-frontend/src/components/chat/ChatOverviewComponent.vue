<script setup lang="ts">
import { onMounted, computed } from "vue";
import { useChatStore } from "@/stores/chat";

const chatStore = useChatStore();

onMounted(() => {
    chatStore.loadChats();
});

// Sorted chats: most recent lastMessageTime on top
const sortedChats = computed(() => {
    return [...chatStore.chats].sort(
        (a, b) => new Date(b.lastMessageTime).getTime() - new Date(a.lastMessageTime).getTime(),
    );
});

function formatDate(iso: string) {
    const d = new Date(iso);
    return Intl.DateTimeFormat("nl-NL", { dateStyle: "short", timeStyle: "short" }).format(d);
}
</script>

<template>
    <div
        class="w-full h-full bg-white shadow-lg rounded-none border border-gray-200 flex flex-col md:w-96 md:h-96 z-50 md:static md:rounded-lg"
    >
        <div class="px-4 py-3 md:py-2 font-semibold border-b flex justify-between items-center">
            <span id="ChatsOverview">Chats</span>
            <button class="text-xl text-gray-500 hover:text-gray-700 py-2 px-2 -mr-2 md:py-0 md:px-0 md:mr-0" @click="$emit('close')">✕</button>
        </div>
        <div class="overflow-y-auto flex-1">
            <div v-if="sortedChats.length === 0" class="p-4 text-sm text-gray-500">Geen chats</div>
            <ul v-else>
                <li
                    v-for="c in sortedChats"
                    :key="c.chatId"
                    class="px-4 py-3 border-b hover:bg-gray-50 cursor-pointer select-none"
                    @click="$emit('open', c.chatId)"
                >
                    <div class="font-medium text-gray-800">{{ c.otherUsername }}</div>
                    <div class="text-xs text-gray-600 truncate">{{ c.lastMessage }}</div>
                    <div class="text-[10px] text-gray-400 mt-1">{{ formatDate(c.lastMessageTime) }}</div>
                </li>
            </ul>
        </div>
    </div>
</template>

<style scoped>
/* Optional minimal scroll styling */
.overflow-y-auto::-webkit-scrollbar {
    width: 6px;
}
.overflow-y-auto::-webkit-scrollbar-track {
    background: transparent;
}
.overflow-y-auto::-webkit-scrollbar-thumb {
    background: #d1d5db;
    border-radius: 3px;
}
</style>
