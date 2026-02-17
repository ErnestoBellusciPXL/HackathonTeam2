import { ref } from "vue";
import { apiFetch } from "@/utils/Api";
import { BACKEND_URL } from "@/stores/config";

export interface MunicipalityCounts {
    [municipalityCode: string]: number;
}

interface BackendCountItem {
    municipalityCode?: string;
    municipality?: string;
    count?: number; // present when a specific condition is requested
    amountOfCommunitymembers?: number; // present when condition= (all conditions)
    countsByCondition?: Record<string, number>; // legacy, kept for backwards compatibility
}

interface BackendCondition {
    name: string;
}

export function useHeatMapCounts() {
    const conditions = ref<string[]>([]);
    const selectedCondition = ref<string>("all");
    const municipalityCounts = ref<MunicipalityCounts>({});
    const isLoading = ref(false);
    const error = ref<string | null>(null);

    async function loadConditions(): Promise<void> {
        try {
            const res = await apiFetch(`${BACKEND_URL}/conditions`);
            if (res.ok) {
                const arr = (await res.json()) as BackendCondition[];
                conditions.value = arr.map((c) => c.name);
                return;
            }
        } catch (e) {
            console.warn("Failed to load conditions from backend", e);
        }

        // Fallback to mock
        try {
            const mock = await fetch("/mock-conditions.json");
            if (mock.ok) {
                const arr = (await mock.json()) as BackendCondition[];
                conditions.value = arr.map((c) => c.name);
            }
        } catch (e) {
            console.warn("Failed to load mock conditions", e);
            error.value = "Could not load conditions";
        }
    }

    function parseCounts(arr: BackendCountItem[]): MunicipalityCounts {
        const counts: MunicipalityCounts = {};
        for (const item of arr) {
            const key = item.municipalityCode ?? item.municipality;
            if (!key) continue;
            // If backend returned a single condition, use item.count
            if (typeof item.count === "number") {
                counts[key] = item.count;
                continue;
            }
            // If backend returned amountOfCommunitymembers (all conditions, unique count)
            if (typeof item.amountOfCommunitymembers === "number") {
                counts[key] = item.amountOfCommunitymembers;
                continue;
            }
            // Legacy: sum all countsByCondition (kept for backwards compatibility)
            if (item.countsByCondition && typeof item.countsByCondition === "object") {
                const total = Object.values(item.countsByCondition)
                    .filter((v) => typeof v === "number")
                    .reduce((a, b) => a + b, 0);
                counts[key] = total;
            }
        }
        return counts;
    }

    async function fetchCounts(conditionName: string): Promise<void> {
        isLoading.value = true;
        error.value = null;
        municipalityCounts.value = {};

        try {
            const param = conditionName === "all" ? "" : conditionName;
            const resp = await apiFetch(
                `${BACKEND_URL}/heatmap/counts-by-municipality?condition=${encodeURIComponent(param)}`,
            );

            if (!resp.ok) throw new Error(`Backend responded ${resp.status}`);

            const arr = (await resp.json()) as BackendCountItem[];
            municipalityCounts.value = parseCounts(arr);
        } catch (e) {
            console.warn("Backend fetch failed, trying mock", e);

            try {
                const mock = await fetch("/mock-counts.json");
                if (!mock.ok) throw new Error("Mock not available");

                const arr = (await mock.json()) as BackendCountItem[];
                municipalityCounts.value = parseCounts(arr);
            } catch (mockError) {
                console.warn("Mock fetch also failed", mockError);
                error.value = "Could not load user counts";
            }
        } finally {
            isLoading.value = false;
        }
    }

    return {
        conditions,
        selectedCondition,
        municipalityCounts,
        isLoading,
        error,
        loadConditions,
        fetchCounts,
    };
}
