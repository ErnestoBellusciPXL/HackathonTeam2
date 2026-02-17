<script setup lang="ts">
import { RouterView } from "vue-router";
import NavigationBar from "./components/NavigationBar.vue";
import SiteFooter from "./components/SiteFooter.vue";
import ToastContainer from "./components/ToastContainer.vue";
import ChatLauncher from "./components/chat/ChatLauncher.vue";

// Chat UI moved to `ChatLauncher.vue`
</script>

<template>
    <div class="min-h-screen flex flex-col overflow-x-hidden">
        <NavigationBar />
        <main class="overflow-x-hidden">
            <RouterView />
        </main>

        <ChatLauncher />

        <SiteFooter />

        <ToastContainer />
    </div>
</template>
