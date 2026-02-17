<script setup lang="ts">
import { computed } from "vue";
import IconUser from "@/components/icons/IconUser.vue";
import FlameChecked from "@/assets/flame-checked.svg";
import FlameUnchecked from "@/assets/flame-unchecked.svg";
import type { StoryPayload } from "@/utils/Story";

const props = defineProps<{
    story: StoryPayload;
    previewText: string;
    maxConditionsShown?: number;
    dataStoryCard?: boolean;
}>();

const emit = defineEmits<{
    (e: "select"): void;
}>();

const maxConditions = computed(() => props.maxConditionsShown ?? 1);
const extraConditionsCount = computed(
    () => Math.max(0, (props.story.conditionNames?.length ?? 0) - maxConditions.value),
);
</script>

<template>
    <article
        :data-story-card="dataStoryCard ? '' : undefined"
        class="bg-white rounded-xl shadow-lg p-4 hover:shadow-xl transition-shadow cursor-pointer flex flex-col h-[240px] md:h-[260px] overflow-hidden relative pb-10 story-card snap-start"
        @click="emit('select')"
    >
        <div class="flex-1 flex flex-col">
            <div class="flex items-start justify-between gap-1 mb-1">
                <div class="min-w-0">
                    <h2 class="text-xl font-bold text-gray-900 wrap-break-word title-text">
                        {{ story.title }}
                    </h2>
                </div>
                <div class="flex items-center gap-2 text-base font-medium text-gray-500 pointer-events-none select-none shrink-0">
                    <img :src="story.likedByCurrentUser ? FlameChecked : FlameUnchecked" alt="Likes" class="w-5 h-5" />
                    <span class="text-base font-semibold text-gray-900">{{ story.likeCount ?? 0 }}</span>
                </div>
            </div>

            <div class="flex items-center mb-1 meta-block">
                <IconUser class="w-10 h-10" />
                <div class="ml-3">
                    <div class="font-medium text-gray-800">{{ story.ownerUsername || "Anoniem" }}</div>
                    <div v-if="story.conditionNames && story.conditionNames.length > 0" class="flex flex-wrap gap-2 mt-1 items-start">
                        <span
                            v-for="(conditionName, conditionIndex) in story.conditionNames.slice(0, maxConditions)"
                            :key="`${conditionName}-${conditionIndex}`"
                            class="inline-block bg-brand-purple text-white text-sm px-3 py-1 rounded-full"
                        >
                            {{ conditionName }}
                        </span>
                        <div v-if="extraConditionsCount > 0" class="relative inline-flex items-center group">
                            <span
                                class="inline-block bg-white text-brand-purple border border-brand-purple text-sm px-3 py-1 rounded-full cursor-default"
                            >
                                +{{ extraConditionsCount }} meer
                            </span>
                            <div
                                role="tooltip"
                                class="pointer-events-none absolute z-10 w-max max-w-xs transform transition-all duration-150 ease-out opacity-0 scale-95 top-full left-1/2 -translate-x-1/2 mt-2 sm:mt-0 sm:top-1/2 sm:left-full sm:translate-x-0 sm:-translate-y-1/2 sm:ml-3 group-hover:opacity-100 group-hover:scale-100 group-hover:pointer-events-auto"
                            >
                                <div class="absolute left-1/2 top-0 -translate-y-1 sm:top-1/2 sm:left-0 sm:-translate-x-1/2 sm:-translate-y-1/2">
                                    <div class="w-2 h-2 bg-brand-purple rotate-45"></div>
                                </div>
                                <div class="bg-brand-purple text-white text-sm rounded-md px-3 py-1.5 shadow-lg">
                                    <ul class="list-disc list-inside pl-0 ml-0 space-y-0.5">
                                        <li
                                            v-for="(conditionName, conditionIndex) in story.conditionNames.slice(maxConditions)"
                                            :key="`remaining-${conditionName}-${conditionIndex}`"
                                            class="leading-tight"
                                        >
                                            {{ conditionName }}
                                        </li>
                                    </ul>
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="mt-auto preview-container">
                <p class="text-gray-700 leading-relaxed wrap-break-word preview-text">
                    {{ previewText }}
                </p>
            </div>
        </div>
        <a
            href="#"
            class="absolute left-4 bottom-3 text-brand-purple hover:text-brand-purple-dark font-medium inline-flex items-center"
            @click.prevent="emit('select')"
        >
            Verder lezen
            <svg class="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
            </svg>
        </a>
    </article>
</template>

<style scoped>
.preview-text {
    line-height: 1.5rem;
    display: -webkit-box;
    -webkit-line-clamp: 3;
    -webkit-box-orient: vertical;
    overflow: hidden;
    text-overflow: ellipsis;
}

.title-text {
    display: -webkit-box;
    -webkit-line-clamp: 2;
    -webkit-box-orient: vertical;
    overflow: hidden;
    min-height: 3rem;
}

.meta-block {
    min-height: 2rem;
}

.story-card {
    flex: 0 0 100%;
}

.preview-container {
    height: 5rem;
    overflow: hidden;
}

@media (min-width: 768px) {
    .story-card {
        flex: 0 0 calc((100% - 2rem) / 3);
    }
}
</style>
