import { defineStore } from "pinia";
import { BACKEND_URL } from "./config";
import { apiFetch } from "@/utils/Api";
import type { ErrorResponse } from "@/utils/Error";

export interface UserAccountResponse {
    username: string;
    email: string;
    zipcode?: string | null;
    hasCondition?: boolean;
    conditions: string[];
}

export interface UpdateAccountPayload {
    username?: string;
    email?: string;
    zipcode?: string;
    conditions?: string[];
    hasCondition?: boolean;
}

export interface AccountUpdateResponse {
    message: string;
    token: string | null;
    account: UserAccountResponse;
}

export const useAccountStore = defineStore("account", () => {
    async function fetchAccount(): Promise<UserAccountResponse | ErrorResponse> {
        const token = localStorage.getItem("token");
        if (!token) {
            return { error: "Niet ingelogd. Log opnieuw in.", status: 401 };
        }

        try {
            const res = await apiFetch(BACKEND_URL + "/account", {
                headers: {
                    Authorization: `Bearer ${token}`,
                },
            });

            if (!res.ok) {
                const message = (await res.text()) || `Fout bij ophalen account (${res.status}).`;
                return { error: message, status: res.status };
            }

            return (await res.json()) as UserAccountResponse;
        } catch (e) {
            return {
                error: e instanceof Error ? e.message : "Onbekende fout bij ophalen account.",
            };
        }
    }

    async function updateAccount(
        payload: UpdateAccountPayload,
    ): Promise<AccountUpdateResponse | ErrorResponse> {
        const token = localStorage.getItem("token");
        if (!token) {
            return { error: "Niet ingelogd. Log opnieuw in.", status: 401 };
        }

        try {
            const res = await apiFetch(BACKEND_URL + "/account", {
                method: "PUT",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify(payload),
            });

            if (!res.ok) {
                const message = (await res.text()) || `Fout bij bijwerken (${res.status}).`;
                return { error: message, status: res.status };
            }

            const data = (await res.json()) as AccountUpdateResponse;

            return {
                message: data.message ?? "Account bijgewerkt.",
                token: data.token ?? null,
                account: data.account,
            };
        } catch (e) {
            return {
                error: e instanceof Error ? e.message : "Onbekende fout bij bijwerken account.",
            };
        }
    }

    async function deleteAccount(password: string, id: string): Promise<boolean> {
        try {
            const response = await apiFetch(BACKEND_URL + "/account/delete", {
                method: "DELETE",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
                },
                body: JSON.stringify({ password, userId: id }),
            });

            if (response.status === 200) {
                return true;
            } else {
                console.error("Failed to delete account:", await response.text());
                return false;
            }
        } catch {
            // apiFetch already showed a toast; return failure flag
            return false;
        }
    }

    return {
        fetchAccount,
        updateAccount,
        deleteAccount,
    };
});
