import { defineStore } from "pinia";
import { BACKEND_URL } from "./config";
import type { ZipcodeEntry } from "@/utils/ZipCode";
import type { ErrorResponse } from "@/utils/Error";
import { apiFetch } from "@/utils/Api";

export const useZipCodeStore = defineStore("zipcodes", {
    actions: {
        // Fetch zipcode list from backend. Returns ZipcodeEntry[] on success
        // or an ErrorResponse on failure.
        async fetchAll(): Promise<ZipcodeEntry[] | ErrorResponse> {
            try {
                const token = localStorage.getItem("token");
                const headers: Record<string, string> = {};
                if (token) headers["Authorization"] = `Bearer ${token}`;

                const res = await apiFetch(`${BACKEND_URL}/zipcodes`, { headers });

                if (!res.ok) {
                    const err: ErrorResponse = {
                        error: `Fout bij ophalen postcodes (${res.status}).`,
                        status: res.status,
                    };
                    return err;
                }

                const data: ZipcodeEntry[] = await res.json();
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
