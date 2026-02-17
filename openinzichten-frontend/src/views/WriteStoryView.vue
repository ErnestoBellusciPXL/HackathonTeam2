<template>
    <div class="mb-5">
        <div class="max-w-3xl mx-auto mt-10 px-4">
            <h1 class="text-2xl md:text-3xl font-bold mb-1.5">Deel jouw verhaal</h1>
            <p class="text-gray-600 mb-4">Jouw ervaring kan anderen helpen.</p>

            <form class="rounded-lg border-2 border-purple-600 p-5 bg-white" @submit.prevent>

                <!-- STEP 0: MODE SELECTION -->
                <div v-if="currentStep === 0" class="py-6">
                    <h2 class="text-xl font-bold mb-6 text-center text-brand-purple">Hoe wil je je verhaal maken?</h2>
                    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
                        <div
                            class="p-6 border-2 border-gray-200 rounded-xl hover:border-brand-purple hover:bg-brand-purple/5 transition-all cursor-pointer flex flex-col items-center text-center group"
                            @click="selectMode('interview')"
                        >
                            <div class="w-16 h-16 bg-brand-purple/10 rounded-full flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                                <IconSparkle class="w-8 h-8 text-brand-purple" />
                            </div>
                            <h3 class="text-lg font-bold mb-2">Interview-modus</h3>
                            <p class="text-gray-600 text-sm">De AI stelt vragen en we bouwen samen stap voor stap jouw verhaal.</p>
                            <Button3D variant="primary" class="mt-6 w-full" @click.stop="selectMode('interview')">Start Interview</Button3D>
                        </div>

                        <div
                            class="p-6 border-2 border-gray-200 rounded-xl hover:border-brand-purple hover:bg-brand-purple/5 transition-all cursor-pointer flex flex-col items-center text-center group"
                            @click="selectMode('self-write')"
                        >
                            <div class="w-16 h-16 bg-gray-100 rounded-full flex items-center justify-center mb-4 group-hover:scale-110 transition-transform">
                                <svg class="w-8 h-8 text-gray-500" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M11 5H6a2 2 0 00-2 2v11a2 2 0 002 2h11a2 2 0 002-2v-5m-1.414-9.414a2 2 0 112.828 2.828L11.828 15H9v-2.828l8.586-8.586z"></path></svg>
                            </div>
                            <h3 class="text-lg font-bold mb-2">Zelfschrijf-modus</h3>
                            <p class="text-gray-600 text-sm">Schrijf zelf je verhaal. Achteraf volgt een controle op taal en richtlijnen.</p>
                            <Button3D variant="secondary" class="mt-6 w-full" @click.stop="selectMode('self-write')">Zelf schrijven</Button3D>
                        </div>
                    </div>
                </div>

                <!-- STEP 1: WIZARD QUESTIONS (Interview Mode) -->
                <div v-if="currentStep === 1">
                    <div class="mb-4">
                        <div class="flex justify-between items-center mb-2">
                             <h2 class="text-lg font-semibold">Interview-modus: Beantwoord vragen</h2>
                             <span class="text-sm font-bold text-brand-purple">{{ currentQuestionIndex + 1 }} / {{ questions.length }}</span>
                        </div>
                        <div class="w-full bg-gray-200 rounded-full h-2.5">
                            <div class="bg-brand-purple h-2.5 rounded-full transition-all duration-300" :style="{ width: ((currentQuestionIndex + 1) / questions.length) * 100 + '%' }"></div>
                        </div>
                    </div>

                    <div class="mb-6 min-h-[200px]">
                        <label class="block mb-2 font-bold text-lg">{{ currentQuestion }}</label>
                        <p class="text-gray-500 text-sm mb-3">{{ currentQuestionHelp }}</p>
                        <textarea
                            v-model="currentAnswer"
                            rows="6"
                            class="w-full p-3 rounded-md border border-gray-300 focus:border-brand-purple focus:ring-1 focus:ring-brand-purple"
                            placeholder="Typ hier je antwoord..."
                            ref="answerInput"
                        ></textarea>
                    </div>

                    <div class="flex flex-col sm:flex-row justify-between gap-3 mt-4">
                        <Button3D
                            variant="secondary"
                            @click="prevQuestion"
                            :disabled="currentQuestionIndex === 0"
                            :class="{ 'opacity-50 cursor-not-allowed': currentQuestionIndex === 0 }"
                            class="w-full sm:w-auto"
                        >
                            Vorige
                        </Button3D>

                        <Button3D
                            v-if="currentQuestionIndex < questions.length - 1"
                            variant="primary"
                            @click="nextQuestion"
                            :disabled="!currentAnswer.trim()"
                            class="w-full sm:w-auto"
                        >
                            Volgende
                        </Button3D>
                        <Button3D
                            v-else
                            variant="primary"
                            @click="handleGenerateStory"
                            :disabled="!currentAnswer.trim()"
                            class="w-full sm:w-auto"
                        >
                            Genereer Verhaal <IconSparkle class="w-5 h-5 ml-2 inline" />
                        </Button3D>
                    </div>
                </div>

                <!-- STEP 2: LOADING GENERATION -->
                <div v-else-if="currentStep === 2" class="flex flex-col items-center justify-center py-10 text-center">
                    <IconSparkle class="w-16 h-16 text-brand-purple pulse-scale mb-4" />
                    <h2 class="text-xl font-bold text-brand-purple mb-2">Even geduld...</h2>
                    <p class="text-gray-600">Onze AI schrijft jouw verhaal op basis van je antwoorden.</p>
                </div>

                <!-- STEP 3: REVIEW & EDIT (Self-write entry or AI result review) -->
                <div v-else-if="currentStep === 3">
                    <div class="flex justify-between items-center mb-3">
                        <h2 class="text-lg font-semibold">{{ creationMode === 'interview' ? 'Stap 2: Bekijk en bewerk je verhaal' : 'Zelfschrijf-modus: Schrijf je verhaal' }}</h2>
                        <button v-if="!storyDetailStore.story?.id" @click="currentStep = 0" class="text-brand-purple text-sm font-semibold hover:underline">Verander modus</button>
                    </div>
                    <p class="text-gray-600 text-sm mb-4">
                        {{ creationMode === 'interview' ? 'Dit is een concept op basis van je interview. Je kan het nog volledig aanpassen.' : 'Voer hieronder je volledige verhaal in. De AI controleert daarna alleen de spelling en richtlijnen.' }}
                    </p>

                    <div class="mb-4">
                        <label class="block font-bold mb-1">Titel</label>
                        <input
                            v-model="form.title"
                            type="text"
                            class="w-full p-2.5 rounded-md border border-gray-300"
                            placeholder="Geef je verhaal een titel"
                        />
                    </div>

                    <div class="mb-4">
                        <label class="block font-bold mb-1">Verhaal</label>
                        <textarea
                            v-model="form.content"
                            rows="15"
                            class="w-full p-2.5 rounded-md border border-gray-300"
                        ></textarea>
                    </div>

                     <!-- Conditions Selection (Integrated here or in Step 1? Keeping it here for context) -->
                    <div class="mb-6">
                        <label class="block font-bold mb-1">Over welke aandoening gaat dit?</label>
                         <div class="flex flex-wrap gap-2">
                            <span
                                v-for="name in selectedConditions"
                                :key="name"
                                class="inline-flex items-center bg-brand-purple text-white px-3 py-1 rounded-[10px] text-sm"
                            >
                                {{ name }}
                                <button
                                    type="button"
                                    v-if="userConditions.length > 1"
                                    class="ml-2"
                                    @click="removeCondition(name)"
                                >×</button>
                            </span>
                             <!-- Add dropdown if needed, reuse logic from component -->
                             <p v-if="userConditions.length > 1 && selectedConditions.length < userConditions.length" class="text-xs text-gray-500 my-1">
                                 (Selecteer hieronder om toe te voegen)
                             </p>
                        </div>
                        <div v-if="userConditions.length > 1" class="flex flex-wrap gap-2 mt-2">
                             <button
                                v-for="c in availableConditions"
                                :key="c.name"
                                class="px-3 py-1 text-sm border border-brand-purple rounded-full text-brand-purple hover:bg-brand-purple/10"
                                @click="addCondition(c.name)"
                             >
                                + {{ c.name }}
                             </button>
                        </div>
                         <p v-if="selectedConditions.length === 0" class="text-red-500 text-sm mt-1">Selecteer minstens één aandoening.</p>
                    </div>

                    <div class="flex flex-col sm:flex-row gap-3 justify-between mt-6">
                        <Button3D v-if="creationMode === 'interview'" variant="secondary" @click="currentStep = 1" class="w-full sm:w-auto">Terug naar vragen</Button3D>
                        <Button3D v-else variant="secondary" @click="currentStep = 0" class="w-full sm:w-auto">Terug naar keuze</Button3D>

                        <div class="flex flex-col sm:flex-row gap-3 w-full sm:w-auto">
                             <!-- Add Delete Button if editing -->
                            <Button3D
                                v-if="storyDetailStore.story?.id"
                                variant="danger"
                                @click="showDeleteConfirm = true"
                                class="w-full sm:w-auto"
                            >
                                Verwijder Verhaal
                            </Button3D>

                            <Button3D variant="primary" @click="handleValidateStory" class="w-full sm:w-auto">
                                Controleer <IconSparkle v-if="creationMode === 'self-write'" class="w-4 h-4 ml-1 inline" />
                            </Button3D>
                        </div>
                    </div>

                    <!-- Accordion for Q&A Reference (Only for Interview Mode) -->
                    <div v-if="creationMode === 'interview'" class="mt-8 border-t pt-4">
                        <button @click="showAnswers = !showAnswers" class="text-brand-purple font-semibold flex items-center">
                            <span>{{ showAnswers ? 'Verberg' : 'Toon' }} mijn antwoorden</span>
                            <svg class="w-4 h-4 ml-1 transform transition-transform" :class="{ 'rotate-180': showAnswers }" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7"></path></svg>
                        </button>
                        <div v-if="showAnswers" class="mt-3 space-y-3 p-3 bg-gray-50 rounded-md">
                            <div v-for="(qa, idx) in answers" :key="idx">
                                <p class="font-bold text-xs uppercase text-gray-500">{{ qa.question }}</p>
                                <p class="text-sm text-gray-700">{{ qa.answer }}</p>
                            </div>
                        </div>
                    </div>
                </div>

                <!-- STEP 4: LOADING VALIDATION -->
                 <div v-else-if="currentStep === 4" class="flex flex-col items-center justify-center py-10 text-center">
                    <IconSparkle class="w-16 h-16 text-brand-purple pulse-scale mb-4" />
                    <h2 class="text-xl font-bold text-brand-purple mb-2">Controleren...</h2>
                    <p class="text-gray-600">Onze AI kijkt je verhaal na op spelling en richtlijnen.</p>
                </div>

                <!-- STEP 5: FINAL APPROVAL -->
                <div v-else-if="currentStep === 5">
                    <h2 class="text-lg font-semibold mb-3">{{ isRejected ? 'Verhaal afgewezen' : 'Stap 3: Klaar voor publicatie' }}</h2>

                    <!-- REJECTION STATE -->
                    <div v-if="isRejected" class="mb-6">
                        <div class="p-5 bg-red-50 border-2 border-red-200 rounded-xl text-red-800">
                            <div class="flex items-center mb-3">
                                <svg class="w-6 h-6 mr-2" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z"></path></svg>
                                <h3 class="font-bold text-lg">Inhoud voldoet niet aan de richtlijnen</h3>
                            </div>
                            <p class="mb-4">Je verhaal bevat inhoud die niet is toegestaan op dit platform (zoals vulgair taalgebruik, beledigingen of ongepaste details). Pas je verhaal aan om verder te kunnen.</p>

                            <div v-if="validationResult?.changes && validationResult.changes.length > 0" class="mt-4 pt-4 border-t border-red-100">
                                <p class="font-bold text-sm uppercase mb-2">Reden van afwijzing:</p>
                                <ul class="list-disc list-inside text-sm space-y-1">
                                    <li v-for="(change, idx) in validationResult.changes" :key="idx">
                                        {{ change.description }}
                                    </li>
                                </ul>
                            </div>
                        </div>

                        <div class="mt-6">
                            <Button3D variant="secondary" @click="currentStep = 1" class="w-full">Terug naar begin</Button3D>
                        </div>
                    </div>

                    <!-- SUCCESS STATE -->
                    <template v-else>
                        <div v-if="validationResult?.changes && validationResult.changes.length > 0" class="mb-4 p-4 bg-blue-50 border border-blue-200 rounded-md">
                            <h3 class="font-bold text-blue-800 mb-2">Wijzigingen:</h3>
                            <ul class="list-disc list-inside text-sm text-blue-800 space-y-1">
                                <li v-for="(change, idx) in validationResult.changes" :key="idx">
                                    <span class="font-semibold">{{ change.reason }}:</span> {{ change.description }}
                                </li>
                            </ul>
                        </div>
                        <div v-else class="mb-4 p-4 bg-green-50 border border-green-200 rounded-md text-green-800">
                            Geen inhoudelijke wijzigingen nodig gevonden. Je verhaal ziet er goed uit!
                        </div>

                        <div class="mb-5 border border-gray-200 rounded-md p-4 bg-gray-50">
                            <h3 class="font-bold text-lg mb-2">{{ validationResult?.fixedTitle || form.title }}</h3>
                             <div class="whitespace-pre-line text-gray-800">{{ validationResult?.fixedContent || form.content }}</div>
                        </div>

                        <div class="flex flex-col sm:flex-row justify-between gap-3 mt-4">
                            <Button3D variant="secondary" @click="currentStep = 3" class="w-full sm:w-auto">Terug naar aanpassen</Button3D>
                            <Button3D variant="primary" @click="confirmPublish" class="w-full sm:w-auto">
                                Bevestig Publicatie
                            </Button3D>
                        </div>
                    </template>
                </div>

            </form>
        </div>
    </div>

    <!-- Delete Confirmation Modal -->
    <div v-if="showDeleteConfirm" class="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-4">
        <div class="bg-white rounded-lg shadow-xl max-w-md w-full p-6 space-y-4">
            <h2 class="text-xl font-semibold text-gray-900">Verhaal verwijderen?</h2>
            <p class="text-gray-700">Weet je zeker dat je je verhaal wilt verwijderen? Dit kan niet ongedaan worden gemaakt.</p>
            <div class="grid grid-cols-2 gap-3">
                <Button3D variant="secondary" @click="showDeleteConfirm = false">Annuleren</Button3D>
                <Button3D variant="danger" :disabled="deletingStory" @click="onDeleteStory">
                    {{ deletingStory ? "Verwijderen..." : "Ja, verwijderen" }}
                </Button3D>
            </div>
        </div>
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch, nextTick } from "vue";
import { useRouter } from "vue-router";
import Button3D from "@/components/Button3D.vue";
import { useToastStore } from "@/stores/toast";
import { useStoryStore } from "@/stores/story";
import { useConditionsStore } from "@/stores/conditions";
import type { Condition } from "@/utils/Condition";
import type { StoryPayload, QuestionAnswer, StoryValidationResponse } from "@/utils/Story";
import IconSparkle from "@/components/icons/IconSparkle.vue";
import { useStoryDetailStore } from "@/stores/storyDetail";

const router = useRouter();
const toast = useToastStore();
const storyStore = useStoryStore();
const conditionsStore = useConditionsStore();
const storyDetailStore = useStoryDetailStore();

const isRejected = computed(() => {
    return currentStep.value === 5 && validationResult.value && !validationResult.value.fixedContent;
});

// --- STATE ---
const currentStep = ref(0); // 0: Selection, 1: Questions, 2: Loading Gen, 3: Review/Self-write, 4: Loading Val, 5: Final
const creationMode = ref<'interview' | 'self-write' | null>(null);
const form = ref({ title: "", content: "" });

// Step 1: Questions
const questions = [
    { q: "Wat waren je eerste symptomen?", help: "Denk terug aan het allereerste moment dat je merkte dat er iets mis was." },
    { q: "Hoe verliep het diagnoseproces?", help: "Was het een lange zoektocht? Hoe reageerden artsen?" },
    { q: "Welke uitdagingen kom je dagelijks tegen?", help: "Fysiek, mentaal, sociaal, op het werk..." },
    { q: "Wat heeft je geholpen om hiermee om te gaan?", help: "Mensen, activiteiten, behandelingen, mindset..." },
    { q: "Welk advies zou je anderen in dezelfde situatie geven?", help: "Wat had jij graag eerder geweten?" }
];
const currentQuestionIndex = ref(0);
const answers = ref<QuestionAnswer[]>(questions.map(q => ({ question: q.q, answer: "" })));

const currentQuestion = computed(() => questions[currentQuestionIndex.value]?.q || "");
const currentQuestionHelp = computed(() => questions[currentQuestionIndex.value]?.help || "");
const currentAnswer = computed({
    get: () => answers.value[currentQuestionIndex.value]?.answer || "",
    set: (val) => {
        const answerObj = answers.value[currentQuestionIndex.value];
        if (answerObj) {
            answerObj.answer = val;
        }
    }
});
const answerInput = ref<HTMLTextAreaElement | null>(null);

// Conditions
const userConditions = ref<Condition[]>([]);
const selectedConditions = ref<string[]>([]);
const availableConditions = computed(() => userConditions.value.filter(c => !selectedConditions.value.includes(c.name)));

// Step 3: Review
const showAnswers = ref(false);

// Step 5: Validation Result
const validationResult = ref<StoryValidationResponse | null>(null);

// --- LIFECYCLE ---
onMounted(async () => {
    // Fetch conditions
    const res = await conditionsStore.fetchForCurrentUser();
    if (Array.isArray(res)) {
        userConditions.value = res;
        // Auto-select if only one
        if (userConditions.value.length === 1 && userConditions.value[0]) {
            selectedConditions.value = [userConditions.value[0].name];
        }
    }

    // Check if editing existing story (Legacy support? Or just block editing old stories with wizard?)
    // For now, if editing, we might need to skip wizard or populate answers?
    // The plan implies a new creation flow. Let's assume for now this is primarily for new stories.
    // However, if the user requested "editable text area", we can support loading existing content into Step 3 immediately.
    await storyDetailStore.fetchMyStory();
    if (storyDetailStore.story) {
        // Existing story found -> Go to Step 3 directly
        form.value.title = storyDetailStore.story.title;
        form.value.content = storyDetailStore.story.content;
        selectedConditions.value = storyDetailStore.story.conditionNames;
        currentStep.value = 3;
    }
});

// --- METHODS ---

function selectMode(mode: 'interview' | 'self-write') {
    creationMode.value = mode;
    if (mode === 'interview') {
        currentStep.value = 1;
    } else {
        currentStep.value = 3;
    }
}

// Wizard Navigation
function nextQuestion() {
    if (currentQuestionIndex.value < questions.length - 1) {
        currentQuestionIndex.value++;
        nextTick(() => answerInput.value?.focus());
    }
}

function prevQuestion() {
    if (currentQuestionIndex.value > 0) {
        currentQuestionIndex.value--;
        nextTick(() => answerInput.value?.focus());
    }
}

// AI Actions
async function handleGenerateStory() {
    currentStep.value = 2; // Loading
    const result = await storyStore.generateStory(answers.value);

    if (result.state === "success") {
        form.value.content = result.response.reformattedStoryContent;
        // Generate a placeholder title or leave empty
        if (!form.value.title) form.value.title = "Mijn verhaal";
        currentStep.value = 3; // Review
    } else {
        // TS narrowing
        const errMsg = result.state === 'failed' ? result.errorResponse.error : "Kon verhaal niet genereren.";
        toast.error(errMsg);
        currentStep.value = 1; // Back to wizard
    }
}

async function handleValidateStory() {
    if (!form.value.title.trim() || !form.value.content.trim()) {
        toast.error("Titel en inhoud zijn verplicht.");
        return;
    }
    if (selectedConditions.value.length === 0) {
        toast.error("Selecteer een aandoening.");
        return;
    }

    currentStep.value = 4; // Validating
    const result = await storyStore.validateStory(form.value.title, form.value.content);

    if (result.state === "success") {
        validationResult.value = result.response;
        currentStep.value = 5; // Final
    } else {
        const errMsg = result.state === 'failed' ? result.errorResponse.error : "Validatie mislukt.";
        toast.error(errMsg);
        currentStep.value = 3; // Back to review
    }
}

// Conditions
function addCondition(name: string) {
    selectedConditions.value.push(name);
}
function removeCondition(name: string) {
    selectedConditions.value = selectedConditions.value.filter(n => n !== name);
}

// Deletion
const deletingStory = ref(false);
const showDeleteConfirm = ref(false);

async function onDeleteStory() {
    const storyId = storyDetailStore.story?.id;
    if (!storyId) return;

    deletingStory.value = true;
    const result = await storyStore.deleteStory(storyId);
    deletingStory.value = false;
    showDeleteConfirm.value = false;

    if (result.success) {
        toast.success("Verhaal verwijderd.");
        storyDetailStore.story = null;
        form.value = { title: "", content: "" };
        selectedConditions.value = [];
        creationMode.value = null;
        currentStep.value = 0; // Restart selection
    } else {
        toast.error(result.error || "Kon verhaal niet verwijderen.");
    }
}

// Final Publish
async function confirmPublish() {
    const payload: StoryPayload = {
        title: validationResult.value?.fixedTitle || form.value.title,
        content: validationResult.value?.fixedContent || form.value.content,
        conditionNames: selectedConditions.value
    } as any; // Cast partial payload

    // Handle ID if updating
    if (storyDetailStore.story?.id) {
        payload.id = storyDetailStore.story.id;
        const res = await storyStore.updateStory(payload);
        if (res.success) {
            toast.success("Verhaal bijgewerkt!");
            router.push({ name: "home" });
        } else {
            toast.error(res.error || "Update mislukt.");
        }
    } else {
        const res = await storyStore.publishStory(payload);
        if (res.success) {
            toast.success("Verhaal gepubliceerd!");
            router.push({ name: "home" });
        } else {
            toast.error(res.error || "Publicatie mislukt.");
        }
    }
}

</script>

<style scoped>
@import "../assets/story_style.css";

/* Reuse pulse animation */
@keyframes pulse-scale {
    0%, 100% { transform: scale(1); }
    50% { transform: scale(1.1); }
}
.pulse-scale {
    animation: pulse-scale 2s ease-in-out infinite;
}
</style>
