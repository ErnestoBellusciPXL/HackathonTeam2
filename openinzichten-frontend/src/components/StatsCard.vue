<template>
    <div class="border-2 border-brand-purple rounded-xl p-6 bg-white flex items-center justify-between">
        <div>
            <h3 class="text-gray-600 text-sm mb-2">{{ title }}</h3>
            <p class="text-3xl font-bold">{{ value }}</p>
        </div>
        <div class="text-4xl" :class="iconColorClass">
            <slot name="icon" />
        </div>
    </div>
</template>

<script setup lang="ts">
defineProps<{
    title: string;
    value: string | number;
    iconColorClass?: string;
}>();
</script>
