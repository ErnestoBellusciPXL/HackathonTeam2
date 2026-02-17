import type { Feature } from "geojson";

export function normalizeString(s: string): string {
    return String(s)
        .toLowerCase()
        .replace(/[^a-z0-9]/g, "");
}

function getProperty(feature: Feature, candidates: string[]): string | null {
    const props = (feature.properties ?? {}) as Record<string, unknown>;

    for (const key of candidates) {
        const val = props[key];
        if (!val) continue;

        let str: string;
        if (Array.isArray(val)) {
            str = String(val[0] ?? "");
        } else if (typeof val === "string") {
            str = val;
        } else if (typeof val === "number") {
            str = String(val);
        } else {
            continue; // Skip objects
        }

        if (str?.trim()) return str.trim();
    }

    return null;
}

export function getMunicipalityCode(feature: Feature): string | null {
    return getProperty(feature, [
        "mun_code",
        "municipalityCode",
        "municipality_code",
        "code",
        "smun_code",
        "nis",
        "niscode",
    ]);
}

export function getMunicipalityName(feature: Feature): string {
    return (
        getProperty(feature, [
            "mun_name_nl",
            "mun_name",
            "name",
            "naam",
            "naam_nl",
            "smun_name_nl",
            "municipality",
        ]) ?? "Unknown"
    );
}

export function getProvinceName(feature: Feature): string | null {
    return getProperty(feature, [
        "prov_name_nl",
        "province",
        "provincie",
        "prov_name",
        "provincienaam",
        "provincienaam_nl",
    ]);
}

export function isFlemishFeature(feature: Feature): boolean {
    const provinceName = getProvinceName(feature);
    if (!provinceName) return false;

    const normalized = normalizeString(provinceName);
    const flemishProvinces = [
        "antwerpen",
        "limburg",
        "oostvlaanderen",
        "vlaamsbrabant",
        "westvlaanderen",
        "provincieantwerpen",
        "provincielimburg",
        "provincieoostvlaanderen",
        "provincievlaamsbrabant",
        "provinciewestvlaanderen",
        "vlaanderen",
    ];

    return flemishProvinces.some((fp) => normalized.includes(fp));
}

export function matchCountsToFeatures(
    municipalityCounts: Record<string, number>,
    features: Feature[],
): Map<string, number> {
    const matched = new Map<string, number>();

    for (const feature of features) {
        const munCode = getMunicipalityCode(feature);
        if (!munCode) continue;

        // Try exact match, normalized (no leading zeros), or padded
        const count =
            municipalityCounts[munCode] ??
            municipalityCounts[munCode.replace(/^0+/, "")] ??
            municipalityCounts[munCode.padStart(5, "0")];

        if (count !== undefined) {
            matched.set(munCode, count);
        }
    }

    return matched;
}
