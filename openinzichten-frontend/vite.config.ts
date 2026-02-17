import { fileURLToPath, URL } from "node:url";

import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";
import vueDevTools from "vite-plugin-vue-devtools";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
    plugins: [vue(), tailwindcss(), vueDevTools()],
    // Provide `global` in the browser environment for some older libs (e.g. sockjs-client)
    define: {
        global: 'window',
    },
    server: {
        host: "0.0.0.0", // Expose to all network interfaces for Docker
        port: 5173,
        proxy: {
            "/api": {
                target: "http://backend:8080",
                changeOrigin: true,
            },
        },
        // Prevent the Vite dev server from reacting to macOS metadata files
        watch: {
            usePolling: true, // Required for Docker on Windows/Mac
            // ignored accepts a string or array of glob/regex patterns
            ignored: ["**/._*", "._*", "**/.DS_Store", ".DS_Store"],
        },
    },
    resolve: {
        alias: {
            "@": fileURLToPath(new URL("./src", import.meta.url)),
        },
    },
});
