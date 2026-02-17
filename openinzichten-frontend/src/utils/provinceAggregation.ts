import type { FeatureCollection, Feature } from "geojson";
import {
    getMunicipalityCode,
    normalizeString,
    isFlemishFeature,
    getMunicipalityName,
    getProvinceName,
} from "./geoDataMatcher";

export interface ProvinceData {
    name: string;
    key: string;
    count: number;
    municipalities: Array<{ code: string; name: string; count: number }>;
}

export function aggregateByProvince(
    municipalityFeatures: Feature[],
    municipalityCounts: Map<string, number>,
): Map<string, ProvinceData> {
    const provinceData = new Map<string, ProvinceData>();
    const flemishFeatures = municipalityFeatures.filter(isFlemishFeature);

    for (const feature of flemishFeatures) {
        const provinceName = getProvinceName(feature) ?? "Onbekend";
        const provinceKey = normalizeString(provinceName);
        const munCode = getMunicipalityCode(feature);
        const munName = getMunicipalityName(feature);
        const count = munCode ? (municipalityCounts.get(munCode) ?? 0) : 0;

        if (!provinceData.has(provinceKey)) {
            provinceData.set(provinceKey, {
                name: provinceName,
                key: provinceKey,
                count: 0,
                municipalities: [],
            });
        }

        const province = provinceData.get(provinceKey)!;
        province.count += count;

        if (munCode) {
            province.municipalities.push({ code: munCode, name: munName, count });
        }
    }

    return provinceData;
}

export function getFlemishProvinces(provincesGeoJSON: FeatureCollection): FeatureCollection {
    return {
        type: "FeatureCollection",
        features: provincesGeoJSON.features.filter(isFlemishFeature),
    };
}

export function getColor(value: number, max: number): string {
    // Interpolate between lightest and darkest colors
    const lightHex = LIGHT_HEX; // lightest
    const darkHex = DARK_HEX; // darkest

    const clamp = (n: number, min: number, max: number) => Math.max(min, Math.min(max, n));
    const ratio = max > 0 ? clamp(value / max, 0, 1) : 0;

    const hexToRgb = (hex: string) => {
        const m = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
        return m
            ? {
                  r: Number.parseInt(m[1]!, 16),
                  g: Number.parseInt(m[2]!, 16),
                  b: Number.parseInt(m[3]!, 16),
              }
            : { r: 0, g: 0, b: 0 };
    };

    const rgbToHex = (r: number, g: number, b: number) => {
        const toHex = (x: number) => x.toString(16).padStart(2, "0");
        return `#${toHex(r)}${toHex(g)}${toHex(b)}`.toUpperCase();
    };

    const lerp = (a: number, b: number, t: number) => Math.round(a + (b - a) * t);

    const c0 = hexToRgb(lightHex);
    const c1 = hexToRgb(darkHex);
    const r = lerp(c0.r, c1.r, ratio);
    const g = lerp(c0.g, c1.g, ratio);
    const b = lerp(c0.b, c1.b, ratio);
    return rgbToHex(r, g, b);
}

// Export the gradient endpoints so other components (legend) can reuse them
export const LIGHT_HEX = "#32DCEB"; // lightest
export const DARK_HEX = "#4D3DB6"; // darkest

export function computeLegendBuckets(provinceCounts: Map<string, ProvinceData>) {
    const counts = Array.from(provinceCounts.values()).map((p) => p.count);
    const max = Math.max(...counts, 0);

    if (max === 0) {
        return [{ color: getColor(0, 1), label: "Geen data", min: 0, max: 0 }];
    }

    const breaks = [0, 0.2, 0.4, 0.6, 0.8, 1].map((r) => Math.ceil(max * r));
    const unique = [...new Set(breaks)].sort((a, b) => a - b);

    return unique.slice(0, -1).map((min, i) => ({
        color: getColor(min, unique[unique.length - 1] ?? 1),
        label: i === unique.length - 2 ? `${min}+` : `${min}-${unique[i + 1]}`,
        min,
        max: unique[i + 1] ?? min,
    }));
}

/**
 * Compute legend buckets from a raw array of counts (numbers).
 * This is useful when the visualization colors individual municipalities
 * instead of aggregated provinces. The function always includes a zero
 * bucket (if applicable) and then divides the non-zero range into up to
 * 4 additional buckets.
 */
export function computeLegendBucketsFromCounts(values: number[]) {
    // Normalize input and floor values
    const counts = values.filter((v) => Number.isFinite(v)).map((v) => Math.max(0, Math.floor(v)));

    // Consider only positive counts for bucket generation. If there are no
    // positive counts, return a single zero bucket to indicate no data.
    const positiveCounts = counts.filter((c) => c > 0);
    if (positiveCounts.length === 0) {
        return [{ color: getColor(0, 1), label: "0", min: 0, max: 0 }];
    }

    const max = Math.max(...positiveCounts);
    const overallMax = Math.max(1, max);

    // For small maximums, create discrete buckets starting at 1 (do not
    // include a '0' bucket) so single-story values are clearly shown.
    if (overallMax <= 4) {
        const mins = Array.from({ length: overallMax }, (_, i) => i + 1);
        return mins.map((min, idx) => {
            const isLast = idx === mins.length - 1;
            const label = isLast ? `${min}+` : `${min}`;
            return {
                color: getColor(min, overallMax),
                label,
                min,
                max: isLast ? overallMax : mins[idx + 1],
            };
        });
    }

    // For larger ranges, build quartile-like breaks starting at 1.
    const rawBreaks = [
        1,
        Math.ceil(overallMax * 0.25),
        Math.ceil(overallMax * 0.5),
        Math.ceil(overallMax * 0.75),
        overallMax,
    ];
    const unique = Array.from(new Set(rawBreaks)).sort((a, b) => a - b);

    return unique.slice(0, -1).map((min, i) => {
        const next = unique[i + 1] ?? min;
        const label = i === unique.length - 2 ? `${min}+` : `${min}-${next}`;
        return {
            color: getColor(min, overallMax),
            label,
            min,
            max: next,
        };
    });
}
