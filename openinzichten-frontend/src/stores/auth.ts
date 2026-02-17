import { defineStore } from "pinia";
import { BACKEND_URL, USER_ADMIN_ROLE } from "./config";
import type {
    DecodedJWT,
    RegisterPayload,
    CompleteProfilePayload,
    CompleteProfileResponse,
} from "@/utils/User";
import { ref, watch } from "vue";
import type { AuthOrErrorResponse } from "@/utils/Auth";
import type { ErrorResponse } from "@/utils/Error";
import { apiFetch } from "@/utils/Api";

async function checkIfUsernameOrEmailExists(username: string, email: string): Promise<boolean> {
    const response = await apiFetch(BACKEND_URL + "/auth/check-info", {
        method: "POST",
        headers: {
            "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, email }),
    });

    if (response.status === 200) {
        const data = await response.json();
        return data;
    }

    return false;
}

export const useAuthStore = defineStore("auth", () => {
    const isUserLoggedInRef = ref(localStorage.getItem("token") !== null);

    const tokenRef = ref<string | null>(localStorage.getItem("token"));

    watch(tokenRef, (newVal) => {
        if (newVal === null) {
            localStorage.removeItem("token");
        } else {
            localStorage.setItem("token", newVal);
        }
    });

    watch(isUserLoggedInRef, (newValue) => {
        if (!newValue) {
            localStorage.removeItem("token");
        }
    });

    async function register(register: RegisterPayload): Promise<AuthOrErrorResponse> {
        const registrationResponse = await fetch(BACKEND_URL + "/auth/register", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify(register),
        });

        if (registrationResponse.status !== 201) {
            switch (registrationResponse.status) {
                case 409:
                    return {
                        error: "Een gebruiker met deze gegevens bestaat al.",
                    };
                case 400:
                    return {
                        error: (await registrationResponse.json()).message,
                    };
                default:
                    return {
                        error: "Er is iets misgegaan bij het registreren. Probeer het later opnieuw.",
                    };
            }
        }

        const data = (await registrationResponse.json()) as AuthOrErrorResponse;
        if (data?.token) {
            tokenRef.value = data.token as string;
            isUserLoggedInRef.value = true;
        }

        return data;
    }

    async function login(email: string, password: string, rememberMe: boolean): Promise<AuthOrErrorResponse> {
        const loginResponse = await fetch(BACKEND_URL + "/auth/login", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ email, password, rememberMe }),
        });

        if (loginResponse.status !== 200) {
            if (loginResponse.status === 403) {
                const json = await loginResponse.json();
                return { error: json.message ?? "Account is disabled.", disabled: true } as any;
            }

            switch (loginResponse.status) {
                case 401:
                    return {
                        error: "Ongeldige inloggegevens.",
                    };
                case 400:
                    return {
                        error: (await loginResponse.json()).message,
                    };
                case 404:
                    return {
                        error: "Gebruiker niet gevonden.",
                    };
                default:
                    return {
                        error: "Er is iets misgegaan bij het inloggen. Probeer het later opnieuw.",
                    };
            }
        }

        const data = (await loginResponse.json()) as AuthOrErrorResponse;
        if (data?.token) {
            tokenRef.value = data.token as string;
            isUserLoggedInRef.value = true;
        }

        return data;
    }

    function parseJwt(token: string): DecodedJWT {
        const base64Url = token.split(".")[1]!;
        const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
        const jsonPayload = decodeURIComponent(
            atob(base64)
                .split("")
                .map(function (c) {
                    return "%" + ("00" + (c.codePointAt(0) ?? 0).toString(16)).slice(-2);
                })
                .join(""),
        );

        return JSON.parse(jsonPayload) as DecodedJWT;
    }

    function getCurrentUser(): DecodedJWT | null {
        const token = tokenRef.value ?? localStorage.getItem("token");
        if (!token) return null;

        return parseJwt(token);
    }

    function reset() {
        isUserLoggedInRef.value = false;
        tokenRef.value = null;
    }

    function setToken(newToken: string | null) {
        tokenRef.value = newToken;
        isUserLoggedInRef.value = Boolean(newToken);
    }

    function logout() {
        reset();
    }

    async function sendResetUrl(email: string) {
        const resetResponse = await apiFetch(BACKEND_URL + "/auth/password-reset", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ email }),
        });

        if (resetResponse.status !== 200) {
            switch (resetResponse.status) {
                case 400:
                    return {
                        error: (await resetResponse.json()).message,
                    };
                case 404:
                    return {
                        error: "Gebruiker niet gevonden.",
                    };
                default:
                    return {
                        error: "Er is iets misgegaan bij het opnieuw instellen van het wachtwoord. Probeer het later opnieuw.",
                    };
            }
        }

        return { success: true, message: "Resetlink verzonden. Controleer je e-mail." } as const;
    }

    async function resetPassword(token: string, newPassword: string) {
        const resp = await apiFetch(BACKEND_URL + "/auth/password-reset/confirm", {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            body: JSON.stringify({ token, newPassword }),
        });

        if (resp.status !== 200 && resp.status !== 204) {
            switch (resp.status) {
                case 400:
                    return { error: (await resp.json()).message };
                case 404:
                    return { error: "Gebruiker niet gevonden." };
                case 410:
                    return { error: "De resetlink is verlopen of ongeldig." };
                default:
                    return {
                        error: "Er is iets misgegaan bij het wijzigen van het wachtwoord. Probeer het later opnieuw.",
                    };
            }
        }

        return { success: true, message: "Wachtwoord succesvol gewijzigd." } as const;
    }

    async function completeProfile(
        payload: CompleteProfilePayload,
    ): Promise<CompleteProfileResponse | ErrorResponse> {
        const token = localStorage.getItem("token");
        if (!token) {
            return { error: "Niet ingelogd. Token ontbreekt." };
        }

        const response = await apiFetch(BACKEND_URL + "/auth/complete", {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
                Authorization: `Bearer ${token}`,
            },
            body: JSON.stringify(payload),
        });

        if (response.status !== 200) {
            switch (response.status) {
                case 400:
                    return { error: (await response.json()).message };
                case 401:
                    return { error: "Niet geautoriseerd. Log opnieuw in." };
                default:
                    return { error: "Profiel aanvullen is mislukt. Probeer later opnieuw." };
            }
        }

        return (await response.json()) as CompleteProfileResponse;
    }

    function isUserAdmin() {
        const user = getCurrentUser();

        return user?.role.includes(USER_ADMIN_ROLE) || false;
    }

    return {
        register,
        completeProfile,
        login,
        sendResetUrl,
        resetPassword,
        isUserLoggedInRef,
        getCurrentUser,
        reset,
        isUserAdmin,
        checkIfUsernameOrEmailExists,
        logout,
        setToken,
    };
});
