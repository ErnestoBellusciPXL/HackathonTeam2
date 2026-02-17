<template>
    <div class="mb-5">
        <div class="max-w-3xl mx-auto mt-10 px-4">
            <h1 class="text-2xl md:text-3xl font-bold mb-1.5">Deel jouw verhaal</h1>
            <p class="text-gray-600 mb-4">Jouw ervaring kan anderen helpen.</p>

            <form class="rounded-lg border-2 border-purple-600 p-5 bg-white" @submit.prevent="onSubmit">
                <h2 class="text-lg font-semibold mb-3">Vertel jouw verhaal</h2>

                <div class="field">
                    <p class="font-bold">Aandoening</p>
                    <span v-if="userConditions.length > 1" class="text-xs text-gray-600"
                        >Selecteer de aandoening(en) waarover jouw verhaal gaat.</span
                    >

                    <!-- Selected chips box (fixed min size, fixed width) -->
                    <div class="mt-3 border border-gray-300 rounded-[10px] p-3 min-h-32 w-full sm:w-[560px]">
                        <div class="text-xs text-gray-700 mb-2">
                            <span v-if="userConditions.length === 1">
                                Automatisch geselecteerd op basis van de aandoening gelinkt aan uw account:
                            </span>
                            <span v-else> Geselecteerde aandoening(en): </span>
                        </div>
                        <div class="flex flex-wrap gap-2 min-h-6">
                            <span
                                v-for="name in selectedConditions"
                                :key="name"
                                class="inline-flex items-center bg-brand-purple text-white px-3 py-1 rounded-[10px] text-sm"
                            >
                                {{ name }}
                                <button
                                    v-if="!(userConditions.length === 1)"
                                    type="button"
                                    class="ml-2 w-5 h-5 inline-flex items-center justify-center bg-white/20 rounded-[10px] hover:bg-white/30"
                                    aria-label="Verwijder"
                                    @click.stop="removeCondition(name)"
                                >
                                    ×
                                </button>
                            </span>
                            <span v-if="!selectedConditions.length" class="text-gray-500 text-sm"
                                >Nog geen aandoeningen gekozen.</span
                            >
                        </div>
                    </div>

                    <!-- Suggestions chips box: only show if user has more than one condition -->
                    <div
                        v-if="userConditions.length > 1"
                        class="mt-2 border border-gray-300 rounded-[10px] p-3 h-48 w-full sm:w-[560px] overflow-y-scroll scroll-stable"
                    >
                        <div class="flex flex-wrap gap-2">
                            <button
                                v-for="name in filteredConditions"
                                :key="name"
                                type="button"
                                class="px-3 py-1 rounded-[10px] text-sm border border-brand-purple text-brand-purple bg-brand-purple/10 hover:bg-brand-purple/20"
                                @click="addCondition(name)"
                            >
                                {{ name }}
                            </button>
                            <span v-if="filteredConditions.length === 0" class="text-gray-500 text-sm"
                                >Geen resultaten</span
                            >
                        </div>
                    </div>
                </div>

                <div class="mt-3 mb-3">
                    <p class="font-bold">Enkele vragen om je op weg te helpen:</p>
                    <ul class="ms-3">
                        <li>Wat waren je eerste symptomen?</li>
                        <li>Hoe verliep het diagnoseproces?</li>
                        <li>Welke uitdagingen kwam je tegen in de zorg?</li>
                        <li>Wat had je anders gewild?</li>
                        <li>Welk advies zou je anderen geven?</li>
                    </ul>
                </div>

                <label class="block mb-3.5">
                    <div class="flex flex-row items-end justify-between mb-3">
                        <span class="font-semibold mb-1.5 h-min">Jouw verhaal</span>
                        <Button3D
                            v-if="!aiIsLoading"
                            asTag="button"
                            variant="secondary"
                            size="sm"
                            class=""
                            @click.prevent="getAIStoryImprovement"
                        >
                            Verbeter Opmaak (AI)
                            <IconSparkle class="w-5 h-5 ml-2 text-white" />
                        </Button3D>
                    </div>
                    <div class="relative" v-if="!aiReformattedContent">
                        <textarea
                            v-model="form.content"
                            rows="10"
                            placeholder="Begin met je verhaal te vertellen ..."
                            id="StoryInput"
                            class="w-full p-2.5 rounded-md border border-gray-200 placeholder-gray-400"
                            :class="{ 'blur-sm transition-all duration-300': aiIsLoading }"
                        ></textarea>

                        <!-- AI Loading Overlay -->
                        <div
                            v-if="aiIsLoading"
                            class="absolute inset-0 flex flex-col items-center justify-center bg-white/80 transition-opacity duration-300 rounded-md"
                        >
                            <IconSparkle class="w-12 h-12 text-brand-purple pulse-scale mb-3" />
                            <p class="text-brand-purple font-semibold">AI Opmaak Bezig...</p>
                        </div>
                    </div>
                </label>

                <!-- AI Improved Content Display -->
                <div
                    v-if="aiReformattedContent && !aiIsLoading"
                    class="mb-3.5 p-4 border-2 border-brand-purple rounded-md bg-purple-50"
                >
                    <p class="font-semibold mb-2 text-brand-purple">
                        Verbeterd Verhaal (AI-assistent toegepast):
                    </p>
                    <div
                        class="whitespace-pre-line bg-white p-3 rounded-md"
                        v-html="aiReformattedContent"
                        id="improvedVersion"
                    ></div>
                    <Button3D
                        asTag="button"
                        variant="danger"
                        size="sm"
                        class="mt-3"
                        @click.prevent="undoAIImprovement"
                    >
                        AI Opmaak Ongedaan Maken
                    </Button3D>
                </div>

                <label class="block mb-3.5">
                    <span class="font-semibold mb-1.5 block">Titel van jouw verhaal</span>
                    <input
                        v-model="form.title"
                        type="text"
                        placeholder="bijvoorbeeld: Weg naar mijn diagnose"
                        class="w-full p-2.5 rounded-md border border-gray-200 placeholder-gray-400"
                        id="TitleInput"
                    />
                    <div class="flex items-center justify-between mt-1.5">
                        <small class="text-gray-500 text-sm">Kies een korte, herkenbare titel</small>
                        <small class="text-sm" :class="titleTooLong ? 'text-red-600' : 'text-gray-500'">
                            {{ titleRemaining }} tekens over
                        </small>
                    </div>
                </label>

                <div class="grid grid-cols-1 sm:grid-cols-2 gap-3 mt-3 w-full">
                    <Button3D :type="'submit'" variant="primary" class="w-full" id="SubmitStoryButton">
                        {{ isUserEditting ? "Bewerken" : "Publiceer verhaal" }}
                    </Button3D>
                    <Button3D
                        asTag="button"
                        variant="secondary"
                        class="w-full"
                        id="CancelStoryButton"
                        @click.prevent="onCancel"
                    >
                        Annuleren
                    </Button3D>
                </div>

                <p class="mt-3 text-gray-600 text-sm">
                    Door je verhaal te publiceren ga je akkoord met onze voorwaarden. Je verhaal wordt anoniem
                    gepubliceerd onder jouw gebruikersnaam.
                </p>

                <Button3D
                    v-if="hasExistingStory"
                    asTag="button"
                    variant="danger"
                    class="w-full mt-3"
                    :disabled="deletingStory"
                    @click.prevent="showDeleteConfirm = true"
                    id="VerwijderVerhaal"
                >
                    {{ deletingStory ? "Verhaal verwijderen..." : "Verwijder verhaal" }}
                </Button3D>
            </form>
        </div>
    </div>

    <div
        v-if="showDeleteConfirm"
        class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4"
        role="dialog"
        aria-modal="true"
    >
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6 space-y-4">
            <h2 class="text-xl font-semibold text-gray-900">Verhaal verwijderen?</h2>
            <p class="text-gray-700">
                Weet je zeker dat je je verhaal wilt verwijderen? Dit kan niet ongedaan worden gemaakt.
            </p>
            <div class="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <Button3D
                    asTag="button"
                    variant="secondary"
                    class="w-full"
                    @click.prevent="showDeleteConfirm = false"
                >
                    Annuleren
                </Button3D>
                <Button3D
                    asTag="button"
                    variant="danger"
                    class="w-full"
                    :disabled="deletingStory"
                    @click.prevent="confirmDeleteStory"
                    id="BevestigVerwijderenVerhaal"
                >
                    {{ deletingStory ? "Verwijderen..." : "Ja, verwijderen" }}
                </Button3D>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import Button3D from "@/components/Button3D.vue";
import { useToastStore } from "@/stores/toast";
import { useStoryStore } from "@/stores/story";
import { useConditionsStore } from "@/stores/conditions";
import type { Condition } from "@/utils/Condition";
import type { StoryPayload } from "@/utils/Story";
import IconSparkle from "@/components/icons/IconSparkle.vue";
import { removeAllHtmlTags, sanitizeHtml } from "@/utils/HTMLSanitizer";
import { useStoryDetailStore } from "@/stores/storyDetail";

const router = useRouter();
const toast = useToastStore();

const form = ref({
    title: "",
    content: "",
});

// Title length limits and helpers
const TITLE_MAX = 100;
const TITLE_MIN = 5;
// use trimmed length for validation (ignore leading/trailing spaces)
const titleLength = computed(() => form.value.title?.trim().length || 0);
const titleRemaining = computed(() => Math.max(0, TITLE_MAX - titleLength.value));
const titleTooLong = computed(() => titleLength.value > TITLE_MAX);
// removed inline short-title error rendering; validation is handled via toasts on submit
const aiIsLoading = ref(false);
const isUserEditting = ref(false);
const oldStory = ref<string>("");
const aiReformattedContent = ref<string>("");
const deletingStory = ref(false);
const showDeleteConfirm = ref(false);
const hasExistingStory = computed(() => isUserEditting.value && Boolean(storyDetailStore.story?.id));

// conditions (fetched from backend)
const conditionsStore = useConditionsStore();
const userConditions = ref<Condition[]>([]);
const selectedConditions = ref<string[]>([]);
const storyStore = useStoryStore();
const storyDetailStore = useStoryDetailStore();

// Only show conditions linked to the logged-in user
const allUserConditionNames = computed(() => userConditions.value.map((c) => c.name));

// Suggestions chips: only user-linked conditions not already selected
const filteredConditions = computed(() => {
    return allUserConditionNames.value.filter((n) => !selectedConditions.value.includes(n));
});

// when dropdown selection changes, add the condition and clear selection
// (no dropdown anymore)

// Ensure that when the user's conditions are fetched and there's exactly one,
// it gets selected immediately. This prevents timing races in tests or fast
// navigation flows where `onMounted` fetch resolves after the user submits.
watch(userConditions, (newVal) => {
    if (Array.isArray(newVal) && newVal.length === 1 && !selectedConditions.value.length) {
        const c = newVal[0];
        if (c && c.name) selectedConditions.value = [c.name];
    }
});

onMounted(async () => {
    // Fetch only the conditions linked to the current user (requires auth token).
    const res = await conditionsStore.fetchForCurrentUser();
    if (Array.isArray(res)) {
        userConditions.value = res;
    } else {
        // non-blocking: log error

        console.error("Failed to load user conditions:", res);
    }

    // If the current account only has one condition, pre-select it
    if (userConditions.value.length === 1) {
        const c = userConditions.value[0];
        if (c && c.name) selectedConditions.value = [c.name];
    }

    // Fetch the currently authenticated user's story.
    await storyDetailStore.fetchMyStory();

    if (!storyDetailStore.story) {
        return;
    }

    isUserEditting.value = true;

    storyDetailStore.story!.content = removeAllHtmlTags(
        storyDetailStore.story!.content.replace("<script>", "").replace("</scr" + "ipt>", ""),
    );

    form.value.content = storyDetailStore.story!.content;
    form.value.title = storyDetailStore.story!.title || "";
});

function addCondition(name: string) {
    if (!selectedConditions.value.includes(name)) selectedConditions.value.push(name);
}

function removeCondition(name: string) {
    selectedConditions.value = selectedConditions.value.filter((n) => n !== name);
}

function onCancel() {
    router.back();
}

async function onSubmit() {
    // basic validation
    if (!form.value.title.trim()) {
        toast.error("Vul een titel in.");
        return;
    }
    // Title: allow only letters, digits and spaces (assumption: spaces allowed between words)
    const titleRegex = /^[\p{L}\p{N} ]+$/u;
    if (!titleRegex.test(form.value.title.trim())) {
        toast.error("Titel mag alleen letters, cijfers en spaties bevatten.");
        return;
    }
    // enforce minimum title length
    if (form.value.title.trim().length < TITLE_MIN) {
        toast.error(`Titel moet minimaal ${TITLE_MIN} tekens bevatten.`);
        return;
    }
    // enforce maximum title length
    if (form.value.title.trim().length > TITLE_MAX) {
        toast.error(`Titel mag maximaal ${TITLE_MAX} tekens bevatten.`);
        return;
    }
    if (!form.value.content.trim()) {
        toast.error("Vul je verhaal in.");
        return;
    }
    if (!selectedConditions.value.length) {
        toast.error("Kies of vul minstens één aandoening/diagnose in.");
        return;
    }

    const payload = {
        title: form.value.title.trim(),
        content:
            aiReformattedContent.value && aiReformattedContent.value.trim().length > 1
                ? aiReformattedContent.value.trim()
                : form.value.content.trim(),
        conditionNames: selectedConditions.value.map((s) => s.trim()).filter((s) => s.length > 0),
    } as StoryPayload;

    let result;
    let msg = "Je verhaal is gepubliceerd.";
    let errorMsg = "Publiceren mislukt.";

    if (isUserEditting.value && storyDetailStore.story && storyDetailStore.story.id) {
        payload.id = storyDetailStore.story.id;
        result = await storyStore.updateStory(payload);
        msg = "Je verhaal is gewijzigd.";
        errorMsg = "Wijziging mislukt.";
    } else {
        result = await storyStore.publishStory(payload);
    }

    if (result.success) {
        toast.success(msg);
        router.push({ name: "home" });
    } else {
        toast.error(result.error || errorMsg);
    }
}

async function getAIStoryImprovement() {
    if (!form.value.content.trim()) {
        toast.error("Vul eerst je verhaal in voordat je het verbetert.");
        return;
    }

    const sanitizedStory = sanitizeHtml(form.value.content.trim());

    aiIsLoading.value = true;
    oldStory.value = sanitizedStory;

    const response = await storyStore.getAIImprovedStory(sanitizedStory);

    if (response.state === "success" && response.response) {
        aiReformattedContent.value = response.response.reformattedStoryContent;
        toast.success("Je verhaal is verbeterd met AI!");
    } else if (response.state === "failed") {
        toast.error(response.errorResponse?.error || "AI verbetering mislukt.");
    } else {
        toast.error("Er was een fout...");
    }

    aiIsLoading.value = false;
}

function undoAIImprovement() {
    form.value.content = oldStory.value;
    oldStory.value = "";
    aiReformattedContent.value = "";
    toast.info("AI verbetering ongedaan gemaakt.");
}

async function onDeleteStory() {
    if (!hasExistingStory.value) return;

    const storyId = storyDetailStore.story?.id;
    if (!storyId) {
        toast.error("Geen verhaal gevonden om te verwijderen.");
        return;
    }

    deletingStory.value = true;
    try {
        const result = await storyStore.deleteStory(storyId);
        if (result.success) {
            toast.success("Je verhaal is verwijderd.");
            storyDetailStore.story = null;
            isUserEditting.value = false;
            form.value = { title: "", content: "" };
            selectedConditions.value = [];
            aiReformattedContent.value = "";
            router.push({ name: "home" });
        } else {
            toast.error(result.error || "Verhaal kon niet verwijderd worden.");
        }
    } catch (error) {
        toast.error(error instanceof Error ? error.message : "Verhaal kon niet verwijderd worden.");
    } finally {
        deletingStory.value = false;
    }
}

const confirmDeleteStory = async () => {
    showDeleteConfirm.value = false;
    await onDeleteStory();
};
</script>

<style>
@import "../assets/story_style.css";

@keyframes pulse-scale {
    0%,
    100% {
        transform: scale(1);
    }

    50% {
        transform: scale(1.2);
    }
}

.pulse-scale {
    animation: pulse-scale 1.5s ease-in-out infinite;
}
</style>
