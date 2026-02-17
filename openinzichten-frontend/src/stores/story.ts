import { defineStore } from "pinia";
import {
    AI_FETCH_TIMEOUT_MS,
    AI_RATE_LIMIT_PER_HOUR,
    AI_RATE_LIMIT_TIME,
    BACKEND_URL,
    STORY_GENERATION_KEY,
} from "./config";
import type {
    ReformatStoryRequestDate,
    ReformattedStoryPayload,
    StoryOrderBy,
    StoryPayload,
    QuestionAnswer,
    StoryValidationResponse,
} from "@/utils/Story";
import type { ResponseState, SuccessOrError } from "@/utils/Error";
import { apiFetch } from "@/utils/Api";
import fetchTimeout from "@/utils/FetchTimeout";

export const useStoryStore = defineStore("story", {
    actions: {
        async saveStory(
            payload: StoryPayload,
            endpoint: string,
            method: "POST" | "PUT",
            unauthorizedError: string,
            networkError: string,
        ): Promise<SuccessOrError> {
            const token = localStorage.getItem("token");
            if (!token) {
                return {
                    success: false,
                    error: unauthorizedError,
                };
            }
            try {
                const resp = await apiFetch(`${BACKEND_URL}${endpoint}`, {
                    method,
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify(payload),
                });
                if (resp.status === 201 || resp.status === 200) {
                    return { success: true };
                }

                return {
                    success: false,
                    error: "Verhaal moet minimum 30 en maximaal 10 000 karakters lang zijn",
                };
            } catch {
                return {
                    success: false,
                    error: networkError,
                };
            }
        },

        async publishStory(payload: StoryPayload): Promise<SuccessOrError> {
            return this.saveStory(
                payload,
                "/stories",
                "POST",
                "Je moet ingelogd zijn om een verhaal te publiceren.",
                "Er is iets misgegaan bij het publiceren. Probeer later opnieuw.",
            );
        },

        async updateStory(payload: StoryPayload): Promise<SuccessOrError> {
            return this.saveStory(
                payload,
                "/stories/update-my-story",
                "PUT",
                "Je moet ingelogd zijn om een verhaal te bewerken.",
                "Er is iets misgegaan bij het bijwerken. Probeer later opnieuw.",
            );
        },

        async deleteStory(storyId: string): Promise<SuccessOrError> {
            const token = localStorage.getItem("token");
            if (!token) {
                return {
                    success: false,
                    error: "Je moet ingelogd zijn om een verhaal te verwijderen.",
                };
            }
            if (!storyId) {
                return {
                    success: false,
                    error: "Geen verhaal-id gevonden.",
                };
            }

            try {
                const resp = await apiFetch(`${BACKEND_URL}/stories/${encodeURIComponent(storyId)}`, {
                    method: "DELETE",
                    headers: {
                        Authorization: `Bearer ${token}`,
                    },
                });

                if (resp.status === 204 || resp.status === 200) {
                    return { success: true };
                }

                if (resp.status === 404) {
                    return { success: false, error: "Verhaal niet gevonden." };
                }

                return {
                    success: false,
                    error: `Verwijderen mislukt (status ${resp.status}).`,
                };
            } catch (error) {
                console.error("Failed to delete story:", error);
                return {
                    success: false,
                    error: "Er is iets misgegaan bij het verwijderen. Probeer later opnieuw.",
                };
            }
        },

        async fetchStories(
            page: number = 0,
            size: number = 25,
            query: string = "",
            condition?: string,
            orderBy?: StoryOrderBy,
        ): Promise<StoryPayload[]> {
            try {
                const token = localStorage.getItem("token");
                const headers: Record<string, string> = {
                    "Content-Type": "application/json",
                };
                if (token) {
                    headers.Authorization = `Bearer ${token}`;
                }

                const params = new URLSearchParams();
                params.set("page", String(page));
                params.set("size", String(size));
                params.set("query", query ?? "");
                if (condition && condition !== "all") {
                    params.set("condition", condition);
                }

                if (orderBy) {
                    params.set("orderBy", orderBy);
                }

                const resp = await apiFetch(`${BACKEND_URL}/stories?${params.toString()}`, {
                    method: "GET",
                    headers,
                });
                if (resp.status === 200) {
                    const data = (await resp.json()) as StoryPayload[];
                    return data;
                }
                return [];
            } catch {
                return [];
            }
        },

        async fetchRecommendedStories(): Promise<StoryPayload[]> {
            try {
                const token = localStorage.getItem("token");
                const headers: Record<string, string> = {
                    "Content-Type": "application/json",
                };
                if (token) {
                    headers.Authorization = `Bearer ${token}`;
                }

                const resp = await apiFetch(`${BACKEND_URL}/stories/recommended`, {
                    method: "GET",
                    headers,
                });

                if (resp.status === 200) {
                    const data = (await resp.json()) as StoryPayload[];
                    return data;
                }
                return [];
            } catch (error) {
                console.error("Failed to fetch recommended stories:", error);
                return [];
            }
        },

        async getAIImprovedStory(originalStory: string): Promise<ResponseState<ReformattedStoryPayload>> {
            const token = localStorage.getItem("token");

            if (!token) {
                return {
                    state: "failed",
                    errorResponse: { error: "Je moet ingelogd zijn om AI-verhaalverbetering te gebruiken." },
                };
            }

            let generations: ReformatStoryRequestDate[];

            if (localStorage.getItem(STORY_GENERATION_KEY)) {
                generations = JSON.parse(
                    localStorage.getItem(STORY_GENERATION_KEY)!,
                ) as ReformatStoryRequestDate[];
            } else {
                generations = [];
            }

            // Order generationDates to have the most recent first
            generations.sort((a, b) => new Date(b.formattedOn).getTime() - new Date(a.formattedOn).getTime());

            // Check if the user has made 3 or more requests in the last hour
            const oneHourAgo = new Date(Date.now() - AI_RATE_LIMIT_TIME);
            const recentRequests = generations.filter((date) => new Date(date.formattedOn) > oneHourAgo);

            // Delete older requests to keep the list clean
            generations = recentRequests;
            localStorage.setItem(STORY_GENERATION_KEY, JSON.stringify(generations));

            if (recentRequests.length >= AI_RATE_LIMIT_PER_HOUR) {
                return {
                    state: "failed",
                    errorResponse: {
                        error: "Je hebt het maximum aantal AI-verhaalverbeteringen per uur bereikt. Probeer later opnieuw.",
                    },
                };
            }

            try {
                const resp = await fetchTimeout(`${BACKEND_URL}/stories/ai/reformat`, AI_FETCH_TIMEOUT_MS, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({ storyContent: originalStory }),
                });

                if (resp.status === 200) {
                    const data: ReformattedStoryPayload = await resp.json();

                    generations.push({ formattedOn: new Date() });
                    localStorage.setItem(STORY_GENERATION_KEY, JSON.stringify(generations));

                    return {
                        state: "success",
                        response: data,
                    };
                }

                if (resp.status === 429) {
                    return {
                        state: "failed",
                        errorResponse: {
                            error: "Je hebt het maximum aantal AI-verhaalverbeteringen per uur bereikt. Probeer later opnieuw.",
                        },
                    };
                }

                return {
                    state: "failed",
                    errorResponse: {
                        error: "Er is iets misgegaan bij het ophalen van de verbeterde versie.",
                    },
                };
            } catch (e: Error | unknown) {
                console.log(e);
                return {
                    state: "failed",
                    errorResponse: {
                        error: e instanceof Error ? e.message : "Onbekende fout bij AI-verhaalverbetering.",
                    },
                };
            }
        },


        async generateStory(inputs: QuestionAnswer[]): Promise<ResponseState<ReformattedStoryPayload>> {
            const token = localStorage.getItem("token");
            if (!token) {
                return {
                    state: "failed",
                    errorResponse: { error: "Je moet ingelogd zijn om een verhaal te genereren." },
                };
            }

            try {
                // Reuse timeout logic or rate limiting if needed, but for now direct call
                // Note: The backend has rate limiting @RateLimit(requests = 5, perSeconds = 3600)
                const resp = await fetchTimeout(`${BACKEND_URL}/stories/ai/generate`, AI_FETCH_TIMEOUT_MS, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({ inputs }),
                });

                if (resp.status === 200) {
                    const data: ReformattedStoryPayload = await resp.json();
                    return {
                        state: "success",
                        response: data,
                    };
                }

                if (resp.status === 429) {
                    return {
                        state: "failed",
                        errorResponse: {
                            error: "Je hebt het maximum aantal AI-verzoeken per uur bereikt. Probeer later opnieuw.",
                        },
                    };
                }

                return {
                    state: "failed",
                    errorResponse: {
                        error: "Er is iets misgegaan bij het genereren van het verhaal.",
                    },
                };
            } catch (e: Error | unknown) {
                console.log(e);
                return {
                    state: "failed",
                    errorResponse: {
                        error: e instanceof Error ? e.message : "Onbekende fout bij AI-verhaalgeneratie.",
                    },
                };
            }
        },


        async validateStory(title: string, content: string): Promise<ResponseState<StoryValidationResponse>> {
            const token = localStorage.getItem("token");
            if (!token) {
                return {
                    state: "failed",
                    errorResponse: { error: "Je moet ingelogd zijn." },
                };
            }

            try {
                const resp = await fetchTimeout(`${BACKEND_URL}/stories/ai/validate`, AI_FETCH_TIMEOUT_MS, {
                    method: "POST",
                    headers: {
                        "Content-Type": "application/json",
                        Authorization: `Bearer ${token}`,
                    },
                    body: JSON.stringify({ title, content }),
                });

                if (resp.status === 200) {
                    const data: StoryValidationResponse = await resp.json();
                    return {
                        state: "success",
                        response: data,
                    };
                }

                if (resp.status === 429) {
                    return {
                        state: "failed",
                        errorResponse: {
                            error: "Je hebt het maximum aantal AI-verzoeken per uur bereikt. Probeer later opnieuw.",
                        },
                    };
                }

                return {
                    state: "failed",
                    errorResponse: {
                        error: "Validatie mislukt.",
                    },
                };
            } catch (e: Error | unknown) {
                console.log(e);
                return {
                    state: "failed",
                    errorResponse: {
                        error: e instanceof Error ? e.message : "Onbekende fout bij validatie.",
                    },
                };
            }
        },
    },
});
