<script setup lang="ts">
import InfoToolTipButton from "./InfoTooltipButton.vue";
import IconEye from "./icons/IconEye.vue";
import IconEyeOff from "./icons/IconEyeOff.vue";
import { ref, computed } from "vue";

const model = defineModel<string>("modelValue");

const props = withDefaults(
    defineProps<{
        id: string;
        label: string;
        type?: string;
        inputStyle?: string;
        tooltipText?: string;
        showTooltip?: boolean;
        // when true (default) and type is password, an eye icon will toggle visibility
        showPasswordToggle?: boolean;
    }>(),
    {
        type: "text",
        inputStyle: "",
        tooltipText: "",
        showTooltip: false,
        showPasswordToggle: true,
    },
);

const { id, label, type, inputStyle, tooltipText, showTooltip, showPasswordToggle } = props;
const focused = ref(false);

const passwordVisible = ref(false);
const inputType = computed(() => {
    if (type === "password") {
        return passwordVisible.value ? "text" : "password";
    }
    return type;
});

// (no extra padding or tooltip offset — eye and tooltip are mutually exclusive)
</script>

<template>
    <!-- Consistent layout: icon always shown inside the input field -->
    <div class="relative mb-1" data-twe-input-wrapper-init>
        <input
            :type="inputType"
            :id="id"
            v-model="model"
            @focus="focused = true"
            @blur="focused = false"
            :class="[
                'peer border border-gray-300 rounded-4xl p-2 pr-10 focus:outline-none focus:ring-2 focus:ring-brand-purple w-100',
                inputStyle,
            ]"
        />

        <label
            :for="id"
            v-if="!model"
            class="peer-focus:bg-white text-neutral-500 peer-focus:text-brand-purple-dark px-2 pointer-events-none absolute left-3 top-0 mb-0 max-w-[90%] origin-top-left truncate pt-[0.37rem] leading-[1.6] transition-all duration-200 ease-out peer-focus:-translate-y-[0.9rem] peer-focus:scale-[0.8] peer-focus:text-primary peer-data-twe-input-state-active:-translate-y-[0.9rem] peer-data-twe-input-state-active:scale-[0.8] motion-reduce:transition-none"
        >
            {{ label }}
        </label>
        <span v-if="showTooltip && !model && !focused" class="absolute inset-y-0 right-2 flex items-center">
            <InfoToolTipButton :tooltipText="tooltipText" />
        </span>

        <!-- Password visibility toggle -->
        <button
            v-if="type === 'password' && showPasswordToggle && model"
            type="button"
            @click="passwordVisible = !passwordVisible"
            @mousedown.prevent
            :aria-pressed="passwordVisible"
            :aria-label="passwordVisible ? 'Verberg wachtwoord' : 'Toon wachtwoord'"
            class="absolute inset-y-0 right-2 flex items-center p-2 text-neutral-600 hover:text-neutral-800"
        >
            <IconEye v-if="!passwordVisible" />
            <IconEyeOff v-else />
        </button>
    </div>
</template>
