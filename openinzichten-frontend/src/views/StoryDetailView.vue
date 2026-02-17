<script setup lang="ts">
import { computed, onMounted, ref, watch } from "vue";
import type { Ref } from "vue";
import { storeToRefs } from "pinia";
import { useRoute, useRouter } from "vue-router";
import IconUser from "@/components/icons/IconUser.vue";
import Button3D from "@/components/Button3D.vue";
import { useStoryDetailStore } from "@/stores/storyDetail";
import { sanitizeHtml } from "@/utils/HTMLSanitizer";
import type { StoryPayload } from "@/utils/Story";
import { useAuthStore } from "@/stores/auth";
import { useConnectionsStore } from "@/stores/connections";
import { useTicketStore, type TicketReportReason } from "@/stores/ticket";
import { useToastStore } from "@/stores/toast";
import AlertSquare from "@/assets/alert-square.svg";
import FlameChecked from "@/assets/flame-checked.svg";
import FlameUnchecked from "@/assets/flame-unchecked.svg";
import ShareIcon from "@/assets/share.svg";

const route = useRoute();
const router = useRouter();

const storyId = computed(() => (route.params.id ? String(route.params.id) : null));

const storyStore = useStoryDetailStore();
const authStore = useAuthStore();
const connectionsStore = useConnectionsStore();
const ticketStore = useTicketStore();
const toast = useToastStore();

const { story, loading, error } = storeToRefs(storyStore) as {
    story: Ref<StoryPayload | null>;
    loading: Ref<boolean>;
    error: Ref<string | null>;
};

// Clear any stale story immediately on component creation to avoid initial render using old data
story.value = null;

const togglingLike = ref(false);
const isLiked = computed(() => Boolean(story.value?.likedByCurrentUser));
const likeCount = computed(() => story.value?.likeCount ?? 0);
const heartIcon = computed(() => (isLiked.value ? FlameChecked : FlameUnchecked));
const showAuthModal = ref(false);
const showReportModal = ref(false);
const submittingReport = ref(false);
const selectedReportReason = ref<TicketReportReason>("SPAM");
const otherReportReason = ref("");
// Character limits for ticket description when reason is OTHER
const OTHER_REASON_MIN = 5;
const OTHER_REASON_MAX = 250;
const reportReasons: { value: TicketReportReason; label: string }[] = [
    { value: "SPAM", label: "Spam of misleidende inhoud" },
    { value: "HARASSMENT", label: "Pesten of intimidatie" },
    { value: "INAPPROPRIATE_CONTENT", label: "Ongepaste inhoud" },
    { value: "MISINFORMATION", label: "Misleidende of foute informatie" },
    { value: "OTHER", label: "Iets anders" },
];
const isOwnStory = computed(() =>
    Boolean(ownerUserId.value && currentUserId.value && ownerUserId.value === currentUserId.value),
);

const hasToken = computed(() => Boolean(localStorage.getItem("token")));

// Current logged-in user info from JWT (normalize to string to avoid '1' vs 1 flicker)
const currentUserId = computed(() => {
    const id = (authStore.getCurrentUser()?.id as unknown) ?? null;
    return id != null ? String(id) : null;
});
// Try to read owner user id from the story payload. We accept multiple common keys.
// Normalize to string as story APIs may return numeric ids.
const ownerUserId = computed(() => {
    const s = story.value as
        | (StoryPayload & { ownerUserId?: string | number; ownerId?: string | number })
        | null;
    const raw = s?.ownerUserId ?? s?.ownerId;
    return raw != null ? String(raw) : null;
});

// Only consider the story ready when it matches the current route param
const storyLoaded = computed(() => Boolean(story.value?.id && story.value.id === storyId.value));

const canInviteVisible = computed(() => {
    if (!hasToken.value) return false;
    // Avoid flicker: wait until the correct story is loaded for this route
    if (!storyLoaded.value || loading.value) return false;
    if (!currentUserId.value) return false;
    if (!ownerUserId.value) return false;
    return ownerUserId.value !== currentUserId.value;
});

const canInviteEnabled = computed(() => !!ownerUserId.value && ownerUserId.value !== currentUserId.value);
const inviteLoading = ref(false);

async function sendInvite() {
    if (!currentUserId.value || !ownerUserId.value) return;
    inviteLoading.value = true;
    try {
        await connectionsStore.invite(currentUserId.value, ownerUserId.value);
    } finally {
        inviteLoading.value = false;
    }
}

const handleSubmit = (): void => {
    router.push({ name: "register" });
};

const handleBack = (): void => {
    try {
        if (window.history.length > 1) {
            router.back();
        } else {
            router.push({ name: "home" });
        }
    } catch {
        router.push({ name: "home" });
    }
};

const openAuthModal = () => {
    showAuthModal.value = true;
};

const closeAuthModal = () => {
    showAuthModal.value = false;
};

const goToLogin = () => {
    router.push({ name: "login", query: { redirect: route.fullPath } });
    closeAuthModal();
};

const goToRegister = () => {
    router.push({ name: "register" });
    closeAuthModal();
};

const openReportModal = () => {
    if (isOwnStory.value) {
        toast.error("Je kunt je eigen verhaal niet rapporteren.");
        return;
    }
    if (!hasToken.value) {
        openAuthModal();
        return;
    }
    showReportModal.value = true;
};

const closeReportModal = () => {
    showReportModal.value = false;
    selectedReportReason.value = "SPAM";
    otherReportReason.value = "";
};

const submitReport = async () => {
    if (isOwnStory.value) {
        toast.error("Je kunt je eigen verhaal niet rapporteren.");
        return;
    }
    if (!hasToken.value) {
        openAuthModal();
        return;
    }
    const id = storyId.value;
    if (!id) {
        toast.error("Kon verhaal-id niet bepalen.");
        return;
    }

    if (selectedReportReason.value === "OTHER" && !otherReportReason.value.trim()) {
        toast.error("Beschrijf kort waarom je dit verhaal wilt rapporteren (5–250 tekens).");
        return;
    }

    // Enforce minimum character length for 'OTHER' reason (no maximum enforced client-side)
    if (selectedReportReason.value === "OTHER") {
        const len = otherReportReason.value.trim().length;
        if (len < OTHER_REASON_MIN) {
            toast.error(
                `Beschrijf kort waarom je dit verhaal wilt rapporteren (minimaal ${OTHER_REASON_MIN} tekens).`,
            );
            return;
        }
        if (len > OTHER_REASON_MAX) {
            toast.error(`De omschrijving mag maximaal ${OTHER_REASON_MAX} tekens bevatten.`);
            return;
        }
    }

    submittingReport.value = true;
    try {
        await ticketStore.fileStoryTicket(id, selectedReportReason.value, otherReportReason.value.trim());
        toast.success("Bedankt, je melding is ontvangen.");
        closeReportModal();
    } catch (err: unknown) {
        const message = err instanceof Error ? err.message : "Je melding kon niet worden verzonden.";
        toast.error(message);
    } finally {
        submittingReport.value = false;
    }
};

const handleToggleLike = async () => {
    if (!hasToken.value) {
        openAuthModal();
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
    const url = window.location.href;
    try {
        await navigator.clipboard.writeText(url);
        toast.success("De link van dit verhaal is gekopieerd naar je klembord");
    } catch {
        toast.error("Kon link niet kopiëren.");
    }
};

onMounted(async () => {
    const id = storyId.value;
    if (!id) {
        storyStore.error = "Geen verhaal-id gevonden in de URL.";
        return;
    }

    await storyStore.fetchStory(id);
});

const sanitizedStory = computed(() => {
    if (!story.value || !story.value.content) return "";
    return sanitizeHtml(story.value.content);
});

watch(
    () => [storyLoaded.value, isOwnStory.value] as const,
    ([loaded, own]) => {
        if (loaded && own && route.name === "story-detail") {
            router.replace({ name: "my-story" });
        }
    },
);

watch(
    () => selectedReportReason.value,
    (reason) => {
        if (reason !== "OTHER") {
            otherReportReason.value = "";
        }
    },
);
</script>

<template>
    <div class="max-w-4xl mx-auto px-5 py-10">
        <div
            v-if="showAuthModal"
            class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
            role="dialog"
            aria-modal="true"
        >
            <div class="w-full max-w-md rounded-2xl bg-white p-6 shadow-xl">
                <h2 class="text-xl font-semibold text-gray-900 mb-2">Je moet ingelogd zijn om dit te doen</h2>
                <p class="text-gray-600 mb-6">
                    Log in of registreer om verhalen te kunnen liken en jouw ondersteuning te tonen.
                </p>
                <div class="flex flex-col gap-3">
                    <button
                        class="w-full min-w-[120px] rounded-lg bg-brand-purple text-white py-2 font-semibold hover:bg-brand-purple/90 transition-colors"
                        @click="goToLogin"
                    >
                        Inloggen
                    </button>
                    <button
                        class="w-full min-w-[120px] rounded-lg border border-brand-purple text-brand-purple py-2 font-semibold hover:bg-[#F5F0FF] transition-colors"
                        @click="goToRegister"
                    >
                        Registreren
                    </button>
                    <button
                        class="w-full min-w-[120px] rounded-lg bg-[#FF383C] text-white py-2 font-semibold hover:bg-[#e32f33] transition-colors"
                        @click="closeAuthModal"
                    >
                        Sluiten
                    </button>
                </div>
            </div>
        </div>

        <div class="flex items-center justify-between gap-4 mb-3">
            <a class="text-sm text-gray-700" href="#" @click.prevent="handleBack">&lt; Terug</a>
            <button
                v-if="!isOwnStory"
                type="button"
                class="inline-flex items-center gap-2 text-sm font-normal text-gray-700 hover:underline"
                @click="openReportModal"
                id="RapporteerVerhaalButton"
            >
                <img :src="AlertSquare" alt="" class="w-4 h-4" />
                Rapporteer verhaal
            </button>
        </div>

        <h1 id="story-title" class="text-3xl md:text-5xl font-extrabold text-gray-900 mb-6 wrap-break-word">
            {{ story?.title ?? "Geen titel gevonden" }}
        </h1>

        <div
            v-if="showReportModal"
            class="fixed inset-0 z-50 flex items-center justify-center bg-black/40 px-4"
            @click.self="closeReportModal"
            role="dialog"
            aria-modal="true"
        >
            <div class="w-full max-w-lg rounded-2xl bg-white p-6 shadow-xl">
                <div class="flex items-start justify-between gap-4 mb-6">
                    <h2 class="text-2xl font-bold text-gray-900">Rapporteer dit verhaal</h2>
                    <button
                        type="button"
                        class="text-gray-500 hover:text-gray-800"
                        aria-label="Sluit meldingsvenster"
                        @click="closeReportModal"
                    >
                        ✕
                    </button>
                </div>

                <div class="mb-5">
                    <label class="block text-base font-semibold text-gray-900 mb-2" for="report-reason">
                        Selecteer een reden
                    </label>
                    <select
                        id="report-reason"
                        v-model="selectedReportReason"
                        class="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm text-gray-800 focus:border-brand-purple focus:ring-brand-purple"
                    >
                        <option v-for="option in reportReasons" :key="option.value" :value="option.value">
                            {{ option.label }}
                        </option>
                    </select>
                </div>

                <div v-if="selectedReportReason === 'OTHER'" class="mb-5">
                    <label class="block text-base font-semibold text-gray-900 mb-2" for="other-reason">
                        Vul je reden in
                    </label>
                    <input
                        id="other-reason"
                        v-model="otherReportReason"
                        type="text"
                        class="w-full rounded-lg border border-gray-300 px-3 py-2.5 text-sm text-gray-800 focus:border-brand-purple focus:ring-brand-purple"
                        placeholder="Reden..."
                    />
                </div>

                <div class="flex flex-col sm:flex-row justify-end sm:justify-between gap-3">
                    <button
                        type="button"
                        class="w-full sm:w-1/2 rounded-lg bg-[#1FD2E7] px-4 py-3 text-sm font-semibold text-white shadow hover:opacity-90 disabled:opacity-60"
                        :disabled="submittingReport"
                        @click="submitReport"
                        id="Rapporteer"
                    >
                        {{ submittingReport ? "Versturen..." : "Rapporteer" }}
                    </button>
                    <button
                        type="button"
                        class="w-full sm:w-1/2 rounded-lg bg-[#7451F5] px-4 py-3 text-sm font-semibold text-white shadow hover:opacity-90"
                        @click="closeReportModal"
                        id="AnnuleerButton"
                    >
                        Annuleer
                    </button>
                </div>
            </div>
        </div>

        <section
            class="flex flex-col md:flex-row items-start md:items-center justify-between gap-4 md:gap-0 border-2"
            :class="['border-[#7C4DFF]', 'rounded-xl', 'p-5', 'bg-white', 'mb-7']"
        >
            <div class="flex items-center">
                <IconUser class="w-11 h-11" />
                <div class="ml-4">
                    <div class="font-medium text-gray-800 flex items-center flex-wrap gap-2">
                        <span id="story-author">{{ story?.ownerUsername ?? "Geen gebruiker gevonden" }}</span>
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
                                :key="`detail-condition-${conditionName}-${conditionIndex}`"
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

            <div class="flex w-full md:w-auto flex-col md:items-end gap-3 text-base font-medium text-gray-700">
                <div class="flex w-full flex-wrap items-center justify-start md:justify-end gap-3">
                    <Button3D
                        v-if="canInviteVisible"
                        variant="primary"
                        class="gap-2 text-sm font-semibold h-12 px-5"
                        :disabled="inviteLoading || !canInviteEnabled"
                        :class="inviteLoading || !canInviteEnabled ? 'opacity-60 cursor-not-allowed' : ''"
                        :title="!canInviteEnabled ? 'Kan geen uitnodiging sturen: ontvanger-id ontbreekt' : ''"
                        @click="sendInvite"
                    >
                        <svg
                            class="w-4 h-4"
                            viewBox="0 0 24 24"
                            fill="none"
                            xmlns="http://www.w3.org/2000/svg"
                            aria-hidden="true"
                        >
                            <path
                                d="M12 21v-2a4 4 0 0 0-4-4H5"
                                stroke="white"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            />
                            <circle cx="10" cy="8" r="3" stroke="white" stroke-width="1.8" />
                            <path
                                d="M17 8v6m-3-3h6"
                                stroke="white"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                            />
                        </svg>
                        Maak connectie
                    </Button3D>
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
                        <span class="text-lg font-semibold text-gray-900">{{ likeCount }}</span>
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
                </div>
            </div>
        </section>

        <article class="text-gray-800 leading-relaxed text-base space-y-4 mb-8">
            <div v-if="loading" class="text-gray-600">Bezig met laden...</div>
            <div v-else-if="error" class="text-red-600">{{ error }}</div>
            <div v-else>
                <div
                    id="story-content"
                    class="whitespace-pre-line wrap-break-word story-content"
                    v-html="sanitizedStory"
                ></div>
            </div>
        </article>

        <aside
            v-if="!hasToken"
            class="rounded-2xl p-6 mb-8 bg-[#D9D2FF] shadow-sm flex flex-col gap-3 text-gray-900"
        >
            <h3 class="text-xl font-semibold">Herken je dit verhaal?</h3>
            <p class="text-gray-700">
                Deel jouw eigen ervaring en help anderen met vergelijkbare uitdagingen.
            </p>
            <button
                class="self-start rounded-xl bg-[#27DAFF] px-5 py-2 text-white font-semibold shadow hover:bg-[#15c3e8] transition-colors"
                @click="handleSubmit"
            >
                Word nu lid
            </button>
        </aside>
    </div>
</template>

<style>
@import "../assets/story_style.css";
</style>
