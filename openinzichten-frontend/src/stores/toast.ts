import { defineStore } from "pinia";

export type ToastType = "success" | "error" | "info" | "warning";

export interface ToastItem {
    id: number;
    type: ToastType;
    message: string;
    duration: number; // milliseconds
}

export const useToastStore = defineStore("toast", {
    state: () => ({
        toasts: [] as ToastItem[],
        nextId: 1,
    }),
    actions: {
        show(message: string, type: ToastType = "info", duration = 3000) {
            const id = this.nextId++;
            this.toasts.push({ id, type, message, duration });
            // Auto-remove after duration
            window.setTimeout(() => this.remove(id), duration);
            return id;
        },
        success(message: string, duration = 3000) {
            return this.show(message, "success", duration);
        },
        error(message: string, duration = 3000) {
            return this.show(message, "error", duration);
        },
        info(message: string, duration = 3000) {
            return this.show(message, "info", duration);
        },
        warning(message: string, duration = 3000) {
            return this.show(message, "warning", duration);
        },
        remove(id: number) {
            this.toasts = this.toasts.filter((t) => t.id !== id);
        },
        clear() {
            this.toasts = [];
        },
    },
});
