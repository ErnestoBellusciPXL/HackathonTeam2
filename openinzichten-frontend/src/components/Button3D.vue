<template>
    <component
        :is="asTag"
        :type="asTag === 'button' ? type : undefined"
        :class="[
            'cursor-pointer inline-flex items-center justify-center rounded-xl px-5 py-3 font-semibold shadow-lg hover:shadow-xl active:translate-y-0.5 active:shadow-md transition',
            variantClasses,
        ]"
        v-bind="$attrs"
    >
        <slot />
    </component>
</template>

<script setup lang="ts">
import { computed } from "vue";

const props = withDefaults(
    defineProps<{
        asTag?: "button" | "a" | "router-link";
        type?: "button" | "submit" | "reset";
        variant?: "primary" | "secondary" | "danger";
    }>(),
    {
        asTag: "button",
        type: "button",
        variant: "primary",
    },
);

const variantClassMap: Record<string, string> = {
    primary: "bg-brand-cyan text-white border-b-4 border-brand-cyan-dark",
    // secondary: white background with purple text and subtle border
    secondary: "bg-brand-purple text-white border-b-4 border-brand-purple-dark",
    // danger: red background with darker red border
    danger: "bg-red-600 text-white border-b-4 border-red-700",
};

const variantClasses = computed(() => {
    return variantClassMap[props.variant as string] ?? variantClassMap.primary;
});

const { asTag, type } = props;
</script>
