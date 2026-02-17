<script setup lang="ts">
import GroupSupportImage from "@/components/GroupSupportImage.vue";
import OpenInzichtLogo from "@/resources/OpenInzicht.svg";
</script>

<template>
    <div class="flex h-full w-full items-stretch justify-center">
        <div class="flex h-full w-full max-w-[660px] flex-col px-12 pt-24 pb-0 text-center">
            <div class="flex flex-col items-center gap-4">
                <img
                    :src="OpenInzichtLogo"
                    alt="OpenInzicht"
                    class="mt-6 h-auto w-full max-w-[440px] shrink-0 object-contain"
                />

                <p class="px-6 text-2xl font-semibold text-brand-purple-dark md:text-3xl">
                    Samen maken we het onzichtbare zichtbaar
                </p>
            </div>

            <div class="flex flex-1 items-end justify-center">
                <GroupSupportImage class="h-auto w-full max-w-[620px] shrink-0 object-contain" />
            </div>
        </div>
    </div>
</template>
