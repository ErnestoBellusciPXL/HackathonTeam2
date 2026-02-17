import { defineStore } from "pinia";
import { ref } from "vue";
import { apiFetch } from "@/utils/Api";
import { BACKEND_URL } from "@/stores/config";
import { useToastStore } from "@/stores/toast";

interface ConnectionItem {
    id: string;
    username?: string;
    hasCondition?: boolean;
    conditions?: string[];
}

interface RequestItem {
    requesterId: string;
    state: string;
    username?: string;
}

export const useConnectionsStore = defineStore("connections", () => {
    const connections = ref<ConnectionItem[]>([]);
    const requests = ref<RequestItem[]>([]);
    const loading = ref(false);
    const toast = useToastStore();

    function getToken() {
        return localStorage.getItem("token");
    }

    async function fetchForUser(userId: string | null) {
        if (!userId) return;
        loading.value = true;
        try {
            const resp = await apiFetch(`${BACKEND_URL}/connection/${userId}`, {
                headers: {
                    Authorization: `Bearer ${getToken()}`,
                },
            });
            if (resp.ok) {
                const data = await resp.json();
                connections.value = data.connections ?? [];
                requests.value = (data.requests ?? []).map(
                    (r: {
                        requesterId: string;
                        state: string;
                        requesterUsername?: string;
                        username?: string;
                    }): RequestItem => ({
                        requesterId: r.requesterId,
                        state: r.state,
                        username: r.requesterUsername ?? r.username,
                    }),
                );
            } else {
                toast.error("Kon connecties niet laden");
            }
        } catch {
        } finally {
            loading.value = false;
        }
    }

    async function accept(requesterId: string, currentUserId: string) {
        if (!currentUserId) return false;
        try {
            const body = { fromUserId: currentUserId, toUserId: requesterId };
            const resp = await apiFetch(`${BACKEND_URL}/connection/accept`, {
                method: "POST",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${getToken()}` },
                body: JSON.stringify(body),
            });
            if (resp.ok) {
                toast.success("Verzoek geaccepteerd");
                // optimistic update: remove the received request and add to connections
                const req = requests.value.find((r) => r.requesterId === requesterId && r.state === "RECEIVED");
                requests.value = requests.value.filter((r) => !(r.requesterId === requesterId && r.state === "RECEIVED"));
                if (req) {
                    connections.value = [...connections.value, { id: requesterId, username: req.username }];
                }
                // refresh from backend to ensure consistency
                await fetchForUser(currentUserId);
                return true;
            }
            toast.error("Kon verzoek niet accepteren");
            return false;
        } catch {
            return false;
        }
    }

    async function reject(requesterId: string, currentUserId: string) {
        if (!currentUserId) return false;
        try {
            const body = { fromUserId: currentUserId, toUserId: requesterId };
            const resp = await apiFetch(`${BACKEND_URL}/connection/reject`, {
                method: "POST",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${getToken()}` },
                body: JSON.stringify(body),
            });
            if (resp.ok) {
                toast.success("Verzoek geweigerd");
                // optimistic update: remove the received request locally
                requests.value = requests.value.filter((r) => !(r.requesterId === requesterId && r.state === "RECEIVED"));
                await fetchForUser(currentUserId);
                return true;
            }
            toast.error("Kon verzoek niet weigeren");
            return false;
        } catch {
            return false;
        }
    }

    async function cancelSent(requesterId: string, currentUserId: string) {
        if (!currentUserId) return false;
        try {
            // when cancelling a sent request the current user is the sender (fromUserId)
            const body = { fromUserId: currentUserId, toUserId: requesterId };
            const resp = await apiFetch(`${BACKEND_URL}/connection/cancel`, {
                method: "POST",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${getToken()}` },
                body: JSON.stringify(body),
            });
            if (resp.ok) {
                toast.success("Verzoek geannuleerd");
                // remove the cancelled request locally for immediate UI feedback
                requests.value = requests.value.filter(
                    (r) => !(r.requesterId === requesterId && r.state === "SENT"),
                );
                // refresh from backend to ensure consistency
                await fetchForUser(currentUserId);
                return true;
            }
            toast.error("Kon verzoek niet annuleren");
            return false;
        } catch {
            return false;
        }
    }

    async function invite(fromUserId: string, toUserId: string) {
        if (!fromUserId || !toUserId) return false;
        if (fromUserId === toUserId) {
            toast.error("Je kunt jezelf niet uitnodigen");
            return false;
        }
        try {
            const body = { fromUserId, toUserId };
            const resp = await apiFetch(`${BACKEND_URL}/connection/invite`, {
                method: "POST",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${getToken()}` },
                body: JSON.stringify(body),
            });
            // Backend may return 200 OK for both success and logical errors.
            // Parse the response body message and decide whether it's success or error.
            let data: Record<string, unknown> | null = null;
            try {
                data = await resp.json();
            } catch {
                // ignore JSON parse errors
            }
            const msg = data?.message ? String(data.message).trim() : null;
            const successMsg = "Connection request sent";
            const pendingMsg = "Connection request already pending";
            const alreadyConnectedMsg = "Already connected";

            if (msg) {
                if (msg === successMsg) {
                    toast.success("Connectieverzoek verzonden");
                    // optimistic update: add a sent request locally
                    if (!requests.value.find((r) => r.requesterId === toUserId && r.state === "SENT")) {
                        requests.value = [...requests.value, { requesterId: toUserId, state: "SENT", username: undefined }];
                    }
                    await fetchForUser(fromUserId);
                    return true;
                }

                if (msg === pendingMsg) {
                    toast.error("Er is al een uitstaand connectieverzoek");
                    return false;
                }

                if (msg === alreadyConnectedMsg) {
                    toast.error("Je bent al verbonden");
                    return false;
                }
            }

            if (resp.ok) {
                toast.success("Connectieverzoek verzonden");
                if (!requests.value.find((r) => r.requesterId === toUserId && r.state === "SENT")) {
                    requests.value = [...requests.value, { requesterId: toUserId, state: "SENT", username: undefined }];
                }
                await fetchForUser(fromUserId);
                return true;
            }

            toast.error("Kon uitnodiging niet verzenden");
            return false;
        } catch {
            return false;
        }
    }

    async function disconnect(currentUserId: string, toUserId: string) {
        if (!currentUserId) return false;
        try {
            const body = { fromUserId: currentUserId, toUserId };
            const resp = await apiFetch(`${BACKEND_URL}/connection/disconnect`, {
                method: "POST",
                headers: { "Content-Type": "application/json", Authorization: `Bearer ${getToken()}` },
                body: JSON.stringify(body),
            });
            if (resp.ok) {
                toast.success("Verbinding verwijderd");
                // optimistic update: remove the connection locally
                connections.value = connections.value.filter((c) => c.id !== toUserId);
                await fetchForUser(currentUserId);
                return true;
            }
            toast.error("Kon verbinding niet verwijderen");
            return false;
        } catch {
            return false;
        }
    }

    return {
        connections,
        requests,
        loading,
        fetchForUser,
        accept,
        reject,
        cancelSent,
        invite,
        disconnect,
    };
});
