import { createApp } from "vue";
import { createPinia } from "pinia";
import "./assets/index.css";
import "leaflet/dist/leaflet.css";

import App from "./App.vue";
import router from "./router";
import { useAccountStatusStore } from "@/stores/accountStatus";

const app = createApp(App);

app.use(createPinia());
app.use(router);

// If a token exists on startup, validate account status (disabled check).
try {
    if (localStorage.getItem("token")) {
        const accountStatus = useAccountStatusStore();
        // Run check but don't block startup; if disabled, redirect.
        void (async () => {
            try {
                await accountStatus.checkMe({ suppressToast: true });
                if (accountStatus.disabled) {
                    router.replace({ name: "disabled", query: { msg: accountStatus.disabledReason ?? "Je account is geblokkeerd." } });
                }
            } catch (e) {
                // eslint-disable-next-line no-console
                console.debug("accountStatus startup check failed:", e);
            }
        })();
    }
} catch (e) {
    // eslint-disable-next-line no-console
    console.debug(e);
}

// fix default icon paths (Vite + Leaflet)
import * as L from "leaflet";
// avoid `any` by treating the prototype as an indexable object with optional property
const iconProto = L.Icon.Default.prototype as unknown as { _getIconUrl?: unknown };
delete iconProto._getIconUrl;
L.Icon.Default.mergeOptions({
    iconRetinaUrl: new URL("leaflet/dist/images/marker-icon-2x.png", import.meta.url).href,
    iconUrl: new URL("leaflet/dist/images/marker-icon.png", import.meta.url).href,
    shadowUrl: new URL("leaflet/dist/images/marker-shadow.png", import.meta.url).href,
});

app.mount("#app");
