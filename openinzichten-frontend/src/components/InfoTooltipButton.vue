<script setup lang="ts">
import { ref } from "vue";
import IconInfo from "./icons/IconInfo.vue";

defineProps<{
    tooltipText?: string;
}>();

const show = ref(false);
</script>

<template>
    <a
        type="button"
        class="relative inline-flex items-center p-1 text-gray-500 hover:text-gray-700 focus:outline-none"
        @mouseenter="show = true"
        @mouseleave="show = false"
        @focus="show = true"
        @blur="show = false"
        aria-describedby="info-tooltip"
    >
        <IconInfo class="w-5 h-5" />
        <div
            id="info-tooltip"
            role="tooltip"
            :aria-hidden="!show"
            class="pointer-events-none absolute z-10 w-max max-w-xs transform transition-all duration-150 ease-out opacity-0 scale-95"
            :class="{
                'opacity-100 scale-100 pointer-events-auto': show,
                'top-full left-1/2 -translate-x-1/2 mt-2 sm:top-1/2 sm:left-full sm:translate-x-0 sm:-translate-y-1/2 sm:ml-3': true,
            }"
        >
            <div
                class="absolute left-1/2 top-0 -translate-y-1 sm:top-1/2 sm:left-0 sm:-translate-x-1/2 sm:-translate-y-1/2"
            >
                <div class="w-2 h-2 bg-gray-800 rotate-45"></div>
            </div>

            <div class="bg-gray-800 text-white text-sm rounded-md px-3 py-2 shadow-lg">
                {{ tooltipText }}
            </div>
        </div>
    </a>
</template>
