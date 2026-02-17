<script setup lang="ts">
import { useAuthStore } from "@/stores/auth";
import Button3D from "./Button3D.vue";
import IconNotification from "./icons/IconNotification.vue";
import { ref, watch, onUnmounted } from "vue";
import { useRouter } from "vue-router";
import { useStoryDetailStore } from "@/stores/storyDetail";
import { useToastStore } from "@/stores/toast";

const authStore = useAuthStore();
const mobileMenuOpen = ref(false);
const toggleMobileMenu = () => (mobileMenuOpen.value = !mobileMenuOpen.value);
const closeMobileMenu = () => (mobileMenuOpen.value = false);
const router = useRouter();
const storyDetailStore = useStoryDetailStore();
const toast = useToastStore();

// Body scroll lock when mobile menu is open
const scrollY = ref(0);
function lockBodyScroll() {
    scrollY.value = window.scrollY || window.pageYOffset || 0;
    const body = document.body;
    body.style.position = "fixed";
    body.style.top = `-${scrollY.value}px`;
    body.style.left = "0";
    body.style.right = "0";
    body.style.width = "100%";
    body.style.overflow = "hidden"; // disable horizontal & vertical scrolling
}

function unlockBodyScroll() {
    const body = document.body;
    body.style.position = "";
    body.style.top = "";
    body.style.left = "";
    body.style.right = "";
    body.style.width = "";
    body.style.overflow = "";
    // Restore previous scroll position
    window.scrollTo(0, scrollY.value || 0);
}

watch(
    mobileMenuOpen,
    (open) => {
        if (open) {
            lockBodyScroll();
        } else {
            unlockBodyScroll();
        }
    },
    { flush: "post" }
);

onUnmounted(() => {
    if (mobileMenuOpen.value) {
        unlockBodyScroll();
    }
});

async function navigateToMyStory() {
    // Ensure we close mobile menu when navigating
    closeMobileMenu();
    try {
        await storyDetailStore.fetchMyStory();
        if (storyDetailStore.story) {
            router.push({ path: "/my-story" });
        } else {
            router.push({ path: "/write" });
        }
    } catch {
        toast.error("Kon verhaal niet bepalen.");
        // Fallback to my-story route so user can create or see an error
        router.push({ path: "/my-story" });
    }
}
</script>

<template>
    <header class="w-screen border-b bg-white/80 backdrop-blur sticky top-0 z-50">
        <div class="mx-auto px-4 py-1 flex items-center justify-between">
            <RouterLink to="/" class="flex items-center gap-2" @click="closeMobileMenu()">
                <img src="/src/resources/OpenInzicht.svg" alt="OpenInzicht" class="h-24 w-auto" />
            </RouterLink>

            <!-- Desktop navigation -->
            <nav class="hidden md:flex items-center gap-10">
                <RouterLink to="/stories" class="brand-link-purple-dark-bold"> Verhalen </RouterLink>
                <RouterLink to="/heatmap" class="brand-link-purple-dark-bold"> Regio </RouterLink>
                <RouterLink
                    v-if="authStore.isUserLoggedInRef"
                    to="/connections"
                    class="brand-link-purple-dark-bold"
                >
                    Connecties
                </RouterLink>
                <RouterLink to="/about" class="brand-link-purple-dark-bold"> Over Ons</RouterLink>
                <RouterLink v-if="authStore.isUserAdmin()" to="/admin" class="brand-link-purple-dark-bold">
                    Admin Dashboard
                </RouterLink>
            </nav>

            <!-- Desktop right side (auth) -->
            <div class="hidden md:block">
                <div v-if="authStore.isUserLoggedInRef" class="flex flex-row items-center">
                    <RouterLink
                        to="/notifications"
                        class="brand-link-purple-dark-bold me-4 stroke-black hover:stroke-brand-purple-dark transition duration-300"
                    >
                        <IconNotification class="w-5" />
                    </RouterLink>

                    <!-- Username with hover dropdown -->
                    <div class="relative me-4">
                        <div class="group">
                            <RouterLink
                                to="/settings"
                                class="brand-link-purple-dark-bold"
                                id="HoverUsername"
                                >{{ authStore.getCurrentUser()?.sub }}</RouterLink
                            >

                            <!-- Dropdown shown on hover using Tailwind group utilities -->
                            <div
                                class="absolute right-0 mt-2 w-44 bg-white rounded-md shadow-lg ring-0 opacity-0 invisible group-hover:opacity-100 group-hover:visible transform scale-95 group-hover:scale-100 transition-all duration-150 z-30"
                            >
                                <RouterLink
                                    to="/settings"
                                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                    id="NavigateToSettingsButton"
                                >
                                    Instellingen
                                </RouterLink>
                                <a
                                    href="#"
                                    class="block px-4 py-2 text-sm text-gray-700 hover:bg-gray-100"
                                    id="NavigateToMyStoryButton"
                                    @click.prevent="navigateToMyStory"
                                >
                                    Mijn Verhaal
                                </a>
                            </div>
                        </div>
                    </div>
                </div>
                <div v-else>
                    <RouterLink to="/login" class="brand-button-purple-dark"> Login </RouterLink>
                    <RouterLink to="/register" class="brand-button-purple-dark-filled ms-4">
                        <Button3D variant="primary"> Word Lid </Button3D>
                    </RouterLink>
                </div>
            </div>

            <!-- Mobile hamburger -->
            <button
                class="md:hidden flex flex-col justify-center items-center w-8 h-8 gap-[3px]"
                @click="toggleMobileMenu"
                aria-label="Open menu"
            >
                <span
                    class="block w-6 h-0.5 bg-gray-400 transition"
                    :class="{ 'rotate-45 translate-y-[7px]': mobileMenuOpen }"
                ></span>
                <span
                    class="block w-6 h-0.5 bg-gray-400 transition"
                    :class="{ 'opacity-0': mobileMenuOpen }"
                ></span>
                <span
                    class="block w-6 h-0.5 bg-gray-400 transition"
                    :class="{ '-rotate-45 -translate-y-[7px]': mobileMenuOpen }"
                ></span>
            </button>
        </div>
    </header>

    <!-- Mobile fullscreen menu teleported to body so het echt 100vh inneemt -->
    <teleport to="body">
        <transition name="fade">
            <div v-if="mobileMenuOpen" class="fixed inset-0 z-50 md:hidden flex justify-end">
                <!-- Klikbare overlay links -->
                <div class="absolute inset-0 bg-black/40 backdrop-blur-sm" @click="closeMobileMenu"></div>
                <!-- Paneel rechts -->
                <div
                    class="relative w-3/4 max-w-[480px] bg-white min-h-screen flex flex-col px-6 py-8 shadow-xl animate-[slideIn_.25s_ease]"
                >
                    <button
                        class="absolute top-4 right-4 text-2xl leading-none"
                        aria-label="Sluit menu"
                        @click="closeMobileMenu"
                    >
                        ×
                    </button>
                    <div class="flex flex-col gap-6 mt-12 grow justify-center items-center">
                        <RouterLink
                            to="/stories"
                            class="brand-link-purple-dark-bold text-center"
                            @click="closeMobileMenu"
                        >
                            Verhalen
                        </RouterLink>
                        <RouterLink
                            to="/heatmap"
                            class="brand-link-purple-dark-bold text-center"
                            @click="closeMobileMenu"
                        >
                            Regio
                        </RouterLink>
                        <RouterLink to="/about" class="brand-link-purple-dark-bold text-center" @click="closeMobileMenu">
                            Over ons
                        </RouterLink>
                        <RouterLink
                            v-if="authStore.isUserLoggedInRef"
                            to="/connections"
                            class="brand-link-purple-dark-bold text-center"
                            @click="closeMobileMenu"
                        >
                            Connecties
                        </RouterLink>
                        <RouterLink
                            v-if="authStore.isUserAdmin()"
                            to="/admin"
                            class="brand-link-purple-dark-bold text-center"
                            @click="closeMobileMenu"
                        >
                            Admin Dashboard
                        </RouterLink>
                    </div>
                    <div class="mt-auto pb-12">
                        <div v-if="authStore.isUserLoggedInRef" class="flex flex-col gap-4">
                            <RouterLink
                                to="/settings"
                                class="brand-link-purple-dark-bold text-center"
                                @click="closeMobileMenu"
                                >Mijn Profiel</RouterLink
                            >
                            <a
                                href="#"
                                class="brand-link-purple-dark-bold text-center"
                                @click.prevent="navigateToMyStory"
                                >Mijn Verhaal</a
                            >
                        </div>
                        <div v-else class="flex flex-col gap-4 items-stretch">
                            <RouterLink
                                to="/login"
                                class="brand-link-purple-dark-bold text-center"
                                @click="closeMobileMenu"
                                >Login</RouterLink
                            >
                            <RouterLink
                                to="/register"
                                class="brand-button-purple-dark-filled"
                                @click="closeMobileMenu"
                            >
                                <Button3D variant="primary" class="w-full"> Word Lid </Button3D>
                            </RouterLink>
                        </div>
                    </div>
                </div>
            </div>
        </transition>
    </teleport>
</template>
