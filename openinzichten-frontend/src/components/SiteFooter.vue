<template>
    <footer
        class="relative bottom-0 border-t border-brand-purple-dark/50 bg-brand-purple py-6 text-sm text-white"
    >
        <div class="mx-auto grid max-w-7xl grid-cols-1 justify-center gap-2 px-4 sm:grid-cols-3 sm:gap-4">
            <div>
                <h1 class="mb-2 text-xl text-white">Open Inzicht</h1>
                <div class="text-white/90 flex flex-col space-y-1 leading-relaxed">
                    <p>Ontdek nieuwe en aanbevolen verhalen van onze community.</p>
                    <p>Deel, lees en verbind met anderen die jou begrijpen.</p>
                </div>
            </div>
            <div class="flex flex-col">
                <h1 class="mb-2 text-xl text-white">Links</h1>
                <RouterLink to="/terms" class="text-white/90 hover:text-white" id="NavigateTerms"
                    >Gebruiksvoorwaarden</RouterLink
                >
                <RouterLink to="/privacy" class="text-white/90 hover:text-white" id="NavigatePrivacy"
                    >Privacy</RouterLink
                >
                <RouterLink to="/about" class="text-white/90 hover:text-white" id="NavigateAboutFooter"
                    >Over ons</RouterLink
                >
            </div>
            <div class="flex flex-col">
                <h1 class="mb-2 text-xl text-white">Meedoen</h1>
                <RouterLink to="/register" class="text-white/90 hover:text-white" id="NavigateRegisterFooter"
                    >Word lid</RouterLink
                >
                <RouterLink to="/write" class="text-white/90 hover:text-white" id="NavigateWriteStoryFooter"
                    >Deel een verhaal</RouterLink
                >
            </div>
        </div>
        <div class="mx-auto mt-4 flex w-screen flex-col items-center border-t border-white/20 py-7">
            <span class="text-white/90">© 2025 OpenInzicht. Alle rechten voorbehouden.</span>
        </div>
    </footer>
</template>
