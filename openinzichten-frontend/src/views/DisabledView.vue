<script setup lang="ts">
import { useRoute, useRouter } from "vue-router";
import { computed, onMounted } from "vue";
import { useAccountStatusStore } from "@/stores/accountStatus";
import SidePanelImage from "@/components/SidePanelImage.vue";
import Button3D from "@/components/Button3D.vue";

const route = useRoute();
const router = useRouter();
const accountStatus = useAccountStatusStore();

const disabledReason = computed<string | null>(() => {
    if (route.query.msg) return String(route.query.msg);
    return accountStatus.disabledReason ?? null;
});

function goHome() {
    void router.push({ name: "home" });
}

onMounted(() => {
    void accountStatus.checkMe({ suppressToast: true });
});
</script>

<template>
    <main class="min-h-screen min-w-full grid grid-cols-1 xl:grid-cols-2 gap-4">
        <section class="min-h-screen sm:min-h-full flex flex-col items-center justify-center">
            <div class="max-w-2xl w-full p-8 text-center">
                <h1 class="text-4xl md:text-5xl font-extrabold text-gray-900 mb-4">Account gedeactiveerd</h1>

                <p class="mb-4 text-lg text-gray-700">Beste gebruiker,</p>

                <p class="mb-6 text-base md:text-lg text-gray-700">
                    Je account op <span class="font-semibold">OpenInzicht.be</span> is uitgeschakeld omdat het
                    gedrag of de inhoud in strijd was met onze communityrichtlijnen.
                </p>

                <div class="flex items-center justify-center gap-3 mb-6">
                    <span class="text-base font-semibold">Reden:</span>
                    <span
                        v-if="disabledReason"
                        class="inline-block bg-red-500 text-white text-sm font-medium px-3 py-1 rounded-full"
                    >
                        {{ disabledReason }}
                    </span>
                    <span
                        v-else
                        class="inline-block bg-gray-200 text-gray-800 text-sm font-medium px-3 py-1 rounded-full"
                    >
                        (geen reden opgegeven)
                    </span>
                </div>

                <p class="mb-6 text-sm text-gray-600">
                    Als je denkt dat er een fout is gemaakt, of als je meer informatie wilt, neem dan gerust
                    contact op via
                    <a href="mailto:contact@openinzicht.be" class="text-brand-purple underline"
                        >contact@openinzicht.be</a
                    >
                </p>

                <div class="flex justify-center mb-6">
                    <Button3D asTag="button" variant="primary" @click="goHome">
                        Terug naar home
                    </Button3D>
                </div>
            </div>
        </section>

        <section class="relative hidden xl:flex items-stretch bg-brand-vanilla">
            <SidePanelImage class="h-full w-full" />
        </section>
    </main>
</template>
