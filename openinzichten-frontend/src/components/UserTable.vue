<template>
    <div class="bg-white border-2 border-brand-purple rounded-xl p-6">
        <h2 class="text-2xl font-bold mb-2">Gebruikers</h2>
        <p class="text-gray-600 mb-6">Filter en beheer alle gebruikers in het systeem</p>

        <div class="flex flex-col md:flex-row gap-4 mb-6 md:items-center">
            <div class="flex-2"></div>
            <div class="flex flex-col md:flex-row gap-2 w-full md:w-auto">
                <div class="w-full md:w-64">
                    <FilterDropdown
                        v-model="conditionFilter"
                        :options="conditionOptions"
                        placeholder="Aandoening"
                    />
                </div>

                <div class="w-full md:w-64">
                    <FilterDropdown
                        v-model="statusFilter"
                        :options="[
                            { label: 'Alle statussen', value: '' },
                            { label: 'Actief', value: 'active' },
                            { label: 'Gedeactiveerd', value: 'deactivated' },
                        ]"
                        placeholder="Status"
                    />
                </div>
            </div>
        </div>

        <div class="border-2 border-gray-200 rounded-xl overflow-y-auto max-h-96">
            <table class="w-full">
                <thead class="bg-gray-50 border-b-2 border-gray-200">
                    <tr>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">
                            Gebruikersnaam
                        </th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">Status</th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">Aandoening</th>
                        <th class="px-6 py-4 text-left text-sm font-semibold text-gray-700">Acties</th>
                    </tr>
                </thead>
                <tbody>
                    <tr
                        v-for="(user, index) in filteredUsers"
                        :key="user.id"
                        class="border-b border-gray-200 hover:bg-gray-50 transition"
                        :class="{ 'bg-gray-50': index % 2 === 1 }"
                    >
                        <td class="px-6 py-4 text-sm">{{ user.username }}</td>
                        <td class="px-6 py-4">
                            <span
                                :class="user.disabled ? 'inline-flex items-center gap-2 px-3 py-1 rounded-full bg-red-100 text-red-700 text-sm' : 'inline-flex items-center gap-2 px-3 py-1 rounded-full bg-green-100 text-green-700 text-sm'"
                            >
                                {{ user.disabled ? 'Gedeactiveerd' : 'Actief' }}
                            </span>
                        </td>
                        <td class="px-6 py-4">
                            <div class="flex flex-wrap gap-1">
                                <span
                                    v-for="condition in user.conditions"
                                    :key="condition"
                                    class="inline-block bg-brand-purple text-white text-xs px-3 py-1 rounded-full"
                                >
                                    {{ condition }}
                                </span>
                                <span v-if="user.conditions.length === 0" class="text-gray-400 text-xs">
                                    Geen aandoening
                                </span>
                            </div>
                        </td>
                        <td class="px-6 py-4">
                            <Button3D variant="primary" class="text-xs px-4 py-2" @click="manageUser(user)">
                                Beheren
                            </Button3D>
                        </td>
                    </tr>
                </tbody>
            </table>
        </div>

        <PaginationControls
            :current-page="currentPage"
            :total-pages="totalPages"
            @update:current-page="$emit('update:currentPage', $event)"
        />
    </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from "vue";
import type { Content } from "@/utils/Admin";
import Button3D from "./Button3D.vue";
import FilterDropdown from "./FilterDropdown.vue";
import PaginationControls from "./PaginationControls.vue";
import type { Condition } from "@/utils/Condition";
import { useConditionsStore } from "@/stores/conditions";

const props = defineProps<{
    users: Content[];
    currentPage: number;
    totalPages: number;
    searchQuery: string;
    conditionFilter: string;
    statusFilter: string;
}>();

const emit = defineEmits<{
    "update:searchQuery": [value: string];
    "update:conditionFilter": [value: string];
    "update:statusFilter": [value: string];
    "update:currentPage": [page: number];
    manageUser: [user: Content];
}>();

const conditionsStore = useConditionsStore();
const conditions = ref<Condition[]>([]);

const conditionFilter = computed({
    get: () => props.conditionFilter,
    set: (value) => emit("update:conditionFilter", value),
});

const statusFilter = computed({
    get: () => props.statusFilter,
    set: (value) => emit("update:statusFilter", value),
});

const conditionOptions = computed(() => [
    { label: "Alle aandoeningen", value: "" },
    ...conditions.value.map((condition) => ({ label: condition.name, value: condition.name })),
]);

const filteredUsers = computed(() => {
    let users = props.users;

    // Filter by condition
    if (props.conditionFilter) {
        users = users.filter((user) => user.conditions.includes(props.conditionFilter));
    }

    // Filter by status if provided (expecting 'active' or 'deactivated' or empty)
    if (props.statusFilter) {
        const sf = props.statusFilter.toLowerCase();
        if (sf === "active") {
            users = users.filter((u) => !u.disabled);
        } else if (sf === "deactivated" || sf === "gedeactiveerd" || sf === "deactivated") {
            users = users.filter((u) => !!u.disabled);
        }
    }

    return users;
});

function manageUser(user: Content) {
    emit("manageUser", user);
}

onMounted(async () => {
    const res = await conditionsStore.fetchAll();
    if (Array.isArray(res)) {
        conditions.value = res;
    } else {
        // non-blocking: log error

        console.error("Failed to load conditions:", res);
    }
});
</script>
