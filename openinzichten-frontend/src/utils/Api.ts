import { useToastStore } from "@/stores/toast";
import router from "@/router";
import { useAuthStore } from "@/stores/auth";

export interface ApiFetchOptions {
    /** Override the default generic error message. */
    errorMessage?: string;
    /** Suppress showing the toast on error if true. */
    suppressToast?: boolean;
}

/**
 * Wrapper around fetch that shows a global toast when the request fails
 * due to a network error (i.e. the Promise rejects). It does not
 * automatically toast on non-2xx HTTP statuses so existing error handling
 * in stores/views remains in control.
 */
export async function apiFetch(
    input: RequestInfo | URL,
    init?: RequestInit,
    options?: ApiFetchOptions,
): Promise<Response> {
    const toast = useToastStore();
    try {
        const resp = await fetch(input, init);

        // If backend returns 403 and indicates the account is disabled, redirect to disabled page.
        if (resp.status === 403) {
            try {
                const body = await resp.clone().json();
                const message = typeof body === "object" ? (body.message ?? body.error ?? "") : "";
                if (String(message).toLowerCase().includes("disabled")) {
                    // Redirect to disabled page and log the user out (clear token)
                    try {
                        const auth = useAuthStore();
                        auth.reset();
                    } catch {}
                    try {
                        router.push({ name: "disabled", query: { msg: message } });
                    } catch {}
                }
            } catch {
                // ignore JSON parse errors
            }
        }

        return resp;
    } catch (e) {
        if (!options?.suppressToast) {
            const message = options?.errorMessage ?? "Er is iets misgelopen, probeer later nog eens";
            // Show a friendly, generic error toast
            try {
                toast.error(message);
            } catch {
                // ignore toast errors (e.g. store not ready)
            }
        }
        throw e;
    }
}
