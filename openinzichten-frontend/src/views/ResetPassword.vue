<script setup lang="ts">
import { ref, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import Button3D from "@/components/Button3D.vue";
import MovingInput from "@/components/MovingInput.vue";
import SidePanelImage from "@/components/SidePanelImage.vue";
import { useAuthStore } from "@/stores/auth";
import { useToastStore } from "@/stores/toast";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const toast = useToastStore();

const token = ref<string | null>(null);
const password = ref("");
const passwordConfirm = ref("");

// Errors and success are shown via toast notifications
const isSubmitting = ref(false);

onMounted(() => {
    // read token from query param
    const t = route.query.token;
    if (typeof t === "string") token.value = t;
});

function validateForm() {
    if (!token.value) {
        toast.error("Ongeldige url.");
        return false;
    }

    if (!password.value || !passwordConfirm.value) {
        toast.error("Vul alle velden in.");
        return false;
    }

    if (password.value.length < 8) {
        toast.error("Wachtwoord moet minimaal 8 tekens bevatten.");
        return false;
    }

    if (password.value !== passwordConfirm.value) {
        toast.error("Wachtwoorden komen niet overeen.");
        return false;
    }

    const passwordRegex = /^(?=.*[A-Z])(?=.*\d)(?=.*[!@#$%^&*?,])/;
    if (!passwordRegex.test(password.value)) {
        toast.error("Wachtwoord moet minstens één hoofdletter, één cijfer en één speciaal teken bevatten.");
        return false;
    }

    // no inline error state; toasts used instead
    return true;
}

async function handleSubmit(event?: Event) {
    event?.preventDefault();
    if (isSubmitting.value) return;
    if (!validateForm()) return;

    isSubmitting.value = true;
    try {
        // assume authStore.resetPassword(token, newPassword) exists and returns { success: true } or { error: string }
        const resp = (await authStore.resetPassword(token.value!, password.value)) as
            | { success?: boolean; message?: string }
            | { error: string };

        if ((resp as { error: string }).error) {
            toast.error((resp as { error: string }).error);
            return;
        }

        toast.success("Wachtwoord is gewijzigd. Je kunt nu inloggen.");

        // optionally redirect to login after a short delay
        setTimeout(() => router.push({ name: "login" }), 1500);
    } finally {
        isSubmitting.value = false;
    }
}
</script>

<template>
    <main class="min-h-screen min-w-full grid grid-cols-1 xl:grid-cols-2 gap-4">
        <section class="min-h-screen sm:min-h-full flex flex-col items-center pt-10">
            <h1 class="font-bold text-5xl text-center">Nieuw wachtwoord instellen</h1>
            <br />
            <span class="text-center">Voer een nieuw wachtwoord in voor je account</span>

            <form @submit.prevent="handleSubmit" class="flex flex-col items-center w-full">
                <div class="flex flex-col gap-4 mt-10 w-3/4 relative items-center">
                    <MovingInput id="password" label="Nieuw wachtwoord" v-model="password" type="password" />
                    <MovingInput
                        id="passwordConfirm"
                        label="Bevestig wachtwoord"
                        v-model="passwordConfirm"
                        type="password"
                    />
                </div>

                <!-- Notifications are shown via toast; inline messages removed -->

                <div class="w-3/4 mt-10 mb-10 flex flex-col">
                    <div class="flex justify-end">
                        <Button3D
                            variant="primary"
                            type="button"
                            class="w-full block"
                            @click="handleSubmit"
                            id="ResetPasswordButton"
                        >
                            Wachtwoord Resetten <span class="ms-3 text-2xl">&rarr;</span>
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
