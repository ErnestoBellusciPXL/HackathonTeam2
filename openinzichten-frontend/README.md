# OpenInzicht Frontend

Below is a quick reference for the project layout, the purpose of important files and folders, the stores & utils, and documentation for the main UI components used across the app. If this is your first time working on this project, please read through this document to get acquainted with the structure and conventions used.

## Table Of Contents

- [Project Setup](#project-setup)
- [Used Dependencies](#used-dependencies)
- [Project Structure](#project-structure)
- [Docker](#docker)
- [WireGuard VPN Usage](#wireguard-vpn-usage)
- [Terms](#terms)
- [Pages](#pages)
- [Stores and Utilities](#stores-and-utilities)
- [Components](#components)
- [Tips and Conventions](#tips-and-conventions)

## Project Setup

Make sure you have [Node.js](https://nodejs.org/en/download/) and NPM installed (we recommend the latest LTS version).

Install dependencies:

```sh
npm install
```

Run dev server:

```sh
npm run dev
```

Build for production:

```sh
npm run build
```

Run unit tests:

```sh
npm run test:unit
```

Lint the project:

```sh
npm run lint
```

Format the code:

```sh
npm run format
```

## Used Dependencies

The following dependencies are used in this project, while working on some parts you might have to refer to their documentation:

- [StompJS](https://github.com/stomp-js/stompjs): WebSocket client for real-time communication. Used for our chat.
- [Vue 3](https://vuejs.org/): The progressive JavaScript framework for building user interfaces.
- [TailwindCSS](https://tailwindcss.com/): A utility-first CSS framework for rapid UI development.
- [Pinia](https://pinia.vuejs.org/): State management library for Vue.
- [Leaflet](https://github.com/vue-leaflet/vue-leaflet): Interactive maps library for Vue for our Heatmap.

We recommend checking their documentation when working on related features.

## Project Structure

Top-level files:

- `index.html`: Application HTML entry.
- `package.json`: NPM scripts and dependencies.
- `vite.config.ts`: Vite configuration.
- `tsconfig.json` & related `tsconfig.*.json`: TypeScript configuration for build/tests.
- `README.md`: (this file) project documentation.

Top-level folders of interest:

- `src/`: application source code.
  - `main.ts`: Vue app bootstrap and global registration.
  - `App.vue`: root Vue component and layout shell.
  - `assets/`: global CSS and static assets used by the app.
  - `components/`: reusable Vue components (buttons, inputs, icons, UI primitives).
  - `composables/`: Vue 3 composables (custom hooks).
  - `resources/`: static images or SVGs (brand logos, illustrations).
  - `router/`: Vue Router routes and configuration (`index.ts`).
  - `stores/`: Pinia stores for app state.
  - `utils/`: helper modules, types and API client wrappers, just all-round useful stuff that is not specific to any single feature.
  - `views/`: page-level Vue components (routes).


## Docker

The `docker/` directory contains Docker/Docker-Compose config for local services (SonarQube, nginx, etc.).

Make sure to pass the BACKEND_HOST environment variable to point the frontend to the correct backend API URL when running locally or in production. To see how it is handled, check `./src/stores/config.ts`. This file reads the BACKEND_HOST variable and exposes it to the rest of the application, it also includes other options for the AI fetch timeouts, rate limiting, and user roles.

Other folders:

- `documentatie/`: internal documentation for pipeline and docker.
- `frontend_testing/`: Selenium tests for frontend E2E testing.
- `public/`: static public assets copied as-is to build output.

## Terms

Here is a short glossary of common terms used in this project and what they refer to:

- **Story (verhaal)**: A user-submitted narrative or experience shared on the platform.
- **Condition (aandoening)**: A health condition or circumstance that users can associate with

## WireGuard VPN Usage

To connect to some of the internal resources (SonarQube, testing environment, etc) you need to be using WireGuard tunnel.

### Usage

Before we start, you will need to install the [WireGuard app](https://www.wireguard.com/install/), then follow the steps below

#### Windows (and probably Mac)

1. Open WireGuard app, click on `Add Tunnel` -> `Import tunnel(s) from file...` and select your `wg-openinzicht.conf`
2. Click Activate

You should see two IPs in `Allowed IPs` field.

#### Linux

1. Go to folder with `wg-openinzicht.conf` and use this command:

```bash
sudo wg-quick up ./wg-openinzicht.conf
```

To disable the VPN replace `up` with `down`.

## Pages

This application is organized into route-driven pages (located in `src/views`). Below is a concise description of each major page, what it does, and how pages link together in common user flows.

- `HomeView.vue` — public landing page and entry point after login. Shows featured content, discovery widgets and links to browse stories. From here users can navigate to `StoryView`, `WriteStoryView` (if logged in), `LoginView`/`RegisterView`, and other informational pages.

- `StoryView.vue` — listing page for stories. Displays paginated lists of user-submitted stories with filters, sorting, and search. Each story links to `StoryDetailView` for the full content. Pagination and filters typically use `PaginationControls` and `FilterDropdown` components.

- `StoryDetailView.vue` — single-story page showing the full story, author info, option to connect (add as friend), like, and report. Navigated to from `StoryView` or internal links. If the viewer is the story owner, `StoryDetailView` may expose edit or delete actions which route to or open the `WriteStoryView` in edit mode.

- `WriteStoryView.vue` — page for creating or editing a story. Accessible from a call-to-action in `HomeView`, from `MyStoryView`, or from an edit action on `StoryDetailView`. Requires authentication; unauthenticated users are redirected to `LoginView` and returned to this page after login when applicable. Contains a text editor and an AI function to assist with doing styling and structuring the story content.

- `MyStoryView.vue` — personal area listing a user's own story. Links to `WriteStoryView` for creating or editing entries and to `StoryDetailView` for viewing.

- `LoginView.vue` — authentication page for existing users. Submits credentials via the `auth` store; on success the user is redirected to `HomeView`. Also links to `RegisterView` and `ForgotPassword.vue`.

- `RegisterView.vue` — multi-step registration page. First collects username/email/password and validates them; second asks for profile settings (postcode, conditions) and completes the profile via `auth` store calls. Successful registration sets token in `localStorage` and redirects to `HomeView`.

- `ForgotPassword.vue` & `ResetPassword.vue` — password recovery flow. `ForgotPassword.vue` collects the user's email to send a reset link; `ResetPassword.vue` handles setting a new password using the token from the reset link and then routes users to `LoginView`.

- `SettingsView.vue` — user account settings and preferences. Accessible when logged in; updates profile data via the `account` or `auth` store.

- `ConnectionsView.vue` — user connections/friends management page. Lists current connections and incoming and outgoing connection requests, think of it like a friends list and follow requests. Works with the `connections` store.

- `HeatmapView.vue` — data visualization page showing story submission by geography. Accessible from the main navigation for all users.

- `AdminDashboardView.vue` — admin-only area for moderation and site management. Routes and features here are usually protected by route guards and require the current user to have admin privileges.

- `AdminUserDetailView.vue` — admin page for viewing and managing a specific user's account, stories, and status. Linked from `AdminDashboardView`.

- `TicketDetailView.vue` — admin page for viewing and managing report tickets submitted by users. Linked from `AdminDashboardView`.

- Informational pages: `AboutView.vue`, `PrivacyView.vue`, `TermsView.vue`, `CodeOfConductView.vue` — static content pages with policy and project information. Linked from the site footer and during registration as needed (e.g., `PrivacyView` is shown in the register checkbox link).

- Navigating to an undefined route shows `ErrorView.vue` (404 page).

- If an admin has disabled an user's account, they are redirected to `DisabledView.vue` which explains the situation and provides contact info.

Routing & flow notes

- Authentication flow: `LoginView` and `RegisterView` call into the `auth` store. Upon successful login/register the app persists the JWT token to `localStorage` and sets `isUserLoggedInRef` so protected routes render. Route guards should check the store for authentication and optionally redirect unauthorized users to `LoginView`.

- Story flow: `StoryView` (list) -> `StoryDetailView` (single) -> `WriteStoryView` (edit). `WriteStoryView` and `MyStoryView` are only meaningful for authenticated users.

- Registration: `RegisterView` has a two-step UI (credentials first, then profile settings). After the first step the UI reveals the second step and loads auxiliary data such as `conditions` and zipcode lists from the respective stores. The complete profile call links the newly created account to the profile settings and then navigates to `HomeView`.

- Navigation: `NavigationBar.vue` provides top-level links and reads the `auth` store to switch between auth-related actions (login/register) and user-related actions (logout, settings, my stories). Footer links expose informational pages (privacy, terms, about).

## Stores and Utilities

**Stores (src/stores)**

- `account.ts`: Manages user account data and profile state.
- `accountStatus.ts`: Tracks account status (active, disabled).
- `admin.ts`: Admin-specific state and actions (site moderation, user management).
- `auth.ts`: Authentication state and actions (login, register, token handling, current user). Key responsibilities:
  - Check username/email availability.
  - Perform `register` and `completeProfile` calls.
  - Manage `isUserLoggedInRef` and persist token to `localStorage`.
  - Also handles parsing JWT token to extract user info and the reset password flow.
- `chat.ts`: Manages chat messages, handles everything related to user-to-user chat functionality (including fetching, sending messages, and real-time updates).
- `conditions.ts`: Loads and caches condition data used for suggestions and filtering.
- `config.ts`: Application configuration and feature flags used at runtime.
- `connections.ts`: Manages user connections/friends (send request, accept, remove).
- `story.ts`: High-level story listing state (creating, fetching lists, paging) and also AI story styling.
- `storyDetail.ts`: Single story view state and actions (fetch story, comment, reactions).
- `ticket.ts`: Manages report tickets submitted by users.
- `toast.ts`: Centralized toast/notification management used by UI to show success/error messages.
- `zipcode.ts`: Loads zipcode / municipality data and provides normalized lookup methods.

These stores are implemented using Pinia and encapsulate API calls through the utilities in `src/utils`.

**Utilities (src/utils)**

- `Api.ts`: Low-level API client (request wrapper, handles base URL, headers, token injection and basic error mapping).
- `Auth.ts`: Authentication helpers (token handling, helper functions used by the `auth` store).
- `Condition.ts`: Types and helpers for condition data (normalization, name extraction).
- `ZipCode.ts`: Types and helpers for zipcode/municipality structures and formatting used in UI.
- `Error.ts`: Common error shapes and helpers for showing and mapping API errors.
- `Story.ts`, `User.ts`, `Admin.ts`: Domain helpers and types for stories, users and admin-related data.
- `Debounce.ts`: Generic debounce function for rate-limiting calls (used in search inputs).
- `FetchTimeout.ts`: Helper to wrap fetch calls with a timeout, mostly used in AI related features.

Each util module exposes typed helper functions and interfaces that stores and components import to keep logic centralized and testable.

## Components

This section documents the primary reusable UI components and their common props/usage. Use `script setup` imports in pages/views and pass props directly or with `v-model` where indicated.

- `Button3D.vue`

  - Props: `variant?: 'primary' | 'secondary' | 'ghost'` (default: `primary`), `disabled?: boolean`.
  - Emits: native `click` event.
  - Use: visually prominent button with subtle 3D style. Example: `<Button3D variant="primary" @click="handleSubmit">Volgende</Button3D>`.

- `MovingInput.vue`

  - Props: `id: string`, `label: string`, `modelValue: string`, `type?: string` (default: `text`).
  - Emits: `update:modelValue` for use with `v-model`.
  - Use: labeled input with animation and floating label. Example: `<MovingInput id="email" label="E-mail" v-model="email" />`.

- `SearchDropdown.vue`

  - Props: `items: { value: string; label: string; }[]`, `placeholder?: string`, `modelValue?: string`.
  - Emits: `update:modelValue` (v-model compatible).
  - Use: searchable dropdown/list that displays `label` and returns `value` when selected. Example: `<SearchDropdown v-model="zipcode" :items="zipcodeItems" placeholder="Postcode ..." />`.

- `InfoTooltipButton.vue`

  - Props: `tooltipText: string` (content to show on hover/focus), optional `placement?: 'top'|'right'|'bottom'|'left'`.
  - Use: compact tooltip trigger next to labels for contextual help.

- `GroupSupportImage.vue`

  - Props: none (presentational).
  - Use: decorative illustration used on the right-hand panel of auth pages.

- `NavigationBar.vue`

  - Props: none (reads auth store to show appropriate links).
  - Use: main site navigation, handles login/logout links and route navigation.

- `PaginationControls.vue`

  - Props: `page: number`, `pageCount: number`, `perPage?: number`.
  - Emits: `update:page` or `page-change` event when user navigates pages.
  - Use: place under lists to allow paging of story/user lists.

- `FilterDropdown.vue`

  - Props: `options: { value: string; label: string }[]`, `modelValue?: string | string[]`, `multiple?: boolean`.
  - Emits: `update:modelValue`.
  - Use: generic filter selector used in admin or stories pages.

- `MovingInput`, `SearchDropdown` and `FilterDropdown` follow the `v-model` convention and should be used with `v-model` in `script setup` style.

- `StatsCard.vue`

  - Props: `title: string`, `value: string | number`, `trend?: 'up'|'down'|null`.
  - Use: small card showing KPIs or counts.

- `ToastContainer.vue`

  - Props: none (mount point for toast store messages).
  - Use: render this once (root layout) to display app toasts from `toast` store.

- `UserTable.vue`

  - Props: `users: User[]`, `loading?: boolean`.
  - Emits: `edit`, `delete` actions from row buttons.
  - Use: admin user management tables.

- `AdminTicketTable.vue`:

  - Props: `tickets: AdminTicket[]`, `isLoading?: boolean`.
  - Emits: `update:currentPage`, `view-ticket` actions for navigating and viewing tickets.
  - Use: admin report ticket management tables.

- `Recommended*.vue`:

  - Props: none (presentational).
  - Use: various recommendation widgets used on `HomeView` to suggest stories.

- `icons/*` (e.g. `IconCheck.vue`, `IconUsers.vue`, `IconProfile.vue`, etc.)

  - Props: optional `class` and `size` for styling.
  - Use: inline SVG components — import and use directly inside templates, e.g. `<IconProfile class="h-5 w-5"/>`.

- `chat/*`: components related to chat functionality (message list, input box, chat window).

- `heatmap/*`: components for rendering the heatmap visualization.

Component usage notes

- Import path examples (in `script setup`):

```ts
import Button3D from "@/components/Button3D.vue";
import MovingInput from "@/components/MovingInput.vue";
// then use directly in template
```

- For `v-model` components (`MovingInput`, `SearchDropdown`, `FilterDropdown`) use the shorthand `v-model="field"` which binds `modelValue` and `update:modelValue` under the hood.

## Tips and Conventions

- Keep API logic inside `src/utils` and `src/stores`; components should be presentational and emit events or use v-model.
- Use the `toast` store to surface errors and success messages (components/pages should not call `alert`).
- All route guards and navigation logic is and should be in `src/router`.
- We recommend using Vue's Composition API with `script setup` syntax for all components and views.
- We use TypeScript throughout; define interfaces/types in `src/utils` or alongside the relevant store/component.
  - Even though TypeScript is used, ensure runtime checks for user input and API responses to avoid uncaught errors.
- Follow existing naming conventions and folder structures when adding new features.
