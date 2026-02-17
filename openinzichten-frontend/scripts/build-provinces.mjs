#!/usr/bin/env node
import fs from 'fs/promises';
import path from 'path';
import union from '@turf/union';

function normalizeKey(s) {
  if (!s) return '';
  return String(s).toLowerCase().trim().split(/[^a-z0-9]/g).join('').replace(/^0+/, '');
}

const inPath = path.resolve('src/resources/georef-belgium-submunicipality.geojson');
const outPath = path.resolve('src/resources/georef-belgium-provinces.geojson');

(async () => {
  try {
    const raw = await fs.readFile(inPath, 'utf8');
    const geo = JSON.parse(raw);
    const features = geo.features || [];
    const groups = new Map();

    for (const f of features) {
      const props = f.properties || {};
      let prov = null;
      const candidates = ['prov_name_nl', 'provincie', 'province', 'prov_name', 'provincienaam', 'province_nl', 'PROVINCIE'];
      for (const k of candidates) {
        if (props[k]) { prov = String(props[k]); break; }
      }
      if (!prov) {
        for (const v of Object.values(props)) {
          if (!v) continue;
          const s = String(Array.isArray(v) ? v[0] : v).toLowerCase();
          const test = s.replace(/[^a-z0-9]/g, '');
          const known = ['antwerpen','limburg','oostvlaanderen','westvlaanderen','vlaamsbrabant','vlaanderen'];
          for (const k of known) {
            if (test.includes(k)) { prov = String(v); break; }
          }
          if (prov) break;
        }
      }
      if (!prov) prov = 'unknown';
      const key = normalizeKey(prov);
      if (!groups.has(key)) groups.set(key, { name: prov, features: [] });
      groups.get(key).features.push(f);
    }

    const outFeatures = [];
    for (const [key, entry] of groups.entries()) {
      const { name, features: feats } = entry;
      let merged = null;
      for (const f of feats) {
        try {
          if (!merged) merged = f;
          else merged = union(merged, f);
        } catch (e) {
          console.warn('union failed for province', name, e && e.message ? e.message : e);
          if (!merged) merged = f;
          else {
            // best-effort fallback: assemble MultiPolygon coordinates
            const coords = [];
            if (merged.geometry.type === 'MultiPolygon') coords.push(...merged.geometry.coordinates);
            else if (merged.geometry.type === 'Polygon') coords.push(merged.geometry.coordinates);
            if (f.geometry.type === 'MultiPolygon') coords.push(...f.geometry.coordinates);
            else if (f.geometry.type === 'Polygon') coords.push(f.geometry.coordinates);
            merged.geometry = { type: 'MultiPolygon', coordinates: coords };
          }
        }
      }
      if (!merged) continue;
      outFeatures.push({ type: 'Feature', properties: { province: entry.name, province_key: key }, geometry: merged.geometry });
    }

    const out = { type: 'FeatureCollection', features: outFeatures };
    await fs.writeFile(outPath, JSON.stringify(out, null, 2), 'utf8');
    console.log('Wrote provinces GeoJSON to', outPath);
  } catch (err) {
    console.error('Failed to build provinces:', err);
    process.exit(1);
  }
})();
