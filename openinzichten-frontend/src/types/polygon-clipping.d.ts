declare module "polygon-clipping" {
    // Simplified type declaration matching usage (Polygon or MultiPolygon coordinate arrays)
    export function union(...polygons: (number[][][] | number[][][][])[]): number[][][][];
}
