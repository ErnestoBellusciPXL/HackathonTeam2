<script setup lang="ts">
import Button3D from "@/components/Button3D.vue";
import MovingInput from "@/components/MovingInput.vue";
import SidePanelImage from "@/components/SidePanelImage.vue";
import { useAuthStore } from "@/stores/auth";
import { ref } from "vue";

const authStore = useAuthStore();

type ResetSuccess = { success: true; message?: string };
type LoginError = { error: string };

function isError(resp: ResetSuccess | LoginError): resp is LoginError {
    return (resp as LoginError).error !== undefined;
}
const email = ref("");

const errorMessage = ref<string | null>(null);
const successMessage = ref<string | null>(null);
const isSubmitting = ref(false);

function validateForm() {
    if (!email.value) {
        errorMessage.value = "Vul alle velden in.";
        return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailRegex.test(email.value)) {
        errorMessage.value = "Voer een geldig e-mailadres in.";

        return false;
    }

    errorMessage.value = null;
    return true;
}

async function handleSubmit(event?: Event) {
    // if called as form submit, prevent actual navigation
    event?.preventDefault();

    // prevent double submissions
    if (isSubmitting.value) {
        return;
    }

    if (!validateForm()) return;

    isSubmitting.value = true;
    try {
        const response = (await authStore.sendResetUrl(email.value)) as ResetSuccess | LoginError;

        // auth store returns { error: string } on failures
        if (isError(response)) {
            errorMessage.value = response.error;
            successMessage.value = null;
            return;
        } else {
            successMessage.value = "Resetlink succesvol verzonden naar je e-mailadres.";
            errorMessage.value = null;
        }
    } finally {
        isSubmitting.value = false;
    }
}
</script>

<template>
    <main class="min-h-screen min-w-full grid grid-cols-1 xl:grid-cols-2 gap-4">
        <section class="min-h-screen sm:min-h-full flex flex-col items-center pt-10">
            <h1 class="font-bold justify-center text-center items-center flex text-5xl">
                Wachtwoord opnieuw instellen
            </h1>
            <br />
            <span class="text-center">Vul je e-mailadres in om een resetlink te ontvangen</span>

            <form @submit.prevent="handleSubmit" class="flex flex-col items-center">
                <div class="flex flex-col gap-4 mt-10 w-3/4 relative items-center">
                    <MovingInput id="email" label="E-mail" v-model="email" />
                </div>

                <div v-if="errorMessage" class="mt-4 text-red-600">
                    <span>{{ errorMessage }}</span>
                </div>

                <div v-if="successMessage" class="mt-4 text-green-600">
                    <span>{{ successMessage }}</span>
                </div>

                <div class="w-3/4 mt-10 mb-10 flex flex-col">
                    <div class="flex justify-end">
                        <Button3D
                            variant="primary"
                            type="button"
                            class="w-full block"
                            @click="handleSubmit"
                            id="SendResetLinkButton"
                        >
                            Verstuur resetlink <span class="ms-3 text-2xl">&rarr;</span>
                        </Button3D>
                    </div>
                </div>
            </form>
        </section>

        <section class="relative hidden xl:flex items-stretch bg-brand-vanilla">
            <SidePanelImage class="h-full w-full" />
        </section>
    </main>
</template>
