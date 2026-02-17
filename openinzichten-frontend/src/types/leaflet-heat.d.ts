/* Minimal TypeScript declarations for leaflet.heat
   This augments the Leaflet module with a heatLayer factory.
*/
import * as L from "leaflet";

declare module "leaflet" {
    namespace heatLayer {
        interface HeatOptions extends L.LayerOptions {
            radius?: number;
            blur?: number;
            maxZoom?: number;
            max?: number;
            gradient?: { [key: number]: string };
        }
    }

    // points: Array<[lat, lng, intensity]>
    function heatLayer(
        points: Array<[number, number, number]> | Array<unknown>,
        options?: heatLayer.HeatOptions,
    ): L.Layer;
}

export {};
declare module "leaflet.heat" {
    import * as L from "leaflet";
    // heatLayer is attached on L as `L.heatLayer` by the library, but we provide a minimal declaration
    export function heatLayer(
        points: Array<[number, number, number]>,
        options?: Record<string, unknown>,
    ): L.Layer;
    const _default: { heatLayer: typeof heatLayer };
    export default _default;
}
