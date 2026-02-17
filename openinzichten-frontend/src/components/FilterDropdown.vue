<template>
    <div class="relative inline-block text-left w-full">
        <button
            type="button"
            @click="isOpen = !isOpen"
            class="w-full bg-white border border-gray-300 rounded-xl px-4 py-2 text-left flex items-center justify-between hover:border-brand-purple transition"
        >
            <span class="text-gray-700">{{ selectedLabel || placeholder }}</span>
            <svg
                class="h-5 w-5 text-gray-400 transition-transform"
                :class="{ 'rotate-180': isOpen }"
                xmlns="http://www.w3.org/2000/svg"
                viewBox="0 0 20 20"
                fill="currentColor"
            >
                <path
                    fill-rule="evenodd"
                    d="M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                    clip-rule="evenodd"
                />
            </svg>
        </button>
        <div
            v-if="isOpen"
            class="absolute z-10 mt-2 w-full bg-white border border-gray-200 rounded-xl shadow-lg max-h-60 overflow-auto"
        >
            <button
                v-for="option in options"
                :key="option.value"
                type="button"
                @click="selectOption(option)"
                class="w-full text-left px-4 py-2 hover:bg-brand-purple hover:text-white transition"
                :class="{
                    'bg-brand-purple text-white': modelValue === option.value,
                }"
            >
                {{ option.label }}
            </button>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";

interface Option {
    label: string;
    value: string;
}

const props = defineProps<{
    modelValue: string;
    options: Option[];
    placeholder?: string;
}>();

const emit = defineEmits<{
    "update:modelValue": [value: string];
}>();

const isOpen = ref(false);

const selectedLabel = computed(() => {
    const option = props.options.find((opt) => opt.value === props.modelValue);
    return option?.label || "";
});

function selectOption(option: Option) {
    emit("update:modelValue", option.value);
    isOpen.value = false;
}
</script>
