<script setup lang="ts">
import { storeToRefs } from "pinia";
import { useToastStore, type ToastItem } from "@/stores/toast";

const toastStore = useToastStore();
const { toasts } = storeToRefs(toastStore);

function remove(id: number) {
    toastStore.remove(id);
}

function classesForType(t: ToastItem["type"]) {
    switch (t) {
        case "success":
            return "bg-green-50 text-green-800 border-green-200";
        case "error":
            return "bg-red-50 text-red-800 border-red-200";
        case "warning":
            return "bg-yellow-50 text-yellow-800 border-yellow-200";
        default:
            return "bg-blue-50 text-blue-800 border-blue-200";
    }
}
</script>

<template>
    <div aria-live="polite" aria-atomic="true">
        <TransitionGroup
            name="toast"
            tag="div"
            class="fixed top-4 left-1/2 -translate-x-1/2 z-50 w-full max-w-xl px-4 space-y-3 pointer-events-none"
        >
            <div
                v-for="t in toasts"
                :key="t.id"
                class="pointer-events-auto rounded-md border shadow-lg px-4 py-3 flex items-start gap-3"
                :class="classesForType(t.type)"
            >
                <div class="flex-1">
                    <p class="font-medium" id="message-toasts">{{ t.message }}</p>
                </div>
                <button
                    type="button"
                    @click="remove(t.id)"
                    class="cursor-pointer ms-2 shrink-0 rounded p-1/2 text-current/70 hover:text-current focus:outline-none focus:ring-2 focus:ring-offset-2 focus:ring-offset-white"
                    aria-label="Sluit melding"
                >
                    ✕
                </button>
            </div>
        </TransitionGroup>
    </div>
</template>

<style scoped>
.toast-enter-from,
.toast-leave-to {
    opacity: 0;
    transform: translateY(-8px) scale(0.98);
}

.toast-enter-active,
.toast-leave-active {
    transition: all 200ms ease;
}

.toast-move {
    transition: transform 200ms ease;
}
</style>
