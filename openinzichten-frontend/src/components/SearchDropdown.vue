<script setup lang="ts">
import { computed, ref, watch, onMounted } from "vue";
import InfoToolTipButton from "./InfoTooltipButton.vue";

type Item = { label: string; value: string };

const model = defineModel<string>({ default: "" });

const props = withDefaults(
    defineProps<{
        id?: string;
        label?: string;
        items: Item[];
        placeholder?: string;
        disabled?: boolean;
        showTooltip?: boolean;
        tooltipText?: string;
    }>(),
    {
        id: "",
        label: "",
        placeholder: "",
        disabled: false,
        showTooltip: false,
        tooltipText: "",
    },
);

const open = ref(false);
const query = ref("");
const inputEl = ref<HTMLInputElement | null>(null);
const focused = ref(false);

const selectedLabel = computed(() => props.items.find((i) => i.value === model.value)?.label || "");

watch(
    () => model.value,
    () => {
        if (!open.value) query.value = selectedLabel.value;
    },
    { immediate: true },
);

// Update query when items change and we have a selected value but no query yet
watch(
    () => props.items,
    () => {
        if (!query.value && selectedLabel.value) {
            query.value = selectedLabel.value;
        }
    },
    { deep: true }
);

const filtered = computed(() => {
    const q = query.value.trim().toLowerCase();
    if (!q) return props.items.slice(0, 50);
    return props.items.filter((i) => i.label.toLowerCase().includes(q)).slice(0, 50);
});

function onFocus() {
    focused.value = true;
    open.value = true;
}

function onBlur() {
    focused.value = false;
    // Delay closing so @click can trigger on results
    setTimeout(() => {
        open.value = false;
        // If nothing was selected, reset query to the current selection label
        if (!model.value || model.value === "") {
             query.value = "";
        } else {
             query.value = selectedLabel.value;
        }
    }, 200);
}

function select(item: Item) {
    model.value = item.value;
    query.value = item.label;
    open.value = false;
}

onMounted(() => {
    if (model.value && selectedLabel.value) {
        query.value = selectedLabel.value;
    }
});
</script>

<template>
    <div class="relative w-full mb-3">
        <input
            ref="inputEl"
            :id="id"
            type="text"
            :placeholder="placeholder"
            v-model="query"
            :disabled="disabled"
            @focus="onFocus"
            @blur="onBlur"
            autocomplete="off"
            class="peer w-full border border-gray-300 rounded-[10px] p-2 focus:outline-none focus:ring-2 focus:ring-brand-purple"
        />
        <label
            v-if="label && !query"
            :for="id"
            class="peer-focus:bg-white text-neutral-500 peer-focus:text-brand-purple-dark px-2 pointer-events-none absolute left-3 top-0 mb-0 max-w-[90%] origin-top-left truncate pt-[0.37rem] leading-[1.6] transition-all duration-200 ease-out peer-focus:-translate-y-[0.9rem] peer-focus:scale-[0.8] motion-reduce:transition-none"
        >
            {{ label }}
        </label>
        <div v-if="showTooltip && !query && !focused" class="absolute right-2 top-2">
            <InfoToolTipButton :tooltipText="tooltipText" />
        </div>
        <ul
            v-if="open && filtered.length > 0"
            class="absolute left-0 right-0 mt-1 max-h-64 overflow-y-auto bg-white border border-gray-200 rounded-[10px] shadow-lg z-50 scroll-stable"
        >
            <li
                v-for="item in filtered"
                :key="`${item.value}|${item.label}`"
                class="px-4 py-3 hover:bg-gray-100 cursor-pointer overflow-hidden text-ellipsis whitespace-nowrap text-sm border-b last:border-0 border-gray-100"
                @mousedown.prevent
                @click="select(item)"
            >
                {{ item.label }}
            </li>
        </ul>
        <div v-if="open && query && filtered.length === 0" class="absolute left-0 right-0 mt-1 p-3 bg-white border border-gray-200 rounded-[10px] shadow-lg z-50 text-gray-500 text-sm">
            Geen resultaten gevonden voor "{{ query }}"
        </div>
    </div>
</template>
