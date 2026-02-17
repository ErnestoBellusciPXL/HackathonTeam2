<script setup lang="ts">
import HeatMap from "@/components/heatmap/HeatMap.vue";
</script>

<template>
    <div class="p-4">
        <h1 class="text-2xl font-bold">Regiokaart</h1>
        <p class="mt-2 text-gray-600 mb-4">
            Deze heatmap laat zien dat er overal mensen zijn die begrijpen wat jij doormaakt. Filter op
            aandoening en ontdek dat je deel uitmaakt van iets groters.
        </p>

        <HeatMap />
    </div>
</template>
