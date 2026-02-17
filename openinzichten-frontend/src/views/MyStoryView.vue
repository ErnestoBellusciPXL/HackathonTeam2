<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import type { Ref } from "vue";
import { storeToRefs } from "pinia";
import { useRouter } from "vue-router";
import IconUser from "@/components/icons/IconUser.vue";
import { useStoryDetailStore } from "@/stores/storyDetail";
import type { StoryPayload } from "@/utils/Story";
import FlameChecked from "@/assets/flame-checked.svg";
import FlameUnchecked from "@/assets/flame-unchecked.svg";
import ShareIcon from "@/assets/share.svg";
import { useToastStore } from "@/stores/toast";
import { sanitizeHtml } from "@/utils/HTMLSanitizer";
import IconEdit from "@/components/icons/IconEdit.vue";
import Button3D from "@/components/Button3D.vue";

const router = useRouter();

const storyStore = useStoryDetailStore();
const toast = useToastStore();

// Use storeToRefs so we always get refs for reactive store properties
const { story, loading, error } = storeToRefs(storyStore) as {
    story: Ref<StoryPayload | null>;
    loading: Ref<boolean>;
    error: Ref<string | null>;
};

const togglingLike = ref(false);
const hasToken = computed(() => Boolean(localStorage.getItem("token")));
const isLiked = computed(() => Boolean(story.value?.likedByCurrentUser));
const likeCount = computed(() => story.value?.likeCount ?? 0);
const heartIcon = computed(() => (isLiked.value ? FlameChecked : FlameUnchecked));

// Navigate back to previous page. If there's no history, fallback to home.
const handleBack = (): void => {
    try {
        if (window.history.length > 1) {
            router.back();
        } else {
            router.push({ name: "home" });
        }
    } catch {
        // If router/back fails for any reason, navigate to home as a safe fallback
        router.push({ name: "home" });
    }
};

onMounted(async () => {
    // Fetch the currently authenticated user's story.
    await storyStore.fetchMyStory();

    if (!storyStore.story) {
        router.push({ name: "write" });
    }

    storyStore.story!.content = storyStore.story!.content.replace("<script>", "").replace("\<\/script>", "");
});

const sanitizedStory = computed(() => {
    if (!story.value || !story.value.content) return "";
    return sanitizeHtml(story.value.content);
});

const handleToggleLike = async () => {
    if (!hasToken.value) {
        toast.error("Je moet ingelogd zijn om te liken.");
        return;
    }
    const storyId = story.value?.id;
    if (!storyId) {
        toast.error("Kon verhaal-id niet bepalen.");
        return;
    }

    togglingLike.value = true;
    try {
        if (isLiked.value) {
            await storyStore.unlikeStory(storyId);
        } else {
            await storyStore.likeStory(storyId);
        }
    } catch (err: unknown) {
        const message = err instanceof Error ? err.message : "Kon like niet bijwerken.";
        toast.error(message);
    } finally {
        togglingLike.value = false;
    }
};

const handleShare = async () => {
    const id = story.value?.id;
    if (!id) return;
    const url = `${window.location.origin}/story/${id}`;
    try {
        await navigator.clipboard.writeText(url);
        toast.success("De link van dit verhaal is gekopieerd naar je klembord");
    } catch {
        toast.error("Kon link niet kopiëren.");
    }
};
</script>

<template>
    <div class="max-w-4xl mx-auto px-5 py-10">
        <a class="text-sm text-gray-700 block mb-3" href="#" @click.prevent="handleBack">&lt; Terug</a>

        <h1 class="text-3xl md:text-5xl font-extrabold text-gray-900 mb-6 wrap-break-word" id="story-title">
            {{ story?.title ?? "Geen titel gevonden" }}
        </h1>

        <section
            class="flex items-center justify-between border-2"
            :class="['border-[#7C4DFF]', 'rounded-xl', 'p-5', 'bg-white', 'mb-7']"
        >
            <div class="flex items-center">
                <IconUser class="w-11 h-11" />
                <div class="ml-4">
                    <div class="font-medium text-gray-800 flex items-center flex-wrap gap-2">
                        <span>{{ story?.ownerUsername ?? "Geen gebruiker gevonden" }}</span>
                        <span
                            v-if="story?.livesWith"
                            class="inline-block bg-[#22D6E6] text-white text-[11px] leading-none px-2 py-1 rounded-full"
                            >Leeft met</span
                        >
                    </div>
                    <div class="mt-2">
                        <div v-if="story?.conditionNames?.length" class="flex flex-wrap gap-2">
                            <span
                                v-for="(conditionName, conditionIndex) in story.conditionNames"
                                :key="`my-story-condition-${conditionName}-${conditionIndex}`"
                                class="inline-block bg-[#7C4DFF] text-white text-sm px-3 py-1 rounded-full"
                            >
                                {{ conditionName }}
                            </span>
                        </div>
                        <div
                            v-else
                            class="inline-block bg-[#7C4DFF] text-white text-sm px-3 py-1 rounded-full"
                        >
                            Geen aandoening gevonden
                        </div>
                    </div>
                </div>
            </div>

            <div class="flex items-center gap-3 text-base font-medium text-gray-700">
                <button
                    type="button"
                    class="flex items-center justify-center w-9 h-9 rounded-full border border-transparent hover:border-[#E4D7FF] hover:bg-[#F6F1FF] transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
                    :aria-pressed="isLiked"
                    :disabled="togglingLike"
                    @click="handleToggleLike"
                >
                    <img :src="heartIcon" alt="Like" class="w-5 h-5" />
                </button>

                <span class="text-base font-semibold text-gray-900">
                    {{ likeCount }}
                </span>
                <div class="relative group">
                    <button
                        type="button"
                        class="flex items-center justify-center w-9 h-9 rounded-full border border-transparent hover:border-[#E4D7FF] hover:bg-[#F6F1FF] transition-colors"
                        aria-label="Deel dit verhaal"
                        @click="handleShare"
                    >
                        <img :src="ShareIcon" alt="Deel" class="w-5 h-5" />
                    </button>
                    <span
                        class="pointer-events-none absolute -bottom-9 left-1/2 -translate-x-1/2 whitespace-nowrap rounded bg-gray-900 px-2 py-1 text-xs font-medium text-white opacity-0 transition-opacity group-hover:opacity-100"
                    >
                        Deel dit verhaal
                    </span>
                </div>
            </div>
        </section>

        <article class="text-gray-800 leading-relaxed text-base space-y-4 mb-8">
            <div v-if="loading" class="text-gray-600">Bezig met laden...</div>
            <div v-else-if="error" class="text-red-600">{{ error }}</div>
            <div v-else>
                <div
                    class="whitespace-pre-line wrap-break-word story-content"
                    id="story-content"
                    v-html="sanitizedStory"
                ></div>
            </div>
        </article>

        <div class="mt-2">
            <Button3D
                variant="primary"
                class="px-4"
                @click="$router.push({ name: 'write' })"
                id="BewerkVerhaal"
            >
                Bewerk Verhaal
                <IconEdit class="text-white ms-3"></IconEdit>
            </Button3D>
        </div>
    </div>
</template>

<style>
@import "../assets/story_style.css";
</style>
