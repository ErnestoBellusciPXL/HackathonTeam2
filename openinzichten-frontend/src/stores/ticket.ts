import { defineStore } from "pinia";
import { BACKEND_URL } from "./config";
import { apiFetch } from "@/utils/Api";

export type TicketReportReason = "SPAM" | "HARASSMENT" | "INAPPROPRIATE_CONTENT" | "MISINFORMATION" | "OTHER";

export interface TicketResponse {
    id: string;
    type: "STORY";
    state: string;
    reportReason: TicketReportReason;
    otherReason?: string | null;
    storyId: string;
    storyTitle?: string | null;
    reporterId?: string;
    reporteeId?: string;
}

export const useTicketStore = defineStore("ticket", () => {
    function getToken(): string {
        const token = localStorage.getItem("token");
        if (!token) throw new Error("Je moet ingelogd zijn om een melding te doen.");
        return token;
    }

    async function fileStoryTicket(storyId: string, reportReason: TicketReportReason, otherReason?: string) {
        if (!storyId) throw new Error("Geen verhaal-id meegegeven voor de melding.");

        const payload = {
            type: "STORY" as const,
            reportReason,
            storyId,
            otherReason: reportReason === "OTHER" ? (otherReason ?? "") : null,
        };

        const resp = await apiFetch(
            `${BACKEND_URL}/tickets`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${getToken()}`,
                },
                body: JSON.stringify(payload),
            },
            { errorMessage: "We konden je melding nu niet versturen. Probeer het later opnieuw." },
        );

        if (resp.ok) {
            return (await resp.json()) as TicketResponse;
        }

        if (resp.status === 409) {
            throw new Error("Je hebt dit verhaal al gerapporteerd.");
        }

        throw new Error("Er liep iets mis, probeer later nog eens.");
    }

    return { fileStoryTicket };
});
