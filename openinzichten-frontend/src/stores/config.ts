const LOCAL_BACKEND_URL = "http://localhost:8080/api";
const PRODUCTION_BACKEND_URL = "https://openinzichten.be/api"; // Not Final, do not use

const IS_PROD = false;

const OVERRIDE_BACKEND_URL = import.meta.env.VITE_BACKEND_URL as string | undefined;

export const USER_ADMIN_ROLE = "Admin";

// Timeout for AI fetch requests in milliseconds
export const AI_FETCH_TIMEOUT_MS = 20_000;

// How many AI requests are allowed per AI_RATE_LIMIT_TIME
export const AI_RATE_LIMIT_PER_HOUR = 3;

// 1 hour in milliseconds
export const AI_RATE_LIMIT_TIME = 60 * 60 * 1000;

export const STORY_GENERATION_KEY = "STORY_GENERATION_DATES";

export const BACKEND_URL = OVERRIDE_BACKEND_URL ?? (IS_PROD ? PRODUCTION_BACKEND_URL : LOCAL_BACKEND_URL);
