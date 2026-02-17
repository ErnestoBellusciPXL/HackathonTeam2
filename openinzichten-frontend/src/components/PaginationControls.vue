<template>
    <div class="flex items-center justify-center gap-2 mt-6">
        <button
            @click="goToPrevious"
            :disabled="currentPage === 1"
            class="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-brand-purple hover:text-white disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-current transition"
        >
            &lt;
        </button>

        <template v-for="item in visiblePages" :key="item">
            <button
                v-if="typeof item === 'number'"
                @click="goToPage(item)"
                class="w-8 h-8 flex items-center justify-center rounded-lg border transition"
                :class="
                    item === currentPage
                        ? 'bg-brand-purple text-white border-brand-purple'
                        : 'border-gray-300 hover:bg-brand-purple hover:text-white'
                "
            >
                {{ item }}
            </button>
            <span v-else class="w-8 h-8 flex items-center justify-center"> ... </span>
        </template>

        <button
            @click="goToNext"
            :disabled="currentPage === totalPages"
            class="w-8 h-8 flex items-center justify-center rounded-lg border border-gray-300 hover:bg-brand-purple hover:text-white disabled:opacity-30 disabled:cursor-not-allowed disabled:hover:bg-transparent disabled:hover:text-current transition"
        >
            &gt;
        </button>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = defineProps<{
    currentPage: number;
    totalPages: number;
}>();

const emit = defineEmits<{
    "update:currentPage": [page: number];
}>();

const visiblePages = computed(() => {
    const maxVisible = 5;

    if (props.totalPages <= maxVisible) {
        // Show all pages if total is less than or equal to max visible
        return Array.from({ length: props.totalPages }, (_, idx) => idx + 1);
    }

    if (props.currentPage <= 3) {
        // Near the start: show 1, 2, 3, ..., last
        return [1, 2, 3, "...", props.totalPages];
    }

    if (props.currentPage >= props.totalPages - 2) {
        // Near the end: show 1, ..., third-last, second-last, last
        const start = props.totalPages - 2;
        return [1, "...", start, start + 1, props.totalPages];
    }

    // In the middle: show 1, ..., current, ..., last
    return [1, "...", props.currentPage, "...", props.totalPages];
});

function goToPage(page: number) {
    if (page >= 1 && page <= props.totalPages) {
        emit("update:currentPage", page);
    }
}

function goToPrevious() {
    if (props.currentPage > 1) {
        emit("update:currentPage", props.currentPage - 1);
    }
}

function goToNext() {
    if (props.currentPage < props.totalPages) {
        emit("update:currentPage", props.currentPage + 1);
    }
}
</script>
