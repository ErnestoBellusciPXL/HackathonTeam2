<template>
    <div class="w-full h-[600px] relative">
        <LMap
            ref="mapRef"
            :zoom="8"
            :center="[51.0, 4.5]"
            :options="mapOptions"
            style="height: 100%; width: 100%"
            @ready="onMapReady"
        >
            <LTileLayer
                url="https://{s}.basemaps.cartocdn.com/light_all/{z}/{x}/{y}{r}.png"
                attribution="&copy; OpenStreetMap contributors & CARTO"
            />
        </LMap>

        <!-- Condition filter control -->
        <div class="absolute z-30 right-4 top-4 bg-white p-3 rounded shadow-md pointer-events-auto w-64">
            <label for="condition-select" class="block text-sm font-medium text-gray-700 mb-1">
                Filter op aandoening
            </label>
            <select
                id="condition-select"
                v-model="selectedCondition"
                @change="onConditionChange"
                class="w-full px-3 py-2 border border-gray-300 rounded-md text-sm focus:ring-2 focus:ring-blue-500 focus:border-blue-500"
                :disabled="isLoading"
            >
                <option value="all">Alle aandoeningen</option>
                <option v-for="c in conditions" :key="c" :value="c">{{ c }}</option>
            </select>

            <div v-if="isLoading" class="mt-2 text-xs text-gray-500">Gegevens ophalen...</div>

            <div v-if="error" class="mt-2 text-xs text-red-600">
                {{ error }}
            </div>
        </div>

        <!-- Legend -->
        <HeatMapLegend :buckets="legendBuckets" :max-count="maxCount" :total-count="totalCount" />
    </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from "vue";
import { LMap, LTileLayer } from "@vue-leaflet/vue-leaflet";
import type { FeatureCollection, Feature, Geometry, Polygon, MultiPolygon } from "geojson";
import type { Map as LeafletMap, PathOptions, Layer } from "leaflet";
import L from "leaflet";
import municipalityGeoUrl from "@/resources/georef-belgium-submunicipality.geojson?url";
import provincesGeoUrl from "@/resources/georef-belgium-provinces.geojson?url";
import HeatMapLegend from "./HeatMapLegend.vue";
import { useHeatMapCounts } from "@/composables/useHeatMapCounts";
import {
    matchCountsToFeatures,
    getMunicipalityCode,
    getMunicipalityName,
    isFlemishFeature,
} from "@/utils/geoDataMatcher";
import polygonClipping from "polygon-clipping";
import {
    aggregateByProvince,
    getColor,
    computeLegendBuckets,
    computeLegendBucketsFromCounts,
    getFlemishProvinces,
    type ProvinceData,
} from "@/utils/provinceAggregation";

// Use a dedicated Canvas renderer to avoid SVG anti-alias seams
const canvasRenderer = L.canvas({ padding: 0.5 });

// Data fetching composable
const { conditions, selectedCondition, municipalityCounts, isLoading, error, loadConditions, fetchCounts } =
    useHeatMapCounts();

// Map and GeoJSON refs
const mapRef = ref<{ leafletObject?: LeafletMap }>();
const municipalityGeoJSON = ref<FeatureCollection>();
const provincesGeoJSON = ref<FeatureCollection>();
const provinceData = ref<Map<string, ProvinceData>>(new Map()); // still used for legend aggregation (can revise later)
const municipalityMatchedCounts = ref<Map<string, number>>(new Map());

// Layer refs
const municipalityFillLayer = ref<L.GeoJSON>();
const municipalityOutlineLayer = ref<L.GeoJSON>();

// Computed values
const maxCount = computed(() => {
    // Use municipality counts (more granular) for coloring scale
    const vals = Array.from(municipalityMatchedCounts.value.values());
    return vals.length ? Math.max(...vals) : 0;
});

const totalCount = computed(() => {
    const vals = Array.from(municipalityMatchedCounts.value.values());
    return vals.length ? vals.reduce((s, v) => s + v, 0) : 0;
});

const legendBuckets = computed(() => {
    const counts = Array.from(municipalityMatchedCounts.value.values());
    const buckets =
        counts.length > 0 ? computeLegendBucketsFromCounts(counts) : computeLegendBuckets(provinceData.value);

    // Ensure 'max' is always a number to satisfy LegendBucket type
    return buckets.map((b) => ({ ...b, max: b.max ?? 0 }));
});

const mapOptions = {
    preferCanvas: true,
    minZoom: 7,
    maxZoom: 11,
    renderer: canvasRenderer,

    // Make panning constraints sticky so Belgium stays visible
    maxBoundsViscosity: 1,
};

/**
 * Style function for province features
 */
// (Kept for potential province overlay, currently not used for fill coloring)

/** Style function for municipality fill (no internal lines) */
function styleMunicipalityFill(feature?: Feature): PathOptions {
    if (!feature) return { weight: 0, fillOpacity: 0 };
    const munCode = getMunicipalityCode(feature) ?? "";
    const count = municipalityMatchedCounts.value.get(munCode) ?? 0;
    return {
        renderer: canvasRenderer,
        stroke: false, // explicitly disable stroke to prevent visible seams
        weight: 0,
        color: "transparent",
        fillColor: count > 0 ? getColor(count, maxCount.value) : "transparent",
        fillRule: "evenodd",
        // Slight transparency so the basemap shows through a bit
        fillOpacity: count > 0 ? 0.6 : 0,
        interactive: true,
    };
}

/** Style function for outer outline */
function styleMunicipalityOutline(feature?: Feature): PathOptions {
    if (!feature) return { weight: 1, color: "#000", fillOpacity: 0, interactive: false };
    const munCode = getMunicipalityCode(feature) ?? "";
    const count = municipalityMatchedCounts.value.get(munCode) ?? 0;
    return {
        renderer: canvasRenderer,
        weight: 1,
        color: count > 0 ? getColor(count, maxCount.value) : "#000",
        lineJoin: "round",
        lineCap: "round",
        opacity: 0.15,
        fillOpacity: 0,
        interactive: false,
    };
}

function onEachMunicipalityFeature(
    feature: Feature,
    layer: Layer & { bindTooltip?: (content: string, options?: unknown) => void },
) {
    const munName = getMunicipalityName(feature);
    const munCode = getMunicipalityCode(feature) ?? "";
    const count = municipalityMatchedCounts.value.get(munCode) ?? 0;
    if (typeof layer.bindTooltip === "function") {
        const label = `${munName}: ${count} ${count === 1 ? "communitylid" : "communityleden"}`;
        layer.bindTooltip(label, { sticky: true, className: "municipality-tooltip" });
    }
}

/**
 * Bind tooltip and click handlers to province features
 */

/**
 * Build HTML tooltip content showing province total and municipality breakdown
 */

type Coord = [number, number];

function extractPolys(feat: Feature): number[][][][] {
    if (feat.geometry.type === "Polygon") {
        return [feat.geometry.coordinates as number[][][]];
    }
    if (feat.geometry.type === "MultiPolygon") {
        return feat.geometry.coordinates as number[][][][];
    }
    return [];
}

function toCoordArray(ring: number[][]): Coord[] {
    return ring.filter(
        (pt): pt is Coord =>
            Array.isArray(pt) && pt.length === 2 && typeof pt[0] === "number" && typeof pt[1] === "number",
    );
}

function closeRing(ring: Coord[]): Coord[] {
    if (ring.length === 0) return ring;
    const first = ring[0]!;
    const last = ring.at(-1)!;
    if (first[0] === last[0] && first[1] === last[1]) return ring;
    return [...ring, [first[0], first[1]]];
}

function ringArea(ring: Coord[]): number {
    let area = 0;
    for (let i = 0, j = ring.length - 1; i < ring.length; j = i++) {
        const [xi, yi] = ring[i]!;
        const [xj, yj] = ring[j]!;
        area += xj * yi - xi * yj;
    }
    return area / 2;
}

function ensureCCW(ring: Coord[]): Coord[] {
    const closed = closeRing(ring);
    return ringArea(closed) < 0 ? closed.slice().reverse() : closed;
}

function dedupeConsecutive(ring: Coord[]): Coord[] {
    if (ring.length <= 2) return ring;
    const out: Coord[] = [];
    for (let i = 0; i < ring.length; i++) {
        const p = ring[i]!;
        const q = i > 0 ? ring[i - 1]! : undefined;
        if (!q || p[0] !== q[0] || p[1] !== q[1]) out.push(p);
    }
    return out;
}

function sanitizeOuter(ring: number[][]): number[][] {
    let r = toCoordArray(ring);
    r = dedupeConsecutive(r);
    r = ensureCCW(r);
    const unique = new Set(r.map((pt) => pt.join(",")));
    if (unique.size < 4) return [];
    return r as unknown as number[][];
}

function groupByMunicipality(features: Feature[]): Map<string, Feature[]> {
    const grouped = new Map<string, Feature[]>();
    for (const f of features) {
        const code = getMunicipalityCode(f);
        if (!code) continue;
        const arr = grouped.get(code) || [];
        arr.push(f);
        grouped.set(code, arr);
    }
    return grouped;
}

function sanitizePolySets(polySets: number[][][][]): number[][][][] {
    const seen = new Set<string>();
    const cleaned: number[][][][] = [];
    for (const poly of polySets) {
        const filtered = poly.filter((ring) => ring.length >= 4);
        if (!filtered.length) continue;
        const key = JSON.stringify(filtered);
        if (seen.has(key)) continue;
        seen.add(key);
        cleaned.push(filtered);
    }
    return cleaned;
}

function collectPolySets(feats: Feature[]): number[][][][] {
    const collected: number[][][][] = [];
    for (const f of feats) {
        collected.push(...extractPolys(f));
    }
    return sanitizePolySets(collected);
}

function mergePolygonSets(polySets: number[][][][], code: string): number[][][][] {
    if (!polySets.length) return [];
    try {
        const unionResult = polygonClipping.union(...polySets);
        if (unionResult && unionResult.length) {
            return unionResult as number[][][][];
        }
        console.warn("Union leeg voor", code, "valt terug op polySets");
        return polySets;
    } catch (e) {
        console.warn("polygon-clipping union error voor", code, e);
        return polySets;
    }
}

function buildPolygonGeometry(polygonCoords: number[][][]): Polygon | null {
    const rawOuter = toCoordArray(polygonCoords[0] ?? []);
    const cleanedOuter = sanitizeOuter(polygonCoords[0] ?? []);
    const ring = cleanedOuter.length >= 4 ? cleanedOuter : rawOuter;
    if (ring.length < 4) return null;
    return { type: "Polygon", coordinates: [ring] } as Polygon;
}

function buildMultiPolygonGeometry(polygons: number[][][][]): MultiPolygon | null {
    const multi: number[][][][] = [];
    for (const poly of polygons) {
        const cleanedOuter = sanitizeOuter(poly[0] ?? []);
        if (cleanedOuter.length >= 4) {
            multi.push([cleanedOuter]);
        }
    }
    if (!multi.length) return null;
    return { type: "MultiPolygon", coordinates: multi } as MultiPolygon;
}

function buildGeometryFromUnion(unionResult: number[][][][]): Geometry | null {
    if (!unionResult.length) return null;
    if (unionResult.length === 1) return buildPolygonGeometry(unionResult[0] as number[][][]);
    return buildMultiPolygonGeometry(unionResult);
}

function buildMunicipalityFeature(code: string, feats: Feature[]): Feature | null {
    if (!feats.length) return null;
    const polySets = collectPolySets(feats);
    if (!polySets.length) {
        console.warn("Geen polygon sets voor", code);
        return null;
    }
    const unionResult = mergePolygonSets(polySets, code);
    const geometry = buildGeometryFromUnion(unionResult);
    if (!geometry) return null;

    const first = feats[0]!;
    const baseProps: { [key: string]: unknown } = {
        ...((first.properties ?? {}) as { [key: string]: unknown }),
    };
    baseProps.mun_code = code;
    baseProps.mun_name_nl = getMunicipalityName(first);

    return { type: "Feature", properties: baseProps, geometry };
}

function mergeMunicipalities(grouped: Map<string, Feature[]>): Feature[] {
    const merged: Feature[] = [];
    for (const [code, feats] of grouped.entries()) {
        const feature = buildMunicipalityFeature(code, feats);
        if (feature) merged.push(feature);
    }
    return merged;
}

async function loadMunicipalityGeoJSON(): Promise<FeatureCollection | undefined> {
    const munResp = await fetch(municipalityGeoUrl);
    if (!munResp.ok) return;
    const fullMunicipalities = await munResp.json();
    const filteredFeatures = (fullMunicipalities.features as Feature[]).filter(isFlemishFeature);
    const grouped = groupByMunicipality(filteredFeatures);
    const mergedFill = mergeMunicipalities(grouped);
    return {
        type: "FeatureCollection",
        features: mergedFill,
    };
}

async function loadProvinceGeoJSON(): Promise<FeatureCollection | undefined> {
    const provResp = await fetch(provincesGeoUrl);
    if (!provResp.ok) return;
    const allProvinces = await provResp.json();
    return getFlemishProvinces(allProvinces);
}

function constrainMapToProvinces(): void {
    const map = mapRef.value?.leafletObject;
    if (!map || !provincesGeoJSON.value) return;
    try {
        const provBounds = L.geoJSON(provincesGeoJSON.value).getBounds();
        if (provBounds.isValid()) {
            map.fitBounds(provBounds, { padding: [20, 20] });
            map.setMaxBounds(provBounds.pad(0.05));
            map.options.maxBoundsViscosity = 1;
        }
    } catch (e) {
        console.warn("Failed to set province bounds on map", e);
    }
}

/**
 * Load GeoJSON files
 */
async function loadGeoJSON(): Promise<void> {
    try {
        const [municipalities, provinces] = await Promise.all([
            loadMunicipalityGeoJSON(),
            loadProvinceGeoJSON(),
        ]);

        if (municipalities) municipalityGeoJSON.value = municipalities;
        if (provinces) {
            provincesGeoJSON.value = provinces;
            constrainMapToProvinces();
        }
    } catch (e) {
        console.error("Failed to load GeoJSON files", e);
    }
}

/**
 * Update visualization with current data
 */
function updateVisualization(): void {
    if (!municipalityGeoJSON.value || !provincesGeoJSON.value) return;

    // Match backend counts to GeoJSON features
    const matchedCounts = matchCountsToFeatures(
        municipalityCounts.value,
        municipalityGeoJSON.value.features as Feature[],
    );
    municipalityMatchedCounts.value = matchedCounts;

    // Keep province aggregation for legend (optional)
    provinceData.value = aggregateByProvince(municipalityGeoJSON.value.features as Feature[], matchedCounts);

    // Render updated municipalities
    renderMunicipalities();
}

/**
 * Handle condition change
 */
async function onConditionChange(): Promise<void> {
    await fetchCounts(selectedCondition.value);
    updateVisualization();
}

/**
 * Handle map ready event
 */
function onMapReady(): void {
    const map = mapRef.value?.leafletObject;
    if (!map) return;
    const boundsSource = municipalityGeoJSON.value ? municipalityGeoJSON.value : provincesGeoJSON.value;
    if (boundsSource) {
        const bounds = L.geoJSON(boundsSource).getBounds();
        if (bounds.isValid()) {
            map.fitBounds(bounds, { padding: [20, 20] });
            map.setMaxBounds(bounds.pad(0.1));
        }
    }
    // Initial render if data present
    renderMunicipalities();
}

/**
 * Render GeoJSON layer on the map
 */
function renderMunicipalities(): void {
    const map = mapRef.value?.leafletObject;
    if (!map || !municipalityGeoJSON.value) return;

    // Fill layer (no internal lines)
    if (municipalityFillLayer.value) municipalityFillLayer.value.remove();
    municipalityFillLayer.value = L.geoJSON(municipalityGeoJSON.value, {
        style: (f) => styleMunicipalityFill(f),
        onEachFeature: onEachMunicipalityFeature,
    }).addTo(map);

    // Outline layer (outer boundary only) - verwijder interne ringen (holes)
    if (municipalityOutlineLayer.value) municipalityOutlineLayer.value.remove();
    const outlineFeatures: Feature[] = municipalityGeoJSON.value.features.map((f) => {
        const geom = f.geometry;
        const props = (f.properties ?? {}) as { [key: string]: unknown };
        if (geom.type === "Polygon") {
            const polyCoords = geom.coordinates as number[][][]; // [rings]
            const outer: number[][] = polyCoords[0] ?? [];
            const newGeom: Polygon = { type: "Polygon", coordinates: [outer] };
            return { type: "Feature", properties: { mun_code: props["mun_code"] }, geometry: newGeom };
        }
        if (geom.type === "MultiPolygon") {
            const multiCoords = geom.coordinates as number[][][][]; // [polygons][rings][points]
            const cleanedOuter = multiCoords
                .map((poly) => poly[0])
                .filter((ring): ring is number[][] => Array.isArray(ring) && ring.length >= 4);
            const multiCoordinates = cleanedOuter.map((ring) => [
                ring,
            ]) as unknown as MultiPolygon["coordinates"];
            const newGeom: MultiPolygon = { type: "MultiPolygon", coordinates: multiCoordinates };
            return { type: "Feature", properties: { mun_code: props["mun_code"] }, geometry: newGeom };
        }
        return f; // fallback
    });
    const outlineFC: FeatureCollection = { type: "FeatureCollection", features: outlineFeatures };
    municipalityOutlineLayer.value = L.geoJSON(outlineFC, {
        style: (f) => styleMunicipalityOutline(f),
    }).addTo(map);
}

/**
 * Initialize component
 */
onMounted(async () => {
    await loadConditions();
    await loadGeoJSON();
    await fetchCounts(selectedCondition.value);
    updateVisualization();
});

// Watch for municipality counts changes to re-render
watch(municipalityCounts, () => {
    updateVisualization();
});
</script>

<style scoped>
.leaflet-container {
    height: 100%;
    width: 100%;
    z-index: 0;
}

/* Custom tooltip styling */
:deep(.province-tooltip) {
    background: white;
    padding: 8px 12px;
    border-radius: 4px;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
    font-size: 13px;
    max-width: 250px;
}
</style>
