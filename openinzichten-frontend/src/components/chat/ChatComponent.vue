<script setup lang="ts">
import {  ref, watch, onMounted, computed, nextTick } from "vue";
import { useChatStore } from "@/stores/chat";
import { useToastStore } from "@/stores/toast";

const props = defineProps<{ chatId?: string }>();
const chatStore = useChatStore();
const toast = useToastStore();
const messageInput = ref("");
const messagesContainer = ref<HTMLElement | null>(null);
// Bepaal ingelogde user id uit JWT token (zelfde aanpak als in store)
function getUserIdFromToken(): string | null {
    const token = localStorage.getItem("token");
    if (!token) return null;
    try {
        const base64Url = token.split(".")[1];
        if (!base64Url) return null;
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        const padLen = base64.length % 4;
        const padded = padLen ? base64 + "=".repeat(4 - padLen) : base64;
        const jsonPayload = decodeURIComponent(
            Array.prototype.map
                .call(
                    atob(padded),
                    (c: string) => "%" + ("00" + (c.codePointAt(0) ?? 0).toString(16)).slice(-2),
                )
                .join(""),
        );
        const payload = JSON.parse(jsonPayload) as Record<string, unknown>;
        return (payload.id || payload.userId || payload.sub) as string | null;
    } catch (e) {
        console.debug("Kon userId niet parsen uit token", e);
        return null;
    }
}

const currentUserId = getUserIdFromToken();

interface Message {
    id?: string;
    senderId?: string;
    senderUsername?: string;
    createdAt?: string | null;
    content?: string;
    [key: string]: unknown;
}

// (removed unused formatDate helper)

function formatTime(iso?: string | null) {
    if (!iso) return "";
    try {
        const d = new Date(iso);
        return Intl.DateTimeFormat("nl-NL", { timeStyle: "short" }).format(d);
    } catch {
        return iso;
    }
}

function formatDateHeader(dateKey: string) {
    try {
        const parts = dateKey.split("-").map(Number);
        const y = parts[0] || 1970;
        const m = (parts[1] || 1) - 1;
        const d = parts[2] || 1;
        const dt = new Date(y, m, d);
        return Intl.DateTimeFormat("nl-NL", { dateStyle: "medium" }).format(dt);
    } catch {
        return dateKey;
    }
}

const groupedMessages = computed(() => {
    const groups: Array<{ date: string; messages: Message[] }> = [];
    for (const raw of chatStore.messages) {
        const m = raw as Message;
        const created = m && m.createdAt ? new Date(m.createdAt) : new Date();
        const dateKey = created.toISOString().slice(0, 10); // YYYY-MM-DD
        const last = groups.at(-1) ?? null;
        if (!last || last.date !== dateKey) {
            groups.push({ date: dateKey, messages: [m] });
        } else {
            last.messages.push(m);
        }
    }
    return groups;
});

async function ensureLoaded() {
    if (props.chatId) {
        try {
            await chatStore.loadChat(props.chatId);
        } catch (e) {
            console.debug("Kon chat niet laden", e);
        }
    }
}

onMounted(() => {
    ensureLoaded().then(async () => {
        await nextTick();
        scrollToBottom(true);
    });
});

watch(
    () => props.chatId,
    async (newId) => {
        if (newId) await ensureLoaded();
        else {
            // clear local view
        }
    },
);

function scrollToBottom(force = false) {
    const el = messagesContainer.value;
    if (!el) return;
    if (force) {
        el.scrollTop = el.scrollHeight;
        return;
    }
    el.scrollTop = el.scrollHeight;
}

function isNearBottom(threshold = 120) {
    const el = messagesContainer.value;
    if (!el) return true;
    const distance = el.scrollHeight - el.scrollTop - el.clientHeight;
    return distance < threshold;
}

// Scroll to bottom when new messages are appended, unless user scrolled up
watch(
    () => chatStore.messages.length,
    async (newLen, oldLen) => {
        if (newLen === oldLen) return;
        await nextTick();
        if (isNearBottom()) scrollToBottom(true);
    },
);

async function send() {
    if (!props.chatId) return;
    const raw = messageInput.value ?? "";
    // Reject messages that are only whitespace
    if (raw.trim().length === 0) {
        toast.error("Bericht mag niet alleen uit spaties bestaan.");
        return;
    }
    // Enforce max length
    if (raw.length > 1000) {
        toast.error("Bericht mag maximaal 1000 tekens bevatten.");
        return;
    }

    const txt = raw.trim();
    try {
        await chatStore.sendMessage(props.chatId, txt);
        messageInput.value = "";
        // scroll to bottom after sending
        await nextTick();
        scrollToBottom(true);
    } catch (e) {
        console.error("Send failed", e);
        toast.error("Versturen mislukt. Probeer opnieuw.");
    }
}
</script>

<template>
    <div
        class="w-full h-full bg-white shadow-lg rounded-none border border-gray-200 flex flex-col md:w-96 md:h-96 z-50 md:static md:rounded-lg"
    >
        <div class="px-4 py-3 md:py-2 font-semibold border-b flex justify-between items-center">
            <div class="flex items-center gap-2">
                <button class="text-sm text-gray-700 hover:underline py-2 px-2 -ml-2 md:py-0 md:px-0 md:ml-0 md:mr-2" @click="$emit('back')">
                    &lt; Terug
                </button>
                <span class="font-semibold" id="ChatWithConnection">Chat</span>
            </div>
            <button class="text-xl text-gray-500 hover:text-gray-700 py-2 px-2 -mr-2 md:py-0 md:px-0 md:mr-0" @click="$emit('close')">✕</button>
        </div>

        <div ref="messagesContainer" class="overflow-y-auto flex-1 p-3">
            <div v-if="chatStore.loading" class="text-sm text-gray-500">Laden...</div>
            <div v-else-if="chatStore.messages.length === 0" class="text-sm text-gray-500">
                Geen berichten
            </div>
            <div v-else>
                <ul class="space-y-2">
                    <template v-for="group in groupedMessages" :key="group.date">
                        <li class="text-center text-xs text-gray-500 my-2">
                            {{ formatDateHeader(group.date) }}
                        </li>
                        <li v-for="m in group.messages" :key="m.id">
                            <div
                                :class="[
                                    'flex',
                                    m.senderId === currentUserId ? 'justify-end' : 'justify-start',
                                ]"
                            >
                                <div
                                    :class="[
                                        'px-3 py-2 rounded-lg max-w-[75%] whitespace-pre-wrap wrap-break-word',
                                        m.senderId === currentUserId
                                            ? 'bg-brand-purple text-white text-right shadow-md'
                                            : 'bg-white border border-gray-200 text-gray-800',
                                    ]"
                                >
                                    <div class="flex items-baseline justify-between gap-2">
                                        <div
                                            class="text-xs font-medium"
                                            :class="
                                                m.senderId === currentUserId
                                                    ? 'text-white/90'
                                                    : 'text-gray-600'
                                            "
                                        >
                                            {{ m.senderUsername }}
                                        </div>
                                        <div class="text-[11px] text-gray-400">
                                            {{ formatTime(m.createdAt) }}
                                        </div>
                                    </div>
                                    <div class="text-sm mt-1">{{ m.content }}</div>
                                </div>
                            </div>
                        </li>
                    </template>
                </ul>
            </div>
        </div>

        <div class="px-3 py-2 border-t">
            <div class="flex gap-2">
                <div class="flex-1 flex flex-col">
                    <div class="relative">
                        <input
                            v-model="messageInput"
                            @keyup.enter="send"
                            maxlength="1000"
                            aria-label="Bericht invoer"
                            class="w-full border rounded px-2 py-1 pr-12"
                            placeholder="Typ een bericht..."
                        />
                    </div>
                </div>
                <button
                    type="button"
                    @click="send"
                    id="sendChatButton"
                    data-test="send-chat-button"
                    tabindex="0"
                    class="bg-indigo-600 text-white px-3 py-1 rounded"
                    style="z-index: 210"
                >
                    Stuur
                </button>
            </div>
        </div>
    </div>
</template>
