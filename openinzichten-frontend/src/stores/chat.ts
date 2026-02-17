import { defineStore } from "pinia";
import { ref } from "vue";
import { BACKEND_URL } from "./config";
import { Client } from "@stomp/stompjs";
import type { StompSubscription, IMessage } from "@stomp/stompjs";
import SockJS from "sockjs-client";

function buildSockJsUrl() {
    if (BACKEND_URL) {
        return `${BACKEND_URL.replace(/\/$/, "")}/ws`;
    }
    return "/ws";
}

export interface ChatOverviewItem {
    chatId: string;
    otherUsername: string;
    lastMessage: string;
    lastMessageTime: string; // ISO timestamp
}

export interface ChatMessage {
    id: string;
    chatId: string;
    senderUsername: string;
    senderId: string;
    content: string;
    createdAt: string; // ISO
    // Optional local-only flag while message is pending server confirmation
    pending?: boolean;
}

export const useChatStore = defineStore("chat", () => {
    const chats = ref<ChatOverviewItem[]>([]);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const currentChatId = ref<string | null>(null);
    const messages = ref<ChatMessage[]>([]);

    // STOMP / SockJS client and current subscription (store-level)
    let stompClient: Client | null = null;
    let currentSubscription: StompSubscription | null = null;
    let activeToken: string | null = null;

    // added: remember a chatId we'd like to (re)subscribe to after connect
    let pendingSubscribeChatId: string | null = null;

    // --- Helpers ---------------------------------------------------------

    // Ensure STOMP client is active and connected. Returns when connected.
    async function ensureStompConnected(token: string | null, connectTimeoutMs = 10000): Promise<void> {
        if (!token) throw new Error("No token provided");

        // If already connected with same token, resolve immediately
        if (stompClient && stompClient.connected && token === activeToken) return;

        // If a client exists and is active but not connected, deactivate to ensure new headers are used
        if (stompClient?.active) {
            try {
                await stompClient.deactivate();
            } catch {
                /* ignore */
            }
            stompClient = null;
            activeToken = null;
        }

        return new Promise((resolve, reject) => {
            const wsUrl = buildSockJsUrl();

            let settled = false;

            stompClient = new Client({
                webSocketFactory: () => new SockJS(wsUrl),
                connectHeaders: { Authorization: `Bearer ${token}` },
                reconnectDelay: 5000,
                heartbeatIncoming: 4000,
                heartbeatOutgoing: 4000,
                debug: (str) => console.debug("[stomp]", str),
                onConnect: (frame) => {
                    if (settled) return;
                    settled = true;
                    activeToken = token;
                    clearTimeout(timeoutHandle);
                    console.debug("[stomp] connected", frame);
                    // If we had a pending chat subscription, (re)subscribe now
                    if (pendingSubscribeChatId) {
                        try {
                            doSubscribeNow(pendingSubscribeChatId);
                        } catch (e) {
                            console.debug("[stomp] failed pending subscribe", e);
                        }
                    }
                    resolve();
                },
                onStompError: (frame) => {
                    if (settled) return;
                    settled = true;
                    clearTimeout(timeoutHandle);
                    console.error("[stomp] broker error", frame);
                    const body = (frame as unknown as { body?: string }).body;
                    reject(new Error(body || "STOMP error"));
                },
                onWebSocketClose: (evt) => {
                    console.info("[stomp] websocket closed", evt);
                    stompClient = null;
                    activeToken = null;
                },
                onWebSocketError: (evt) => {
                    console.error("[stomp] websocket error", evt);
                },
            });

            const timeoutHandle = setTimeout(() => {
                if (settled) return;
                settled = true;
                try {
                    stompClient?.deactivate();
                } catch {
                    /* ignore */
                }
                stompClient = null;
                activeToken = null;
                reject(new Error("STOMP connect timeout"));
            }, connectTimeoutMs);

            stompClient.activate();
        });
    }

    // Subscribe to a chat topic and handle incoming messages
    async function subscribeToChat(chatId: string) {
        const token = localStorage.getItem("token");
        if (!token) {
            console.warn("No token found in localStorage; cannot subscribe");
            return;
        }

        // Ensure STOMP connection (wait for it). If connect fails, keep pending so onConnect will resume.
        try {
            await ensureStompConnected(token);
        } catch (err) {
            console.error("Failed to connect STOMP:", err);
            pendingSubscribeChatId = chatId;
            return;
        }

        // Try to subscribe immediately; if it fails, mark pending so onConnect will try again
        if (doSubscribeNow(chatId)) {
            pendingSubscribeChatId = null;
            return;
        }
        pendingSubscribeChatId = chatId;
    }

    // helper that tries to subscribe now (returns true if subscribed)
    function doSubscribeNow(chatId: string): boolean {
        if (!stompClient?.connected) return false;

        try {
            // unsubscribe previous
            try {
                currentSubscription?.unsubscribe();
            } catch {
                /* ignore */
            }
            currentSubscription = null;

            currentSubscription = stompClient.subscribe(
                `/topic/chat/${chatId}`,
                (msg: IMessage) => {
                    try {
                        const body = JSON.parse(msg.body) as ChatMessage;
                        // reconcile or push
                        const matchedIdx = messages.value.findIndex((m) => {
                            if (!m.id || !m.content) return false;
                            if (!m.id.toString().startsWith("temp-")) return false;
                            if (m.content !== body.content) return false;
                            if (m.senderId && body.senderId && m.senderId !== body.senderId) return false;
                            try {
                                const a = new Date(m.createdAt).getTime();
                                const b = new Date(body.createdAt).getTime();
                                return Math.abs(a - b) < 15000;
                            } catch {
                                return false;
                            }
                        });
                        if (matchedIdx !== -1) {
                            messages.value[matchedIdx] = body;
                        } else {
                            messages.value.push(body);
                        }
                    } catch (err) {
                        console.error("Failed to parse chat message", err);
                    }
                },
                { id: `sub-chat-${chatId}` },
            );
            return true;
        } catch (e) {
            console.debug("[stomp] subscribe failed", e);
            return false;
        }
    }

    // Unsubscribe helper
    function unsubscribe() {
        try {
            currentSubscription?.unsubscribe();
        } catch {
            /* ignore */
        }
        currentSubscription = null;
    }

    // Deactivate stomp and cleanup
    function deactivateStomp() {
        try {
            if (stompClient) {
                stompClient.deactivate();
                stompClient = null;
            }
        } catch (e) {
            console.debug("[chat] error deactivating stomp", e);
        }
        try {
            currentSubscription?.unsubscribe();
        } catch {
            /* ignore */
        }
        currentSubscription = null;
        activeToken = null;
    }

    // --- API actions ----------------------------------------------------

    const toStr = (v: unknown) => (v === null || v === undefined ? "" : String(v));

    function clearChatState() {
        messages.value = [];
        currentChatId.value = null;
        unsubscribe();
    }

    function beginChatLoad() {
        loading.value = true;
        error.value = null;
    }

    function handleLoadChatError(e: unknown) {
        error.value = e instanceof Error ? e.message : String(e);
        clearChatState();
    }

    function unsubscribeOnChatSwitch(chatId: string) {
        if (currentChatId.value && currentChatId.value !== chatId) {
            unsubscribe();
        }
    }

    function parseMessagesFromResponse(data: unknown): ChatMessage[] | null {
        if (!data || !Array.isArray((data as Record<string, unknown>).messages)) return null;
        const list = (data as { messages: Array<Record<string, unknown>> }).messages;
        return list.map((m) => ({
            id: toStr(m["id"]),
            chatId: toStr(m["chatId"]),
            senderUsername: toStr(m["senderUsername"]),
            senderId: toStr(m["senderId"]),
            content: toStr(m["content"]),
            createdAt: toStr(m["createdAt"]),
        }));
    }

    async function fetchChatData(chatId: string, token: string) {
        const url = `${BACKEND_URL}/chat/${encodeURIComponent(chatId)}`;
        const resp = await fetch(url, {
            headers: {
                Authorization: `Bearer ${token}`,
                "Content-Type": "application/json",
            },
        });
        if (!resp.ok) throw new Error(`Kon chat niet laden (status ${resp.status})`);
        return resp.json();
    }

    function subscribeToChatSafely(chatId: string) {
        try {
            subscribeToChat(chatId).catch((e) => console.debug("[chat] subscribe failed", e));
        } catch (e) {
            console.debug("[chat] subscribe failed", e);
        }
    }

    // Load chat list quick overview for current user (reads userId from token)
    async function loadChats() {
        const token = localStorage.getItem("token");
        if (!token) {
            chats.value = [];
            return;
        }

        loading.value = true;
        error.value = null;

        // Decode JWT zoals in storyDetail.ts (ipv auth store) om userId te verkrijgen
        let userId: string | undefined;
        try {
            const base64Url = token.split(".")[1];
            if (!base64Url) throw new Error("Ongeldig token formaat.");
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
            userId = (payload.id || payload.userId || payload.sub) as string | undefined;
        } catch (e) {
            console.debug("[chat] kon userId niet parsen uit token", e);
        }

        if (!userId) {
            error.value = "Kon user id niet uit token halen.";
            loading.value = false;
            chats.value = [];
            return;
        }

        const url = `${BACKEND_URL}/chat/quickoverview/${encodeURIComponent(userId)}`;
        try {
            const resp = await fetch(url, {
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
            });
            if (!resp.ok) {
                throw new Error(`Kon chats niet laden (status ${resp.status})`);
            }
            const data = (await resp.json()) as ChatOverviewItem[];
            chats.value = Array.isArray(data)
                ? data.filter(
                      (d) =>
                          typeof d.chatId === "string" &&
                          typeof d.otherUsername === "string" &&
                          typeof d.lastMessage === "string" &&
                          typeof d.lastMessageTime === "string",
                  )
                : [];
        } catch (e) {
            const msg =
                e instanceof Error ? e.message : typeof e === "string" ? e : "Onbekende fout bij laden chats";
            error.value = msg;
            chats.value = [];
        } finally {
            loading.value = false;
        }
    }

    // Load a single chat's messages and subscribe to realtime updates for it
    async function loadChat(chatId: string) {
        const token = localStorage.getItem("token");
        if (!token) {
            clearChatState();
            return;
        }

        beginChatLoad();
        try {
            unsubscribeOnChatSwitch(chatId);
            const data = await fetchChatData(chatId, token);
            const parsed = parseMessagesFromResponse(data);
            if (!parsed) {
                clearChatState();
                return;
            }
            messages.value = parsed;
            currentChatId.value = chatId;
            subscribeToChatSafely(chatId);
        } catch (e) {
            handleLoadChatError(e);
        } finally {
            loading.value = false;
        }
    }

    // Send a message (optimistic update). Server should broadcast saved message via STOMP.
    async function sendMessage(chatId: string, message: string) {
        const token = localStorage.getItem("token");
        if (!token) throw new Error("Niet ingelogd");

        const parsePayload = (tk: string) => {
            try {
                const base64Url = tk.split(".")[1];
                if (!base64Url) return {} as Record<string, unknown>;
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
                return JSON.parse(jsonPayload) as Record<string, unknown>;
            } catch {
                return {} as Record<string, unknown>;
            }
        };

        const payload = parsePayload(token);
        const senderId = (payload.id || payload.userId || payload.sub) as string | undefined;
        const senderUsername = (payload.username || payload.preferred_username || payload.name) as
            | string
            | undefined;

        const tempMsg: ChatMessage = {
            id: `temp-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`,
            chatId,
            senderId: senderId || "",
            senderUsername: senderUsername || "",
            content: message,
            createdAt: new Date().toISOString(),
            pending: true,
        };

        // append locally
        messages.value.push(tempMsg);

        // send to server in background; if it fails, mark pending false
        try {
            const url = `${BACKEND_URL}/chat/send`;
            const resp = await fetch(url, {
                method: "POST",
                headers: {
                    Authorization: `Bearer ${token}`,
                    "Content-Type": "application/json",
                },
                body: JSON.stringify({ chatId, message }),
            });
            if (!resp.ok) {
                const idx = messages.value.findIndex((m) => m.id === tempMsg.id);
                if (idx !== -1) {
                    const mm = messages.value[idx];
                    if (mm) mm.pending = false;
                }
                throw new Error(`Kon bericht niet verzenden (status ${resp.status})`);
            }
            // success - server will broadcast saved message via STOMP and reconcile
        } catch (e) {
            throw e;
        }
    }

    // Expose store state and actions
    return {
        chats,
        loading,
        error,
        loadChats,
        currentChatId,
        messages,
        loadChat,
        sendMessage,
        unsubscribe,
        deactivateStomp,
    };
});
