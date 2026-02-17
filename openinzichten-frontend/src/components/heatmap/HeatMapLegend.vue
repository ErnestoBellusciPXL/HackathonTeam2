<template>
    <div
        class="absolute z-30 left-4 bottom-4 bg-white p-3 rounded shadow-md text-sm pointer-events-auto max-w-xs"
    >
        <div class="font-semibold mb-2 text-gray-800">Legende</div>

        <div v-if="(totalCount ?? maxCount) === 0" class="text-gray-500 text-xs">Geen data beschikbaar</div>

        <div v-else class="space-y-2">
            <!-- Gradient bar showing the full color scale (left: light, right: dark) -->
            <div
                class="w-full h-2 rounded-sm mb-2 border border-gray-200"
                :style="{
                    background: `linear-gradient(90deg, ${colorWithOpacity(LIGHT_HEX)} 0%, ${colorWithOpacity(DARK_HEX)} 100%)`,
                }"
            ></div>

            <div v-for="bucket in sortedBuckets" :key="bucket.label" class="flex items-center gap-2">
                <span
                    :style="{ background: colorWithOpacity(bucket.color) }"
                    class="w-6 h-4 rounded-sm border border-gray-300 flex-shrink-0"
                ></span>
                <span class="text-sm text-gray-700">{{ formatBucketLabel(bucket) }}</span>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { LIGHT_HEX, DARK_HEX } from "@/utils/provinceAggregation";

interface LegendBucket {
    color: string;
    label: string;
    min: number;
    max: number;
}

interface Props {
    buckets: LegendBucket[];
    maxCount: number;
    totalCount?: number;
}

const props = defineProps<Props>();

// Ensure buckets are displayed in ascending order
const sortedBuckets = computed(() => {
    return [...props.buckets].sort((a, b) => a.min - b.min);
});

// Match the heatmap's fill opacity for legend swatches
const LEGEND_OPACITY = 0.6;

function colorWithOpacity(color?: string): string {
    if (!color) return "transparent";
    const c = String(color).trim();
    if (c === "transparent") return "transparent";
    if (c.startsWith("rgb")) {
        const nums = c
            .replace("rgba(", "")
            .replace("rgb(", "")
            .replace(")", "")
            .split(",")
            .map((s: string) => s.trim());
        const r = nums[0] ?? "0";
        const g = nums[1] ?? "0";
        const b = nums[2] ?? "0";
        return `rgba(${r}, ${g}, ${b}, ${LEGEND_OPACITY})`;
    }
    let hex = c.replace("#", "");
    if (hex.length === 3) {
        hex = hex
            .split("")
            .map((ch) => ch + ch)
            .join("");
    }
    if (hex.length !== 6) {
        return c;
    }
    const r = Number.parseInt(hex.slice(0, 2), 16);
    const g = Number.parseInt(hex.slice(2, 4), 16);
    const b = Number.parseInt(hex.slice(4, 6), 16);
    return `rgba(${r}, ${g}, ${b}, ${LEGEND_OPACITY})`;
}

function formatBucketLabel(bucket: LegendBucket) {
    // If the bucket label is a special 'Geen data' message, return as-is
    if (!bucket || typeof bucket.label !== "string") return "";
    if (bucket.label.toLowerCase().includes("geen")) return bucket.label;

    // Singular only when the bucket represents exactly one item
    if (bucket.min === bucket.max && bucket.min === 1) {
        return `1 communitylid`;
    }

    // For all other buckets, use plural 'communityleden'. Keep the label (e.g. '0', '1+', '2-3')
    return `${bucket.label} communityleden`;
}
</script>
