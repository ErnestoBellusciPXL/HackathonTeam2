<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from "vue";
import { useRouter } from "vue-router";
import { useStoryStore } from "@/stores/story";
import type { StoryPayload } from "@/utils/Story";
import RecommendedStoryCard from "@/components/RecommendedStoryCard.vue";
import Button3D from "@/components/Button3D.vue";

const MAX_CONDITIONS_SHOWN = 1;

const props = withDefaults(
    defineProps<{
        title?: string;
        subtitle?: string;
        sectionId?: string;
        placeholderCount?: number;
        showWriteCta?: boolean;
    }>(),
    {
        title: "Voorgestelde verhalen",
        subtitle: "Deze verhalen vind je misschien interessant om te lezen.",
        sectionId: undefined,
        placeholderCount: 3,
        showWriteCta: true,
    },
);

const router = useRouter();
const storyStore = useStoryStore();
const sliderRef = ref<HTMLElement | null>(null);

const stories = ref<StoryPayload[]>([]);
const loading = ref<boolean>(true);
const currentIndex = ref<number>(0);
const isAtStart = ref<boolean>(true);
const isAtEnd = ref<boolean>(false);
const shouldPaginate = computed(() => stories.value.length > 3);
const maxStartIndex = computed(() => Math.max(0, stories.value.length - 3));
const CARD_GAP_PX = 16;

function getPreviewText(story: StoryPayload) {
    return truncateContent(filterHtmlTags(story.content));
}

function truncateContent(content: string | undefined, maxLength: number = 220): string {
    if (!content) return "";
    if (content.length <= maxLength) return content;
    const truncated = content.substring(0, maxLength);
    const lastSpaceIndex = truncated.lastIndexOf(" ");
    if (lastSpaceIndex > 0) return truncated.substring(0, lastSpaceIndex) + "...";
    return truncated.trim() + "...";
}

function filterHtmlTags(content: string): string {
    const div = document.createElement("div");
    div.innerHTML = content;
    return div.textContent || div.innerText || "";
}

async function loadFallbackPopularStories() {
    try {
        const resp = await storyStore.fetchStories(0, 100);
        const sorted = resp.slice().sort((a, b) => (b.likeCount ?? 0) - (a.likeCount ?? 0));
        stories.value = sorted.slice(0, props.placeholderCount);
    } catch (e) {
        console.error("Failed to load popular stories fallback:", e);
        stories.value = [];
    }
}

async function loadRecommendedStories() {
    loading.value = true;
    try {
        const resp = await storyStore.fetchRecommendedStories();
        if (resp.length > 0) {
            stories.value = resp;
            return;
        }
        await loadFallbackPopularStories();
    } catch (e) {
        console.error("Failed to load recommended stories:", e);
        await loadFallbackPopularStories();
    } finally {
        loading.value = false;
    }
}

function navigateToStory(story: StoryPayload) {
    if (!story?.id) return;
    router.push({ name: "story-detail", params: { id: story.id } });
}

function getCardStep(container: HTMLElement): number {
    const card = container.querySelector<HTMLElement>("[data-story-card]");
    if (!card) return 0;
    return card.offsetWidth + CARD_GAP_PX;
}

function updateScrollBoundaries() {
    const container = sliderRef.value;
    if (!container) return;
    const maxScroll = container.scrollWidth - container.clientWidth;
    isAtStart.value = container.scrollLeft <= 1;
    isAtEnd.value = container.scrollLeft >= maxScroll - 1;
}

function scrollToIndex(nextIndex: number) {
    if (!shouldPaginate.value) return;
    const container = sliderRef.value;
    if (!container) return;
    const clampedIndex = Math.min(maxStartIndex.value, Math.max(0, nextIndex));
    const step = getCardStep(container);
    const targetLeft = step * clampedIndex;
    currentIndex.value = clampedIndex;
    container.scrollTo({ left: targetLeft, behavior: "smooth" });
    requestAnimationFrame(() => requestAnimationFrame(updateScrollBoundaries));
}

function nextSlide() {
    if (!shouldPaginate.value || currentIndex.value >= maxStartIndex.value || isAtEnd.value) return;
    scrollToIndex(currentIndex.value + 1);
}

function prevSlide() {
    if (!shouldPaginate.value || currentIndex.value <= 0 || isAtStart.value) return;
    scrollToIndex(currentIndex.value - 1);
}

function handleScroll() {
    if (!shouldPaginate.value) return;
    const container = sliderRef.value;
    if (!container) return;
    const step = getCardStep(container);
    if (step > 0) {
        const inferredIndex = Math.round(container.scrollLeft / step);
        currentIndex.value = Math.min(maxStartIndex.value, Math.max(0, inferredIndex));
    }
    updateScrollBoundaries();
}

watch(
    () => stories.value.length,
    (length) => {
        if (length <= 3 || currentIndex.value > maxStartIndex.value) {
            currentIndex.value = 0;
        }
        nextTick(updateScrollBoundaries);
    },
);

onMounted(() => {
    loadRecommendedStories();
    nextTick(updateScrollBoundaries);
});
</script>

<template>
    <section :id="sectionId" class="p-4">
        <h1 class="text-3xl md:text-4xl font-extrabold text-gray-900">{{ title }}</h1>
        <p class="mt-2 text-gray-600 mb-4">
            {{ subtitle }}
        </p>

        <div v-if="loading" class="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div
                v-for="index in placeholderCount"
                :key="`placeholder-${index}`"
                class="bg-white rounded-xl shadow-lg p-6 animate-pulse"
            >
                <div class="h-5 w-32 bg-gray-200 rounded mb-4"></div>
                <div class="flex items-center mb-4 gap-3">
                    <div class="w-10 h-10 bg-gray-200 rounded-full"></div>
                    <div class="space-y-2 w-full">
                        <div class="h-4 w-24 bg-gray-200 rounded"></div>
                        <div class="h-4 w-20 bg-gray-200 rounded"></div>
                    </div>
                </div>
                <div class="space-y-2 mb-4">
                    <div class="h-4 w-full bg-gray-200 rounded"></div>
                    <div class="h-4 w-11/12 bg-gray-200 rounded"></div>
                    <div class="h-4 w-10/12 bg-gray-200 rounded"></div>
                </div>
                <div class="h-4 w-24 bg-gray-200 rounded"></div>
            </div>
        </div>

        <div v-else-if="stories.length" class="mb-6">
            <div v-if="!shouldPaginate" class="mb-4">
                <div
                    class="flex gap-4 overflow-x-auto pb-4 px-1 scrollbar-hide snap-x snap-mandatory items-stretch md:hidden"
                >
                    <RecommendedStoryCard
                        v-for="story in stories"
                        :key="`mobile-${story.id}`"
                        :story="story"
                        :preview-text="getPreviewText(story)"
                        :max-conditions-shown="MAX_CONDITIONS_SHOWN"
                        @select="navigateToStory(story)"
                    />
                </div>

                <div class="hidden md:grid grid-cols-1 md:grid-cols-3 gap-4">
                    <RecommendedStoryCard
                        v-for="story in stories"
                        :key="story.id"
                        :story="story"
                        :preview-text="getPreviewText(story)"
                        :max-conditions-shown="MAX_CONDITIONS_SHOWN"
                        @select="navigateToStory(story)"
                    />
                </div>
            </div>

            <div v-else>
                <div class="relative">
                    <button
                        type="button"
                        class="absolute left-1 top-1/2 -translate-y-1/2 z-10 p-1 rounded-full border border-brand-purple/40 text-brand-purple transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:bg-brand-purple/10 hover:text-brand-purple bg-white/70 backdrop-blur"
                        :disabled="isAtStart"
                        @click.stop="prevSlide"
                        aria-label="Vorige verhalen"
                    >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
                        </svg>
                    </button>

                    <div
                        ref="sliderRef"
                        class="flex gap-4 overflow-x-auto pb-4 md:pb-6 px-1 md:px-2 scrollbar-hide snap-x snap-mandatory items-stretch"
                        @scroll.passive="handleScroll"
                    >
                        <RecommendedStoryCard
                            v-for="story in stories"
                            :key="story.id"
                            :story="story"
                            :preview-text="getPreviewText(story)"
                            :max-conditions-shown="MAX_CONDITIONS_SHOWN"
                            :data-story-card="true"
                            @select="navigateToStory(story)"
                        />
                    </div>

                    <button
                        type="button"
                        class="absolute right-1 top-1/2 -translate-y-1/2 z-10 p-1 rounded-full border border-brand-purple/40 text-brand-purple transition-colors disabled:opacity-40 disabled:cursor-not-allowed hover:bg-brand-purple/10 hover:text-brand-purple bg-white/70 backdrop-blur"
                        :disabled="isAtEnd"
                        @click.stop="nextSlide"
                        aria-label="Volgende verhalen"
                    >
                        <svg class="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9 5l7 7-7 7" />
                        </svg>
                    </button>
                </div>
            </div>
        </div>
        <div v-else class="bg-white rounded-xl shadow-lg p-6 mb-6 text-gray-700">
            Geen aanbevolen verhalen beschikbaar. Probeer het later opnieuw of bekijk alle verhalen in het overzicht.
        </div>

        <div v-if="showWriteCta" class="flex justify-center mb-4">
            <Button3D asTag="router-link" to="/write" variant="primary">Deel jouw verhaal</Button3D>
        </div>
    </section>
</template>

<style scoped>
.scrollbar-hide {
    -ms-overflow-style: none;
    scrollbar-width: none;
}

.scrollbar-hide::-webkit-scrollbar {
    display: none;
}
</style>
