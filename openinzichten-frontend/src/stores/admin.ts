import { defineStore } from "pinia";
import { useAuthStore } from "./auth";
import { BACKEND_URL } from "./config";
import { apiFetch } from "@/utils/Api";
import type { AllUsersPagedResponse, AdminTicket, AdminTicketsPagedResponse } from "@/utils/Admin";

export const useAdminStore = defineStore("admin", () => {
    const authStore = useAuthStore();

    async function getAllUsersPaged(page: number = 0, size: number = 20): Promise<AllUsersPagedResponse> {
        if (!authStore.isUserAdmin()) {
            throw new Error("Unauthorized");
        }

        const response = await apiFetch(BACKEND_URL + "/admin/accounts?page=" + page + "&pageSize=" + size, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
            },
        });

        if (response.status === 200) {
            return (await response.json()) as AllUsersPagedResponse;
        } else {
            throw new Error("Failed to fetch users");
        }
    }

    async function getAllTicketsPaged(
        page: number = 0,
        size: number = 1000,
    ): Promise<AdminTicketsPagedResponse> {
        if (!authStore.isUserAdmin()) {
            throw new Error("Unauthorized");
        }

        const response = await apiFetch(`${BACKEND_URL}/admin/tickets?page=${page}&pageSize=${size}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
            },
        });

        if (response.ok) {
            return (await response.json()) as AdminTicketsPagedResponse;
        }

        throw new Error("Failed to fetch tickets");
    }

    async function getUserById(userId: string) {
        if (!authStore.isUserAdmin()) {
            throw new Error("Unauthorized");
        }

        const token = localStorage.getItem("token");
        if (!token) {
            throw new Error("Token ontbreekt. Log opnieuw in.");
        }

        const response = await apiFetch(`${BACKEND_URL}/admin/accounts/${userId}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
        });

        if (response.ok) {
            return (await response.json()) as any;
        }

        throw new Error("Failed to fetch user");
    }

    async function getStoriesByUserId(userId: string) {
        if (!authStore.isUserAdmin()) {
            throw new Error("Unauthorized");
        }

        const token = localStorage.getItem("token");
        if (!token) {
            throw new Error("Token ontbreekt. Log opnieuw in.");
        }

        const response = await apiFetch(`${BACKEND_URL}/stories/byuserid/${userId}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
        });

        if (response.ok) {
            return (await response.json()) as any[];
        }

        throw new Error("Failed to fetch stories");
    }

    async function setUserDisabled(userId: string, disabled: boolean, reason?: string) {
        if (!authStore.isUserAdmin()) {
            throw new Error("Unauthorized");
        }

        const token = localStorage.getItem("token");
        if (!token) {
            throw new Error("Token ontbreekt. Log opnieuw in.");
        }

        // Use POST to a dedicated admin endpoint to toggle disabled state.
        const body: any = { disabled };
        if (reason) body.reason = reason;

        const response = await apiFetch(`${BACKEND_URL}/admin/accounts/${userId}/disable`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(body),
        });

        if (response.ok) {
            return (await response.json()) as any;
        }

        throw new Error("Failed to update user");
    }

        async function adminResetPassword(userId: string): Promise<void> {
            if (!authStore.isUserAdmin()) throw new Error("Unauthorized");
            const token = localStorage.getItem("token");
            if (!token) throw new Error("Token ontbreekt. Log opnieuw in.");

            const response = await apiFetch(`${BACKEND_URL}/admin/accounts/${userId}/reset-password`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
            });

            if (response.ok) return;
            throw new Error("Failed to reset password");
        }

        async function adminSendEmail(userId: string, subject: string, body: string): Promise<void> {
            if (!authStore.isUserAdmin()) throw new Error("Unauthorized");
            const token = localStorage.getItem("token");
            if (!token) throw new Error("Token ontbreekt. Log opnieuw in.");

            const response = await apiFetch(`${BACKEND_URL}/admin/accounts/${userId}/send-email`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${token}`,
                },
                body: JSON.stringify({ subject, body }),
            });

            if (response.ok) return;
            throw new Error("Failed to send email");
        }

    async function getTicketById(ticketId: string): Promise<AdminTicket> {
        if (!authStore.isUserAdmin()) {
            throw new Error("Unauthorized");
        }

        const response = await apiFetch(`${BACKEND_URL}/admin/tickets/${ticketId}`, {
            method: "GET",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
            },
        });

        if (response.ok) {
            return (await response.json()) as AdminTicket;
        }

        throw new Error("Failed to fetch ticket");
    }

    async function closeTicket(ticketId: string, removeStory: boolean = false): Promise<AdminTicket> {
        if (!authStore.isUserAdmin()) {
            throw new Error("Unauthorized");
        }

        const response = await apiFetch(
            `${BACKEND_URL}/admin/tickets/${ticketId}/close?removeStory=${removeStory}`,
            {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${localStorage.getItem("token") || ""}`,
                },
            },
        );

        if (response.ok) {
            return (await response.json()) as AdminTicket;
        }

        throw new Error("Failed to close ticket");
    }

    return {
        getAllUsersPaged,
        getAllTicketsPaged,
        getTicketById,
        closeTicket,
        getUserById,
        setUserDisabled,
            adminResetPassword,
            adminSendEmail,
        getStoriesByUserId,
    };
});
