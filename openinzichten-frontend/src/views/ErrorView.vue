

<script setup lang="ts">
import { useRouter } from "vue-router";
import Button3D from "@/components/Button3D.vue";
const router = useRouter();

function goHome() {
	// Try named route first, fallback to root path
	router
		.push({ name: "home" })
		.catch(() => {
			router.push("/");
		});
}
</script>

<template>
	<div class="min-h-screen flex items-center justify-center bg-white">
		<div class="w-full max-w-4xl mx-auto flex flex-col items-center text-center transform -translate-y-8 md:-translate-y-12">
			<img
				src="../assets/error.svg"
				alt="404 illustration"
				class="w-64 md:w-80 lg:w-[420px] h-auto"
			/>

			<h1 class="text-3xl mb-10 md:text-4xl font-extrabold text-brand-purple">Oeps! <span class=" text-brand-cyan">Deze pagina is nergens te vinden</span></h1>

			<p class="text-gray-600 max-w-lg mb-0 leading-tight mb-2">
				De link werkt niet of de pagina bestaat niet.
			</p>
			<p class="text-gray-600 max-w-lg mt-0 leading-tight mb-10">
				Probeer opnieuw of ga naar de homepagina.
			</p>

			<div class="flex items-center justify-center">
				<Button3D
					@click="goHome"
					class="inline-flex items-center gap-3 px-5 py-3 rounded-full text-white font-medium shadow-md focus:outline-none focus:ring-4 focus:ring-sky-200"
					aria-label="Terug naar Home"
				>
					Terug naar Home
				</Button3D>
			</div>
		</div>
	</div>
</template>
