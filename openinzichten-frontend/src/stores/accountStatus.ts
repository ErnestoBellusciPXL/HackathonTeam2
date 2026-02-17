import { defineStore } from "pinia";
import { ref } from "vue";
import { BACKEND_URL } from "./config";
import { apiFetch } from "@/utils/Api";
import { useAuthStore } from "@/stores/auth";

export const useAccountStatusStore = defineStore("accountStatus", () => {
    const disabled = ref(false);
    const disabledReason = ref<string | null>(null);
    const disabledAt = ref<string | null>(null);
    const lastChecked = ref<number | null>(null);

    async function checkMe(opts?: { suppressToast?: boolean }) {
        const token = localStorage.getItem("token");
        if (!token) return null;

        try {
            const resp = await apiFetch(BACKEND_URL + "/auth/me", {
                method: "GET",
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            }, { suppressToast: opts?.suppressToast ?? true }).catch(() => null as any);

            if (resp && resp.ok) {
                const body = await resp.json();
                disabled.value = !!body.disabled;
                disabledReason.value = body.disabledReason ?? body.message ?? null;
                disabledAt.value = body.disabledAt ? String(body.disabledAt) : null;
                lastChecked.value = Date.now();

                if (disabled.value) {
                    // If backend reports account disabled, log the user out (clear token)
                    try {
                        const auth = useAuthStore();
                        auth.reset();
                    } catch {}
                }

                return body;
            }
        } catch (e) {
            // ignore network/backend errors; callers decide what to do
        }

        return null;
    }

    return {
        disabled,
        disabledReason,
        disabledAt,
        lastChecked,
        checkMe,
    };
});

export default useAccountStatusStore;
