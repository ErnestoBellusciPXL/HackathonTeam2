import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth";
import { useAccountStatusStore } from "@/stores/accountStatus";

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    // Always scroll to top when navigating to a new route
    scrollBehavior() {
        return { left: 0, top: 0 };
    },
    routes: [
        {
            path: "/",
            name: "home",
            component: () => import("../views/HomeView.vue"),
            // component: () => import("../views/HomeView.vue"),
        },
        {
            path: "/error",
            name: "error",
            component: () => import("../views/ErrorView.vue"),
        },
        {
            path: "/about",
            name: "about",
            component: () => import("../views/AboutView.vue"),
        },
        {
            path: "/register",
            name: "register",
            component: () => import("../views/RegisterView.vue"),
        },
        {
            path: "/login",
            name: "login",
            component: () => import("../views/LoginView.vue"),
        },
        {
            path: "/settings",
            name: "settings",
            component: () => import("../views/SettingsView.vue"),
        },
        {
            path: "/my-story",
            name: "my-story",
            component: () => import("../views/MyStoryView.vue"),
        },
        {
            path: "/terms",
            name: "terms",
            component: () => import("../views/TermsView.vue"),
        },
        {
            path: "/privacy",
            name: "privacy",
            component: () => import("../views/PrivacyView.vue"),
        },
        {
            path: "/forgot-password",
            name: "forgot-password",
            component: () => import("../views/ForgotPassword.vue"),
        },
        {
            path: "/disabled",
            name: "disabled",
            component: () => import("../views/DisabledView.vue"),
        },
        {
            path: "/reset-password",
            name: "reset-password",
            component: () => import("../views/ResetPassword.vue"),
        },
        {
            path: "/admin",
            name: "admin",
            component: () => import("../views/AdminDashboardView.vue"),
        },
        {
            path: "/admin/tickets/:id",
            name: "admin-ticket-detail",
            component: () => import("../views/TicketDetailView.vue"),
        },
        {
            path: "/admin/users/:id",
            name: "admin-user-detail",
            component: () => import("../views/AdminUserDetailView.vue"),
        },
        {
            path: "/write",
            name: "write",
            component: () => import("../views/WriteStoryView.vue"),
        },
        {
            path: "/story/:id",
            name: "story-detail",
            component: () => import("../views/StoryDetailView.vue"),
        },
        {
            path: "/stories",
            name: "stories",
            component: () => import("../views/StoryView.vue"),
        },
        {
            path: "/connections",
            name: "connections",
            component: () => import("../views/ConnectionsView.vue"),
        },
        {
            path: "/heatmap",
            name: "heatmap",
            component: () => import("../views/HeatMapView.vue"),
        },
        // Catch-all route to redirect to error page for unknown paths
        {
            path: "/:pathMatch(.*)*",
            redirect: { name: "error" },
        },
    ],
});

const publicRouteNames = new Set([
    "home",
    "login",
    "register",
    "about",
    "terms",
    "privacy",
    "code-of-conduct",
    "forgot-password",
    "reset-password",
    "disabled",
    "story-detail",
    "stories",
    "heatmap",
    "error",
]);
const adminRouteNames = new Set(["admin", "admin-ticket-detail"]);
adminRouteNames.add("admin-user-detail");

router.beforeEach(async (to, from, next) => {
    const routeName = String(to.name ?? "");
    const authRequired = !publicRouteNames.has(routeName);
    const authStore = useAuthStore();
    const accountStatus = useAccountStatusStore();

    // If user already expired according to token, reset and go home
    const user = authStore.getCurrentUser();
    if (user && user?.exp * 1000 < Date.now()) {
        authStore.reset();
        localStorage.clear();
        return next({ name: "home" });
    }

    // Check backend /auth/me for account disabled status on every route,
    // but avoid checking when already navigating to the disabled page.
    try {
        if (routeName !== "disabled") {
            if (localStorage.getItem("token")) {
                await accountStatus.checkMe({ suppressToast: true });
                if (accountStatus.disabled) {
                    return next({ name: "disabled", query: { msg: accountStatus.disabledReason ?? "Je account is geblokkeerd." } });
                }
            }
        }
    } catch (e) {
        // log debug but don't block navigation
        // eslint-disable-next-line no-console
        console.debug("accountStatus.checkMe error:", e);
    }

    if (authRequired && !authStore.getCurrentUser()) {
        return next({ name: "login" });
    }

    if (adminRouteNames.has(routeName) && !authStore.isUserAdmin()) {
        return next({ name: "home" });
    }

    next();
});

export default router;
