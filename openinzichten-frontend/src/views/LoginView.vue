<script setup lang="ts">
import Button3D from "@/components/Button3D.vue";
import MovingInput from "@/components/MovingInput.vue";
import { useAuthStore } from "@/stores/auth";
import type { AuthOrErrorResponse } from "@/utils/Auth";
import { ref } from "vue";
import { useToastStore } from "@/stores/toast";
import { useRouter } from "vue-router";
import SidePanelImage from "@/components/SidePanelImage.vue";

const authStore = useAuthStore();
const router = useRouter();
const toast = useToastStore();

function isLoginError(resp: AuthOrErrorResponse) {
    return resp.error !== undefined;
}

const rememberMe = ref(false);
const email = ref("");
const password = ref("");

const errorMessage = ref<string | null>(null);

function validateForm() {
    if (!email.value || !password.value) {
        errorMessage.value = "Vul alle velden in.";
        toast.error(errorMessage.value!);
        return false;
    }

    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

    if (!emailRegex.test(email.value)) {
        errorMessage.value = "Voer een geldig e-mailadres in.";
        toast.error(errorMessage.value!);

        return false;
    }

    if (password.value.length < 8) {
        errorMessage.value = "Wachtwoord moet minstens 8 tekens lang zijn.";
        toast.error(errorMessage.value!);
        return false;
    }

    errorMessage.value = null;
    return true;
}

async function handleSubmit(event?: Event) {
    // if called as form submit, prevent actual navigation
    event?.preventDefault();

    if (!validateForm()) return;

    const response = await authStore.login(email.value, password.value, rememberMe.value);

    // auth store returns { error: string } on failures
    if (isLoginError(response)) {
        // special case: account disabled
        if ('disabled' in response && response.disabled) {
            await router.push({ name: "disabled", query: { msg: response.error } });
            return;
        }

        errorMessage.value = response.error!;
        toast.error(errorMessage.value!);
        return;
    }

    // success path: store token and navigate to Home
    localStorage.setItem("token", response.token!);
    errorMessage.value = null;
    toast.success("Inloggen gelukt.");
    await router.push({ name: "home" });
}
</script>

<template>
    <main class="min-h-screen min-w-full grid grid-cols-1 xl:grid-cols-2 gap-4">
        <section
            class="min-h-screen sm:min-h-full flex flex-col items-center justify-center xl:justify-start pt-0 xl:pt-30"
        >
            <h1 class="font-bold text-5xl">Welkom</h1>
            <br />
            <span class="text-center px-5"
                >Deel je verhaal, vind lotgenoten en krijg toegang tot informatie die echt bij jou past.</span
            >

            <form action="post" class="flex flex-col items-center w-full max-w-[360px] sm:max-w-[560px]">
                <div class="flex flex-col gap-4 mt-10 relative items-center">
                    <div class="mt-1 w-full sm:w-[560px]">
                        <MovingInput
                            id="email"
                            label="E-mail"
                            v-model="email"
                            inputStyle="w-full"
                        />
                    </div>

                    <div class="mt-1 w-full sm:w-[560px]">
                        <MovingInput
                            id="password"
                            label="Wachtwoord"
                            v-model="password"
                            type="password"
                            inputStyle="w-full"
                        />
                    </div>
                </div>

                <!-- Errors are shown via toast notifications -->

                <div class="w-full sm:w-[560px] self-start flex items-center justify-center pt-4">
                    <input
                        type="checkbox"
                        name="terms"
                        id="terms"
                        class="me-3 w-4 h-4"
                        v-model="rememberMe"
                    />
                    <label for="terms" class="text-sm text-gray-600">Ingelogd blijven</label>
                </div>

                <div class="sm:w-3/4 mt-10 mb-10 flex flex-col">
                    <div class="flex justify-end mb-10">
                        <Button3D
                            variant="primary"
                            class="w-full block"
                            @click="handleSubmit"
                            id="loginButton"
                        >
                            Log in <span class="ms-3 text-2xl">&rarr;</span>
                        </Button3D>
                    </div>

                    <div class="text-center mt-2">
                        <RouterLink :to="{ name: 'forgot-password' }" class="text-sm text-brand-purple">
                            Wachtwoord vergeten?
                        </RouterLink>
                    </div>

                    <div class="text-center mt-2">
                        <RouterLink :to="{ name: 'register' }" class="text-sm text-brand-purple">
                            Nog geen account? Registreer hier
                        </RouterLink>
                    </div>
                </div>
            </form>
        </section>

        <section class="relative hidden xl:flex items-stretch bg-brand-vanilla">
            <SidePanelImage class="h-full w-full" />
        </section>
    </main>
</template>
