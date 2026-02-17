<script setup lang="ts">
import { ref, onMounted, computed, watch } from "vue";
import { useRouter } from "vue-router";
import StoriesHeatMap from "@/components/heatmap/HeatMap.vue";
import { useStoryStore } from "@/stores/story";
import { StoryOrderBy, storySort, type StoryPayload } from "@/utils/Story";
import IconUser from "@/components/icons/IconUser.vue";
import FlameChecked from "@/assets/flame-checked.svg";
import FlameUnchecked from "@/assets/flame-unchecked.svg";
import Button3D from "@/components/Button3D.vue";
import { useHeatMapCounts } from "@/composables/useHeatMapCounts";
import { debounce } from "@/utils/Debounce";
import RecommendedStoriesSection from "@/components/RecommendedStoriesSection.vue";


const router = useRouter();
const storyStore = useStoryStore();
const MAX_CONDITIONS_SHOWN = 1;
const PLACEHOLDER_COUNT = 3;

const loading = ref(false);
const allStories = ref<StoryPayload[]>([]);
const hasMoreStories = ref(true);
// Authentication state derived from localStorage token presence
const isLoggedIn = ref<boolean>(!!localStorage.getItem("token"));
const { conditions, selectedCondition, loadConditions } = useHeatMapCounts();
const searchQuery = ref("");
const selectedOrder = ref<StoryOrderBy>(StoryOrderBy.ALL);

const filteredStories = computed(() => {
    // Stories are filtered and sorted by the backend
    return allStories.value;
});
// Banner text changes depending on auth state
const bannerText = computed(() => {
    if (!isLoggedIn.value) {
        return "Sluit je aan bij OpenInzicht en ontdek verhalen van mensen die hetzelfde meemaken. Word deel van een warme community waar ervaring en begrip centraal staan.";
    }
    return "Ontdek nieuwe en aanbevolen verhalen van onze community. Deel, lees en verbind met anderen die jou begrijpen.";
});

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

async function loadNewStories() {
    try {
        loading.value = true;
        // Fetch a larger page to ensure we have the most-liked stories available client-side.
        const resp = await storyStore.fetchStories(0, 3, "", undefined, StoryOrderBy.NEWEST);
        allStories.value = resp;
    } catch (e) {
        console.error("Failed to load recent stories:", e);
        allStories.value = [];
    } finally {
        loading.value = false;
    }
}

function navigateToStory(story: StoryPayload) {
    if (!story?.id) return;
    router.push({ name: "story-detail", params: { id: story.id } });
}

// Watch for search query changes
function onSearchQueryChange() {
    debouncedSearch();
}

// Search functionality
const debouncedSearch = debounce(async () => {
    navigateToStories();
}, 500);

async function navigateToStories() {
    router.push({
        name: "stories",
        query: {
            search: searchQuery.value || undefined,
            condition: selectedCondition.value !== "all" ? selectedCondition.value : "all",
            order: selectedOrder.value !== StoryOrderBy.ALL ? selectedOrder.value : StoryOrderBy.ALL,
        },
    });
}

// Watch for filter changes
watch(selectedCondition, () => {
    navigateToStories();
});

watch(selectedOrder, () => {
    navigateToStories();
});

onMounted(async () => {
    await loadConditions();
    loadNewStories();
});
</script>

<template>
    <!-- Welkomst-banner -->
    <section class="w-full bg-linear-to-r from-purple-700 via-purple-600 to-indigo-600 py-16 mb-6">
        <div class="max-w-5xl mx-auto text-center px-6">
            <h1 class="text-4xl md:text-5xl font-extrabold text-white mb-4">Welkom bij OpenInzicht</h1>
            <p class="text-white/90 max-w-2xl mx-auto mb-6">{{ bannerText }}</p>
            <div class="flex flex-col md:flex-row items-center justify-center gap-4">
                <Button3D v-if="!isLoggedIn" asTag="router-link" to="/register" variant="primary"
                    >Word lid</Button3D
                >
                <Button3D asTag="router-link" to="/stories" variant="primary">Lees verhalen</Button3D>
            </div>
        </div>
    </section>

    <RecommendedStoriesSection sectionId="popular-stories" />

    <div class="px-5 py-10">
        <h1 class="text-3xl md:text-4xl font-extrabold text-gray-900">Alle verhalen</h1>
        <p class="mt-2 text-gray-600 mb-10">Ontdek persoonlijke ervaringen</p>

        <!-- Search and Filters -->
        <div class="flex flex-col md:flex-row items-stretch md:items-center gap-4 mb-10">
            <!-- Search Input -->
            <div class="relative flex-1">
                <svg
                    class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400"
                    fill="none"
                    stroke="currentColor"
                    viewBox="0 0 24 24"
                >
                    <path
                        stroke-linecap="round"
                        stroke-linejoin="round"
                        stroke-width="2"
                        d="M21 21l-6-6m2-5a7 7 0 11-14 0 7 7 0 0114 0z"
                    />
                </svg>
                <input
                    v-model="searchQuery"
                    @input="onSearchQueryChange"
                    type="text"
                    placeholder="Zoek verhalen"
                    class="w-full pl-10 pr-10 h-10 border border-gray-300 rounded-[10px] bg-white text-gray-900 placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-brand-purple"
                />
                <button
                    v-if="searchQuery"
                    type="button"
                    class="absolute right-3 top-1/2 transform -translate-y-1/2 text-gray-400 hover:text-gray-600"
                >
                    <svg class="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M6 18L18 6M6 6l12 12"
                        ></path>
                    </svg>
                </button>
            </div>

            <!-- Condition Dropdown (reuses heatmap conditions) -->
            <div class="md:w-64 w-full">
                <div class="relative">
                    <svg
                        class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M8 9l4-4 4 4m0 6l-4 4-4-4"
                        />
                    </svg>
                    <select
                        id="condition-select"
                        v-model="selectedCondition"
                        class="w-full pl-10 pr-10 h-10 border border-gray-300 rounded-[10px] bg-white text-gray-900 text-sm focus:ring-2 focus:ring-brand-purple focus:border-brand-purple appearance-none"
                    >
                        <option value="all">Alle aandoeningen</option>
                        <option v-for="c in conditions" :key="c" :value="c">{{ c }}</option>
                    </select>
                </div>
            </div>

            <div class="md:w-64 w-full">
                <div class="relative">
                    <svg
                        class="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-gray-400 pointer-events-none"
                        fill="none"
                        stroke="currentColor"
                        viewBox="0 0 24 24"
                        aria-hidden="true"
                    >
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M8 9l4-4 4 4m0 6l-4 4-4-4"
                        />
                    </svg>
                    <select
                        id="order-select"
                        v-model="selectedOrder"
                        class="w-full pl-10 pr-10 h-10 border border-gray-300 rounded-[10px] bg-white text-gray-900 text-sm focus:ring-2 focus:ring-brand-purple focus:border-brand-purple appearance-none"
                    >
                        <option v-for="sort in storySort" :key="sort.value" :value="sort.value">
                            {{ sort.label }}
                        </option>
                    </select>
                </div>
            </div>
        </div>

        <!-- Stories Grid -->
        <div v-if="loading" class="space-y-6">
            <div
                v-for="index in PLACEHOLDER_COUNT"
                :key="`story-placeholder-${index}`"
                class="bg-white rounded-xl shadow-lg p-6 animate-pulse"
            >
                <div class="h-6 w-40 bg-gray-200 rounded mb-4"></div>
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
        <div v-else-if="filteredStories.length > 0" class="space-y-6">
            <article
                v-for="(story, index) in filteredStories"
                :key="index"
                class="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-shadow cursor-pointer"
                @click="navigateToStory(story)"
            >
                <!-- Story Header -->
                <div class="flex items-start justify-between gap-4 mb-4">
                    <div class="min-w-0">
                        <h2 class="text-2xl font-bold text-gray-900 wrap-break-word">
                            {{ story.title }}
                        </h2>
                    </div>
                    <div
                        class="flex items-center gap-2 text-base font-medium text-gray-500 pointer-events-none select-none shrink-0"
                    >
                        <img
                            :src="story.likedByCurrentUser ? FlameChecked : FlameUnchecked"
                            alt="Likes"
                            class="w-5 h-5"
                        />
                        <span class="text-base font-semibold text-gray-900">
                            {{ story.likeCount ?? 0 }}
                        </span>
                    </div>
                </div>

                <!-- Author Info -->
                <div class="flex items-center mb-4">
                    <IconUser class="w-10 h-10" />
                    <div class="ml-3">
                        <div class="font-medium text-gray-800">
                            {{ story.ownerUsername || "Anoniem" }}
                        </div>
                        <div
                            v-if="story.conditionNames && story.conditionNames.length > 0"
                            class="flex flex-wrap gap-2 mt-1 items-start"
                        >
                            <span
                                v-for="(conditionName, conditionIndex) in story.conditionNames.slice(
                                    0,
                                    MAX_CONDITIONS_SHOWN,
                                )"
                                :key="`${conditionName}-${conditionIndex}`"
                                class="inline-block bg-brand-purple text-white text-sm px-3 py-1 rounded-full"
                            >
                                {{ conditionName }}
                            </span>
                            <div
                                v-if="story.conditionNames.length > MAX_CONDITIONS_SHOWN"
                                class="relative inline-flex items-center group"
                            >
                                <span
                                    class="inline-block bg-white text-brand-purple border border-brand-purple text-sm px-3 py-1 rounded-full cursor-default"
                                >
                                    +{{ story.conditionNames.length - MAX_CONDITIONS_SHOWN }} meer
                                </span>
                                <div
                                    role="tooltip"
                                    class="pointer-events-none absolute z-10 w-max max-w-xs transform transition-all duration-150 ease-out opacity-0 scale-95 top-full left-1/2 -translate-x-1/2 mt-2 sm:mt-0 sm:top-1/2 sm:left-full sm:translate-x-0 sm:-translate-y-1/2 sm:ml-3 group-hover:opacity-100 group-hover:scale-100 group-hover:pointer-events-auto"
                                >
                                    <div
                                        class="absolute left-1/2 top-0 -translate-y-1 sm:top-1/2 sm:left-0 sm:-translate-x-1/2 sm:-translate-y-1/2"
                                    >
                                        <div class="w-2 h-2 bg-brand-purple rotate-45"></div>
                                    </div>
                                    <div
                                        class="bg-brand-purple text-white text-sm rounded-md px-3 py-1.5 shadow-lg"
                                    >
                                        <ul class="list-disc list-inside pl-0 ml-0 space-y-0.5">
                                            <li
                                                v-for="(
                                                    conditionName, conditionIndex
                                                ) in story.conditionNames.slice(MAX_CONDITIONS_SHOWN)"
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

                <!-- Story Preview -->
                <p class="text-gray-700 leading-relaxed mb-4 wrap-break-word">
                    {{ truncateContent(filterHtmlTags(story.content)) }}
                </p>

                <!-- Read More Link -->
                <a
                    href="#"
                    class="text-brand-purple hover:text-brand-purple-dark font-medium inline-flex items-center"
                    @click.prevent="navigateToStory(story)"
                >
                    Verder lezen
                    <svg class="w-4 h-4 ml-1" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path
                            stroke-linecap="round"
                            stroke-linejoin="round"
                            stroke-width="2"
                            d="M9 5l7 7-7 7"
                        />
                    </svg>
                </a>

                <!-- Interaction Stats (reserved) -->
            </article>
        </div>

        <!-- Empty State -->
        <div v-else-if="!loading && allStories.length === 0" class="text-center py-12">
            <p class="text-gray-600 text-lg">Geen verhalen gevonden.</p>
        </div>

        <!-- End of Stories Message -->
        <div v-if="!hasMoreStories && allStories.length > 0" class="text-center py-8">
            <p class="text-gray-600">Je hebt alle verhalen gelezen</p>
        </div>

        <div class="w-screen flex justify-center mt-10 mb-10">
            <Button3D variant="primary" id="go-to-story" @click="navigateToStories"> Lees Meer </Button3D>
        </div>
    </div>

    <div class="p-4">
        <h1 class="text-3xl md:text-4xl font-extrabold text-gray-900">Regiokaart</h1>
        <p class="mt-2 text-gray-600 mb-4">
            Deze heatmap laat zien dat er overal mensen zijn die begrijpen wat jij doormaakt. Filter op
            aandoening en ontdek dat je deel uitmaakt van iets groters.
        </p>

        <StoriesHeatMap />
    </div>
</template>
