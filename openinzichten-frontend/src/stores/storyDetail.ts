import { defineStore } from "pinia";
import { ref } from "vue";
import { BACKEND_URL } from "./config";
import type { StoryPayload } from "@/utils/Story";
import { apiFetch } from "@/utils/Api";

export const useStoryDetailStore = defineStore("storyDetail", () => {
    const story = ref<StoryPayload | null>(null);
    const loading = ref(false);
    const error = ref<string | null>(null);
    const REQUEST_TIMEOUT_MS = 10000;

    async function fetchStory(id: string) {
        loading.value = true;
        error.value = null;
        // Clear any stale story to prevent UI from briefly rendering previous data
        story.value = null;

        // timeoutId needs to be visible in the finally block so we can clear it.
        let timeoutId: ReturnType<typeof setTimeout> | undefined;

        try {
            // debug: log fetch start so we can confirm the call is made
            // remove or change to a proper logger in production
            console.debug(`[storyDetail] fetchStory start id=${id}`);

            // Start timing so we can measure backend latency
            const start = Date.now();

            // Get token if present. For GET requests we avoid setting
            // Content-Type because `application/json` will trigger a CORS
            // preflight (OPTIONS) which adds extra latency. Only send
            // Authorization when available.
            const token = localStorage.getItem("token");
            const headers: Record<string, string> = {};
            if (token) headers.Authorization = `Bearer ${token}`;

            // Abort the fetch if it takes too long (timeout in ms).
            const controller = new AbortController();
            const timeoutMs = 10000; // 10s timeout — adjust as needed
            timeoutId = setTimeout(() => controller.abort(), timeoutMs);

            const res = await apiFetch(`${BACKEND_URL}/stories/${id}`, {
                headers,
                signal: controller.signal,
            });

            const duration = Date.now() - start;
            console.debug(`[storyDetail] fetchStory response status=${res.status} (took ${duration}ms)`);

            if (!res.ok) {
                error.value = `Kon verhaal niet laden (status ${res.status}).`;
                story.value = null;
                return;
            }

            story.value = await res.json();
        } catch (err: unknown) {
            // Distinguish abort/timeouts from other errors for clearer UI messages
            if (err instanceof DOMException && err.name === "AbortError") {
                error.value = `Verzoek time-out na ${10000 / 1000}s.`;
            } else {
                error.value = err instanceof Error ? err.message : String(err);
            }
            story.value = null;
        } finally {
            // clear timeout if still pending
            try {
                if (timeoutId) clearTimeout(timeoutId);
            } catch {
                // ignore
            }
            loading.value = false;
        }
    }

    async function sendLikeRequest(storyId: string, method: "POST" | "DELETE") {
        if (!storyId) {
            throw new Error("Geen geldig verhaal-id om te liken.");
        }

        const token = getAuthToken();
        const url = `${BACKEND_URL}/stories/${encodeURIComponent(storyId)}/likes`;
        const { controller, timeoutId } = createAbortControllerWithTimeout(REQUEST_TIMEOUT_MS);
        try {
            const res = await apiFetch(url, {
                method,
                headers: {
                    ...buildAuthHeaders(token),
                },
                signal: controller.signal,
            });

            if (!res.ok) {
                const action = method === "POST" ? "liken" : "unliken";
                throw new Error(`Kon het verhaal niet ${action} (status ${res.status}).`);
            }

            const data = (await res.json()) as StoryPayload;
            story.value = data;
            return data;
        } catch (err: unknown) {
            if (isAbortError(err)) {
                throw new Error(`Verzoek time-out na ${REQUEST_TIMEOUT_MS / 1000}s.`);
            }
            throw err instanceof Error ? err : new Error(String(err));
        } finally {
            try {
                clearTimeout(timeoutId);
            } catch {
                // ignore
            }
        }
    }

    async function likeStory(storyId: string) {
        return sendLikeRequest(storyId, "POST");
    }

    async function unlikeStory(storyId: string) {
        return sendLikeRequest(storyId, "DELETE");
    }

    /**
     * Helper: Retrieve token from localStorage or throw an error.
     */
    function getAuthToken(): string {
        const token = localStorage.getItem("token");
        if (!token) {
            throw new Error("Geen token gevonden. Niet ingelogd.");
        }
        return token;
    }

    /**
     * Helper: Decode JWT payload from base64url-encoded token.
     */
    function decodeJwtPayload(token: string): Record<string, unknown> {
        const base64Url = token.split(".")[1];
        if (!base64Url) throw new Error("Ongeldig token.");

        // base64url -> base64
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        // Add padding if necessary
        const pad = base64.length % 4;
        const padded = pad ? base64 + "=".repeat(4 - pad) : base64;
        const jsonPayload = decodeURIComponent(
            Array.prototype.map
                .call(atob(padded), function (c: string) {
                    return "%" + ("00" + (c.codePointAt(0) ?? 0).toString(16)).slice(-2);
                })
                .join(""),
        );

        return JSON.parse(jsonPayload) as Record<string, unknown>;
    }

    /**
     * Helper: Extract user ID from JWT payload.
     * Tries common claim names: id, userId.
     */
    function extractUserIdFromPayload(payload: Record<string, unknown>): string {
        const userId = (payload.id || payload.userId) as string | undefined;
        if (!userId) {
            console.debug("[storyDetail] token payload:", payload);
            throw new Error("Kon user id niet uit token halen.");
        }
        return userId;
    }

    /**
     * Helper: Create an AbortController with a timeout.
     * Returns the controller and timeout ID for cleanup.
     */
    function createAbortControllerWithTimeout(timeoutMs: number): {
        controller: AbortController;
        timeoutId: ReturnType<typeof setTimeout>;
    } {
        const controller = new AbortController();
        const timeoutId = setTimeout(() => controller.abort(), timeoutMs);
        return { controller, timeoutId };
    }

    /**
     * Helper: Build authorization headers with Bearer token.
     */
    function buildAuthHeaders(token: string): Record<string, string> {
        return { Authorization: `Bearer ${token}` };
    }

    /**
     * Helper: Normalize API response to a single StoryPayload or null.
     * Backend may return an array or a single object.
     */
    function normalizeStoryResponse(data: unknown): StoryPayload | null {
        if (Array.isArray(data)) {
            return data.length > 0 ? (data[0] as StoryPayload) : null;
        }
        return data as StoryPayload;
    }

    /**
     * Helper: Get current user auth context (token + userId).
     */
    function getUserAuthContext(): { token: string; userId: string } {
        const token = getAuthToken();
        const payload = decodeJwtPayload(token);
        const userId = extractUserIdFromPayload(payload);
        return { token, userId };
    }

    /**
     * Helper: Perform an authorized fetch with timeout handling.
     * Ensures the timeout is always cleared.
     */
    async function fetchWithAuth(
        url: string,
        token: string,
        timeoutMs = REQUEST_TIMEOUT_MS,
    ): Promise<Response> {
        const { controller, timeoutId } = createAbortControllerWithTimeout(timeoutMs);
        try {
            return await apiFetch(url, {
                headers: buildAuthHeaders(token),
                signal: controller.signal,
            });
        } finally {
            try {
                clearTimeout(timeoutId);
            } catch {
                /* ignore */
            }
        }
    }

    function isAbortError(err: unknown): boolean {
        return err instanceof DOMException && err.name === "AbortError";
    }

    /**
     * Fetch the story that belongs to the currently authenticated user.
     * This decodes the JWT in localStorage to determine the user id (sub or id)
     * and calls a backend endpoint to retrieve that user's story.
     *
     * NOTE: This assumes the backend exposes an endpoint at
     * `${BACKEND_URL}/stories/user/:userId`. If your API uses a different
     * route (for example `/stories/my`), update this function accordingly.
     */
    async function fetchMyStory() {
        loading.value = true;
        error.value = null;
        // Clear any stale story to prevent UI from briefly rendering previous data
        story.value = null;

        try {
            const { token, userId } = getUserAuthContext();
            const url = `${BACKEND_URL}/stories/byuserid/${encodeURIComponent(userId)}`;
            const res = await fetchWithAuth(url, token, REQUEST_TIMEOUT_MS);

            if (!res.ok) {
                error.value = `Kon verhaal niet laden (status ${res.status}).`;
                story.value = null;
                return;
            }

            const data = await res.json();
            console.debug("[storyDetail] fetchMyStory response shape:", data);
            story.value = normalizeStoryResponse(data);
        } catch (err: unknown) {
            error.value = isAbortError(err)
                ? `Verzoek time-out na ${REQUEST_TIMEOUT_MS / 1000}s.`
                : err instanceof Error
                  ? err.message
                  : String(err);
            story.value = null;
        } finally {
            loading.value = false;
        }
    }

    return { story, loading, error, fetchStory, fetchMyStory, likeStory, unlikeStory };
});
