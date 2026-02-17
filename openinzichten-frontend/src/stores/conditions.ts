import { defineStore } from "pinia";
import { BACKEND_URL } from "./config";
import type { Condition } from "@/utils/Condition";
import type { ErrorResponse } from "@/utils/Error";
import { apiFetch } from "@/utils/Api";

export const useConditionsStore = defineStore("conditions", {
    actions: {
        // Fetch all conditions from backend. Returns Condition[] on success
        // or an ErrorResponse on failure.
        async fetchAll(): Promise<Condition[] | ErrorResponse> {
            try {
                const res = await apiFetch(`${BACKEND_URL}/conditions`);

                if (!res.ok) {
                    const err: ErrorResponse = {
                        error: `Fout bij ophalen aandoeningen (${res.status}).`,
                        status: res.status,
                    };
                    return err;
                }

                const data: Condition[] = await res.json();
                return data;
            } catch (e) {
                const err: ErrorResponse = {
                    error: e instanceof Error ? e.message : String(e),
                };
                return err;
            }
        },

        // Fetch conditions linked to the currently authenticated user.
        async fetchForCurrentUser(): Promise<Condition[] | ErrorResponse> {
            try {
                const token = localStorage.getItem("token");
                const headers: Record<string, string> = { "Content-Type": "application/json" };
                if (token) headers.Authorization = `Bearer ${token}`;

                const res = await apiFetch(`${BACKEND_URL}/conditions/user-conditions`, { headers });

                if (!res.ok) {
                    const err: ErrorResponse = {
                        error: `Fout bij ophalen aandoeningen (${res.status}).`,
                        status: res.status,
                    };
                    return err;
                }

                const data: Condition[] = await res.json();
                return data;
            } catch (e) {
                const err: ErrorResponse = {
                    error: e instanceof Error ? e.message : String(e),
                };
                return err;
            }
        },
    },
});
