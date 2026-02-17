<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import { useStoryStore } from "@/stores/story";
import { useAuthStore } from "@/stores/auth";
import { StoryOrderBy, type StoryPayload } from "@/utils/Story";
import IconUser from "@/components/icons/IconUser.vue";
import FlameChecked from "@/assets/flame-checked.svg";
import FlameUnchecked from "@/assets/flame-unchecked.svg";
import RecommendedStoriesSection from "@/components/RecommendedStoriesSection.vue";
import { useHeatMapCounts } from "@/composables/useHeatMapCounts";
import { debounce } from "@/utils/Debounce";

const router = useRouter();
const route = useRoute();
const storyStore = useStoryStore();
const authStore = useAuthStore();

// State
const allStories = ref<StoryPayload[]>([]);
const currentPage = ref(0);
const loading = ref(false);
const hasMoreStories = ref(true);
const searchQuery = ref("");
const selectedOrder = ref<StoryOrderBy>(StoryOrderBy.ALL);
// Reuse heatmap logic to fetch and bind conditions
const { conditions, selectedCondition, loadConditions } = useHeatMapCounts();
const isSearchMode = ref(false);
const MAX_CONDITIONS_SHOWN = 2;

const storySort = ref([
    { label: "Geen Sortering", value: StoryOrderBy.ALL },
    { label: "Nieuwste", value: StoryOrderBy.NEWEST },
    { label: "Populair", value: StoryOrderBy.MOST_LIKED },
]);

// Computed
const filteredStories = computed(() => {
    // Stories are filtered and sorted by the backend
    return allStories.value;
});

// Methods
async function loadMoreStories(isSearch: boolean = isSearchMode.value) {
    if (loading.value || !hasMoreStories.value) return;

    loading.value = true;
    try {
        const query = isSearch ? searchQuery.value : "";
        const conditionParam =
            selectedCondition.value && selectedCondition.value !== "all"
                ? selectedCondition.value
                : undefined;
        const response: StoryPayload[] = await storyStore.fetchStories(
            currentPage.value,
            25,
            query,
            conditionParam,
            selectedOrder.value,
        );

        if (response.length > 0) {
            if (response.length < 25) {
                hasMoreStories.value = false;
            }
            allStories.value.push(...response);
            currentPage.value++;
        } else {
            hasMoreStories.value = false;
        }
    } catch (error) {
        console.error("Error loading stories:", error);
    } finally {
        loading.value = false;
    }
}

// Search functionality
const debouncedSearch = debounce(async () => {
    // Reset state when searching or clearing search
    currentPage.value = 0;
    allStories.value = [];
    hasMoreStories.value = true;
    isSearchMode.value = searchQuery.value.trim() !== "";

    // Load stories with or without search
    await loadMoreStories();
}, 500);

// Watch for search query changes
function onSearchQueryChange() {
    debouncedSearch();
}

// Clear search function
function clearSearch() {
    searchQuery.value = "";
    onSearchQueryChange();
}

// Function to reload stories when filters change
async function reloadStories() {
    currentPage.value = 0;
    allStories.value = [];
    hasMoreStories.value = true;
    await loadMoreStories();
}

// Watch for filter changes
watch(selectedCondition, () => {
    reloadStories();
});

watch(selectedOrder, () => {
    reloadStories();
});

function handleScroll() {
    const scrollPosition = window.innerHeight + window.scrollY;
    const threshold = document.documentElement.scrollHeight - 500; // Load 500px before bottom

    if (scrollPosition >= threshold && !loading.value && hasMoreStories.value) {
        loadMoreStories(isSearchMode.value);
    }
}

function navigateToStory(story: StoryPayload) {
    if (!story.id) {
        console.error("Story is missing an id:", story);
        return;
    }
    const currentUserId = authStore.getCurrentUser()?.id;
    const ownerId =
        (story as StoryPayload & { ownerUserId?: string | number; ownerId?: string | number }).ownerUserId ??
        (story as StoryPayload & { ownerId?: string | number }).ownerId;
    const normalizedOwnerId = ownerId != null ? String(ownerId) : null;
    if (currentUserId && normalizedOwnerId && String(currentUserId) === normalizedOwnerId) {
        router.push({ name: "my-story" });
        return;
    }
    router.push({ name: "story-detail", params: { id: story.id } });
}

function truncateContent(content: string | undefined, maxLength: number = 200): string {
    if (!content) return "";
    if (content.length <= maxLength) return content;

    // Truncate at maxLength and find the last space
    const truncated = content.substring(0, maxLength);
    const lastSpaceIndex = truncated.lastIndexOf(" ");

    // If there's a space, cut at the space; otherwise use the full truncated string
    if (lastSpaceIndex > 0) {
        return truncated.substring(0, lastSpaceIndex) + "...";
    }

    return truncated.trim() + "...";
}

function filterHtmlTags(content: string): string {
    const div = document.createElement("div");
    div.innerHTML = content;
    return div.textContent || div.innerText || "";
}

// Lifecycle
onMounted(async () => {
    await loadConditions();

    // Read URL query parameters and initialize state
    const hasQueryParams = route.query.search || route.query.condition || route.query.order;

    if (route.query.search) {
        searchQuery.value = route.query.search as string;
        isSearchMode.value = true;
    }

    if (route.query.condition && route.query.condition !== "all") {
        selectedCondition.value = route.query.condition as string;
    }

    if (route.query.order) {
        selectedOrder.value = route.query.order as StoryOrderBy;
    }

    // Only load stories manually if no query params are present
    // Otherwise, the watchers will trigger reloadStories()
    if (!hasQueryParams) {
        await loadMoreStories(isSearchMode.value);
    } else {
        // Trigger initial load with query params
        await reloadStories();
    }

    window.addEventListener("scroll", handleScroll);
});

onUnmounted(() => {
    window.removeEventListener("scroll", handleScroll);
});
</script>

<template>
    <div class="w-full py-10">
        <RecommendedStoriesSection
            class="mb-10"
            title="Voorgestelde verhalen"
            subtitle="Deze verhalen vind je misschien interessant om te lezen."
        />

        <div class="px-5">
            <!-- Header -->
            <h1 class="text-3xl md:text-4xl font-extrabold text-gray-900 mb-3">Alle verhalen</h1>
            <p class="text-gray-600 mb-8">Ontdek persoonlijke ervaringen</p>

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
                    @click="clearSearch"
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
        <div v-if="filteredStories.length > 0" class="space-y-6">
            <article
                v-for="(story, index) in filteredStories"
                :key="index"
                class="bg-white rounded-xl shadow-lg p-6 hover:shadow-xl transition-shadow cursor-pointer"
                @click="navigateToStory(story)"
                id="VerhaalGebruikerCard"
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

        <!-- Loading Indicator -->
        <div v-if="loading" class="space-y-4">
            <div
                v-for="index in 3"
                :key="`story-loading-${index}`"
                class="bg-white rounded-xl shadow-lg p-6 animate-pulse"
            >
                <div class="h-6 w-1/3 bg-gray-200 rounded mb-4"></div>
                <div class="flex items-center mb-4 gap-3">
                    <div class="w-10 h-10 bg-gray-200 rounded-full"></div>
                    <div class="space-y-2 w-full">
                        <div class="h-4 w-28 bg-gray-200 rounded"></div>
                        <div class="h-4 w-24 bg-gray-200 rounded"></div>
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

        <!-- End of Stories Message -->
        <div v-if="!hasMoreStories && allStories.length > 0" class="text-center py-8">
            <p class="text-gray-600">Je hebt alle verhalen gelezen</p>
        </div>
    </div>
</div>
</template>
