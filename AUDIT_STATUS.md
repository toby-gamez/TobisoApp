# Tobiso App Native — Status vs. Tobiso.Web

_Originally written 2026-10-16 (static review of both repos). Updated the same day after Phase 0/1 work. **Re-synced with Tobiso.Web on 2026-10-23** (school classes, class/group/personal events, event themes, ICS calendar feed, email verification, cookie sessions) — see §0.1, §1, §8.6–8.8. Chronicle (timeline) is **out of scope** by decision._

## TL;DR

The app is a read-only, offline-capable reader for roughly the 2026-05 API. The web has since grown into an account-based platform (~30 AI endpoints, bookmarks/progress sync, grade-versioned posts, PDF/cheatsheets, Organic design). The app uses ~35 of ~150 endpoints and has no user account. Tooling and architecture are modern; the remaining gap is **product parity and screen-level design**, not infrastructure.

| Area | Status | Grade |
|---|---|---|
| Build / tooling / deps | Current; Gradle caches + 4 GB heap on | 🟢 |
| Architecture (MVI + Hilt + Room) | `ApiService` now Hilt-injected (workers + `PersonBottomSheet` still on `ApiClient`) | 🟢 |
| Feature parity with web | Large gap (Phase 2) | 🔴 |
| Design tokens (colors, shapes, fonts) | Organic tokens applied app-wide | 🟡 |
| Design per screen | Legacy screens (Profile, Shop, Backpack, Streak) still hard-code old styling | 🔴 |
| Performance | Over-fetching fixed on questions screens; baseline profile pending device run | 🟡 |
| Offline | Strong (app's advantage) | 🟢 |
| Tests | 119 unit tests green (31 for `ContentFormatter`); still no repository/DAO/UI tests | 🟡 |
| Security | Release secrets stay out of `BuildConfig`; web-side C2/C3 still open | 🟡 |

---

## 0. Progress log (what changed since the audit)

| # | Item | State | Commit |
|---|---|---|---|
| 1 | Untrack `app/release/**` | ✅ | `196cf06` |
| 2 | `collectAsStateWithLifecycle` everywhere | ✅ | `a4b95ae` |
| 3 | Questions screens use `posts/summaries` | ✅ (offline sync intentionally still downloads full content) | `c534379` |
| 4 | Lazy post body | ✅ **already existed** — `PostDetailScreen` uses `LazyColumn` (audit was wrong) | — |
| 5 | Gradle heap/caches | ✅ | `043c4a3` |
| 6 | Organic theme tokens (colors, shapes, Baloo 2 + Figtree) | ✅ | `f70deec` |
| 7 | Hilt-injected `ApiService` | ✅ | `f0a7bf5` |
| 7b | Optional Bearer session seam (`AuthSession.token` → `Authorization: Bearer`) | ✅ seam only; no login UI | `6b6f563` |
| 8 | Baseline Profile module (`:baselineprofile`) | 🟡 module + generator compile; **profile not generated yet** (no device was connected). Run `./gradlew :app:generateBaselineProfile` with the phone attached, then commit `app/src/main/baseline-prof.txt` | `6b6f563` |
| 9 | `ContentFormatter` tests | ✅ | `55d7bdb` |
| 10 | Difficulty rating (§8.4) + article of the day / random (§8.5) | ✅ | see git log |
| 11 | Grade-version picker + "Zkontrolováno" badge (§8.2); choice stored per post, reviewed flag = `PostVersion.isChecked` | ✅ | see git log |
| 12 | "Ověř si porozumění" (§8.1 A + most of B): rename, `QuizCtaCard`, "Otázka i ze n", explanation always shown in results, "Hotovo"/"Začít znovu". **Still open:** web-style instant per-answer feedback (the app reveals results at the end) | 🟡 | see git log |
| 13 | Accounts v1 (§8.6): `AuthSession` persisted encrypted (Keystore AES-GCM), 401 → clear + "Přihlášení vypršelo" snackbar, `AuthScreen` login/register/check-inbox, Profile "Účet" section (sign out, sign out everywhere), forgot password + legal docs open the web. **Verified:** read endpoints are `[AllowAnonymous]`, so a Bearer token doesn't break them. **Not yet:** Google button (needs `auth/google-token`), bookmark/read-progress sync | ✅ | see git log |
| 14 | Bookmark sync (union on first login, then server-authoritative, offline outbox) + reading progress ("Pokračovat ve čtení" on Home, local always, `user/read-progress` when signed in). The card now uses `GET user/continue-reading` when signed in (204 = nothing in progress; offline/failed → device-local fallback), so it follows the account. Note: every read-progress report also triggers the web's 20-credit daily read bonus | ✅ | see git log |
| – | Fixed stale unit tests | ✅ | `0f706f0` |
| – | Numbered lists (`1. foo`) were never parsed (over-escaped regex) | ✅ fixed | `16324ca` |
| – | `<sub>`/`<sup>` rendered as raw text | ✅ fixed + spacing after bullet lists | `bfc5d7b` |

**Corrections to the original audit:** post body was already lazy; `ThemeKt` *was* applied (via `TobisoApp.kt`), it just carried template colors.

**Auth contract (verified in Tobiso.Web `Program.cs`):** `SmartAuth` picks the scheme from the `Authorization` prefix — `Bearer …` → JWT, anything else → Basic. A signed-in request therefore **replaces** the app's Basic credentials; `user/*` endpoints must accept JWT only. Verify that public endpoints the app calls are `[AllowAnonymous]` or JWT-accepting before enabling login.

### 0.1 Web changes since the original audit (2026-10-21 → 10-23)

| Web change | Commit | Impact on the app |
|---|---|---|
| **School classes**: admin-approved membership (`Pending/Approved/Rejected`), one class per user, optional groups, group-change requests needing approval | `c3e9356`, `9bbb207` | New feature area; needs login (§8.7) |
| **Scoped events**: `Type` (`DayOff/Test/Homework/Other`), `Scope` (`Global/Class/Group/Personal`), students can add/edit/delete their own events, `CreatedByName`, rate limit (429) | `c3e9356` | App's `Event` model + `CalendarScreen` don't know these fields (§8.7) |
| **Event themes** (Téma): posts/categories attached to an event, meant for tests | `b4f6164` | Deep-link event → post/category (§8.7) |
| **Event color optional**: `Color` is nullable; `CustomColor` = admin pick, `Color` = effective color | `fe25959` | **Breaking for the app's `Event` DTO** if it declares `color: String` non-null — check now, even before login work |
| **ICS calendar feed** (`/api/calendar/{token}.ics`, per-user secret token, type mask) | `b4f6164` | Not an app feature; the token is only created through the Blazor `CalendarSyncCard` (no REST endpoint) — see §8.8 |
| **Email verification, password reset, session revocation**; student JWT carries a security stamp (`sst`) checked on every request | `78fa7ff` | Register → unverified (403 `email_unverified`); tokens can die server-side at any time → 401 handling is mandatory (§8.6) |
| **HttpOnly cookie sessions** for the website; Google login now sets a cookie and redirects to `/` — **no token is ever returned** | `78fa7ff` | Bearer API clients still work via `student-login`, but **Google sign-in cannot be reused by the app** (§8.6) |
| Admin role allow-list, auth throttling | `ab95542` | None for the app (no admin) |

---

## 1. Feature parity (remaining gaps)

| Web capability | Web endpoints / pages | App today |
|---|---|---|
| User accounts (email + verification + reset; Google is web-only for now) — **user accounts only; the app has no admin login or admin features** | `api/auth/student-login`, `register`, `forgot-password`, `reset-password`, `me`, `logout-everywhere` | 🟡 email/password login + register done; Google and sync open |
| School classes (join, groups, group-change request, leave) | `api/classes`, `classes/mine`, `join`, `mine/groups` | ❌ |
| Class/group/personal calendar events + student-created events (Test/Homework/Other) | `events/mine`, `events/student[/{id}]` | ⚠️ app shows only global events (`Events/range`) and has no type/scope |
| Event themes (linked posts/categories) | `Event.Themes` | ❌ |
| Calendar subscription (ICS) | `calendar/{token}.ics` | ❌ not needed in-app (local notifications exist); optional "open feed" link |
| Server-side progress, bookmarks, export | `api/user/*` | ✅ bookmarks + read-progress synced; export/stats not wired |
| Grade-versioned posts | `postversions`, `ai/rewrite-grade` | ✅ picker + reviewed badge (`ai/rewrite-grade` not wired) |
| ~30 AI tools | `api/ai/*` | ⚠️ 8 wired |
| Article of the day / random | `pages/article-of-day`, `pages/random` | ✅ Home `ArticleOfDayCard`, cached per day |
| Post difficulty rating | `posts/{id}/difficulty`, `rate-difficulty` | ✅ `DifficultyRating` at end of post |
| Reading progress ("continue reading") | `user/read-progress` | ✅ (device-local card, see §0 row 14) |
| "Ověř si porozumění" as a first-class per-article feature | `Quiz.razor`, `PostDetail.razor` | 🟡 renamed + CTA card done; instant per-answer feedback still open (§8.1) |
| Legal / cookie consent flow | `Legal` | ❌ (verify Play-store needs when accounts land) |
| ~~Chronicle / timeline~~ | `api/chronicle/*` | ⛔ out of scope |

**Keep (app-only):** gamification, TTS, offline manager, calendar notifications, update checker. These are local-only and cannot sync with web accounts yet.

**Content rendering:** hand-rolled parser (`ContentFormatter.kt`), now covered by tests. Still missing vs web: callouts (`!!!tip`, `!!!sidefact`), math/KaTeX. `<sub>`/`<sup>` and numbered lists now work.

---

## 2. Design

Tokens now match web `variables.css` (cream `#f5ead8`, terracotta/sage ramps, warm neutrals, 8/16/28 dp radii, Baloo 2 headings, Figtree body, light + dark). Not yet matching:

- Icons: web uses Lucide (stroke 2.75); app uses Material Icons Extended.
- Legacy screens (Profile 1 882 lines, Shop 1 435, Calendar 1 045, AllQuestions 970, FullScreenPointsOverlay 918) hard-code colors/elevation from the old look and ignore the new components below.
- Shell: web = left rail; app = bottom bar (keep bottom bar, it's the mobile idiom).
- Baloo 2 font file is ~680 KB (full glyph set); subset to Latin Extended when tooling is available.

See §8.3 for the web component → Compose mapping.

---

## 3. Performance

Done: summaries on questions screens, lifecycle-aware collection, Gradle caching.
Open:
1. Baseline Profile generation (§0 row 8) + a Macrobenchmark startup test.
2. `OfflineRepositoryImpl.fetchPosts()` still pulls the whole corpus (needed for offline; consider paging/ETag).
3. Giant composables (list in §2); run Compose compiler reports.
4. Workers and `PersonBottomSheet` still use `ApiClient` singleton.

## 4. Architecture / code health (open)

- Gamification singletons (`PointsManager`, `ShopManager`, …) block account sync → need a repository + conflict rules.
- `TobisoApp.kt` (623 lines) is a god-composable.
- ~44 `GlobalScope`/`runBlocking`/`SharedPreferences` usages to spot-check.
- Stale root docs: `AUDIT_UI_UX.md`, `UI_AUDIT_EXERCISE_SCREENS.md`, `INSTRUCTIONS.md` — consolidate.

## 5. Tests

119 unit tests. Missing: repositories, DAOs/migrations, UI. Add a `PostDetail` lazy-list smoke test when instrumented tests are set up.

## 6. Security / API-contract notes

- `ai/credits` signed with `SECURITY_TOKEN_SECRET`; web audit C3 (HMAC skipped when secret unset) and C2 (rate limit trusts client `X-Device-Id`) are server fixes. Accounts fix this properly (per-user credits).
- Web hardening since the audit (`ab95542`, `78fa7ff`): login throttling, email verification, security-stamp session revocation. App implications: handle 429/403 `email_unverified` gracefully; never send admin credentials (the shipped Basic credentials are for public read endpoints only).
- Token storage for login should be `EncryptedSharedPreferences`; add a 401 → clear `AuthSession.token` handler (web: `docs/auth-401-review.md`).

---

## 7. Roadmap (updated)

**Phase 0 — done.** **Phase 1 — done except baseline profile generation** (needs the phone attached).

**Phase 2 — Parity**
0. **Compat check (small, do first):** make the app's `Event` model tolerant of the new fields (`color` nullable, `type`, `scope`, `themes`; enums as strings with `coerceInputValues`/unknown-safe) and fall back to the type color. Also verify the Room `Event` entity/migration if events are cached offline.
1. Difficulty rating (§8.4), article of the day / random (§8.5) — anonymous endpoints, no login needed.
2. Grade-version picker + reviewed badge (§8.2).
3. Optional user accounts (**email/password only** at first), `AuthSession` persistence + 401 handling, bookmark/read-progress sync (§8.6).
4. **Classes + scoped calendar** (needs 3): class picker/status, `events/mine`, student events, themes (§8.7).
5. AI: `ask-stream`, then the remaining tools via a data-driven registry in `AiToolsBottomSheet`.

**Phase 3 — Redesign**
5. **"Ověř si porozumění"** (§8.1) — rename, article CTA card, quiz runner polish; built together with the re-skin using the §8.3 component mapping.
6. Re-skin legacy screens on tokens; split mega-screens; Lucide icon set.
7. Callouts + math in the parser.
8. Decide on shared rendering (pre-rendered HTML/AST from the API).

**Decided:** accounts are **optional** (anonymous stays usable) and **user-only** — no admin login, no admin-only endpoints (`grammar-check`, `PUT posts/{id}/video`, `admin/*`) in the app. **Decided (2026-10-23):**
- **Gamification:** server-authoritative when signed in, device-local (as now) when anonymous. Needs a `GamificationRepository` in front of `PointsManager`/`ShopManager`/`BackpackManager`/streak managers that picks the source by `AuthSession`, plus first-login merge rules (e.g. take max/union, never lose local progress) and server endpoints for points/shop/backpack/streaks (**web work required — spec them before building**).
- **Google sign-in: in scope.** Needs a web change: `POST auth/google-token` (Google ID token → student JWT). App side uses Credential Manager + Google ID. Email/password ships first; Google follows once the endpoint exists.
- **"Ověř si porozumění" (§8.1) moves forward** to right after the grade picker (small, self-contained), instead of waiting for the full re-skin.

---

## 8. Concrete 1:1 implementation specs (web → app)

### 8.1 "Ověř si porozumění" (per-article comprehension check)

The web has **two** distinct things under this name. Mirror both.

**A. Article CTA card** — `PostDetail.razor` ~line 649 (shown only if the post has questions)
- Container `quiz-cta`: icon tile (Lucide `file-check-2`) + `h2` "Ověř si porozumění" + text `Zjisti, jak dobře látku ovládáš (N otázek).` (Czech plural: 1 otázka / 2–4 otázky / 5+ otázek) + pill button "Spustit kvíz →" → `/practice/quiz/{postId}`.
- Also a toolbar button (`file-check-2`, "Ověř si porozumění") at the top of the article that scrolls to this card.
- **App:** replace the "Prověrka" button in `ExerciseButtonsRow.kt:57` and the sticky action bar label in `PostDetailScreen.kt:539` with "Ověř si porozumění"; add a `QuizCtaCard` composable as the last `item {}` of the post `LazyColumn` (before related posts), using `surfaceVariant` tonal card, 28 dp radius, `primaryContainer` icon tile, filled pill button. Question count comes from the already-loaded `questions` in `PostDetailViewModel`. Add `pluralCs(n, "otázka", "otázky", "otázek")` helper in `utils/`.

**B. Quiz runner** — `Quiz.razor`, routes `/practice/quiz`, `/practice/quiz/{PostId}`, `/practice/quiz/category/{id}`
- Header: back pill ("Zpět na článek" if opened from a post, else "Zpět na procvičování"); title `Ověř si porozumění - {postTitle}`; right label `Otázka {i} ze {n}`; progress bar = `(i)/n`.
- One question per screen in a soft card; question text supports math (`MathText`).
- Question kinds (all already in the app's `QuestionsScreen`, keep behavior):
  - **single choice** (exactly one correct): tap answer → instantly locked, correct = sage/check, wrong = terracotta.
  - **multi-select** (>1 correct): checkbox rows, hint "Vyber všechny správné odpovědi.", button "Zkontrolovat odpověď"; correct iff selected set == correct set.
  - **free text** (exactly 1 answer): input "Napiš odpověď…", button "Zkontrolovat odpověď"; compare **diacritics- and case-insensitively**; on wrong show "Správná odpověď: …".
- After answering: explanation callout (lightbulb, sage) from `explanations.first()`.
- Navigation: "Předchozí" (if i>0), "Další otázka →" (after answered), on last: "Hotovo" (back) + "Začít znovu".
- Session size for non-post modes: 25 questions, prefer unmastered; `mode=recommended` uses weak categories (needs account — skip until Phase 2).
- Signed-in users: `RecordAttempt(userId, questionId, correct)` (server-side; skip anonymously — the app keeps its local `QuestionProgressManager`).
- **App gaps vs. this:** explanation shown only when wrong (web: always after answering); label text "Prověrka (i/n)" → "Otázka i ze n" + progress bar; no "Hotovo/Začít znovu" pair; no per-answer result icon check. Change in `QuestionsScreen.kt` (~lines 151, 419) — small.

**C. AI "Zkus to vysvětlit" (Feynman)** — `ai/evaluate-comprehension`
- Request `{postId, studentExplanation}` → `{score, feedback, strongPoints[], missingPoints[]}` (matches app DTO `EvaluateComprehensionResponse`, already wired in `AiToolsBottomSheet.kt:98`).
- Web placement: AI tool panel; costs 1 credit if logged in, otherwise rate-limited per device. App: keep in the AI sheet; optionally also offer it at the end of the quiz ("Vysvětli vlastními slovy").

### 8.2 Grade versions + reviewed badge
- Web: H1 + grade badge pill (`accent-200` bg / `accent-800` text) + chevron → dropdown of all versions (6./7./9. ročník), active highlighted; "Zkontrolováno" badge in sage in the meta row.
- App: `GradeBadge.kt` already shows `activeVersion.gradeName`. Add a `ModalBottomSheet` picker from `Post.versions`, persist choice per post, re-fetch with `gradeId`. Badge: sage chip when version has reviewed flag (confirm field name on `PostVersionResponse`).

### 8.3 Organic component → Compose mapping
| Web | Compose |
|---|---|
| `.pill` / `.pill-on` | `FilterChip` / filled `Button` with `CircleShape`; on = `primary` |
| `.soft` card / `.quiz-card` | `Card` with `surfaceVariant`, 28 dp radius, no shadow |
| `.lift` row | clickable `Surface` with 16 dp radius, `surfaceContainerLow` |
| `.progress-bar` | `LinearProgressIndicator`, rounded track, `primary` |
| tip callout `#ffe1d0` / sidefact `#e1eecc` | `primaryContainer` tint / `secondaryContainer` tint with leading icon |
| `grade-chip` | `AssistChip`, `accent-200` / `accent-800` |
| toolbar (pill container) | existing `PostActionsRow` inside a `surfaceVariant` pill |
| Lucide icons | add `lucide` icon set (compose-icons `LucideIcons`) in Phase 3 |

### 8.4 Difficulty rating
- `GET posts/{id}/difficulty` → `{easy, ok, hard, total}`; `POST posts/{id}/rate-difficulty` body `{rating: 1|2|3, deviceId}` → `{message: "ok" | "already_rated"}`. Anonymous; keyed by `deviceId` (reuse `AiCreditManager.instance.deviceId`).
- UI at the bottom of a post: label "Jak těžký je tento článek?", three pills Snadné/Střední/Těžké; after voting show three progress rows with percentages and "Děkujeme za hodnocení · N hlasů". Persist the user's own vote locally so the results view survives restarts.

### 8.5 Article of the day / random
- `GET pages/article-of-day`, `GET pages/random` → a `Post`. Home screen: "Článek dne" card (sage-tinted) and a "Náhodný článek" action. Cache article-of-day per calendar day for offline.

### 8.6 Accounts, bookmarks, read progress (optional sign-in)
- **Correction:** `POST auth/login` is the **admin** login (Basic-config credentials) — never use it. The app uses `POST auth/student-login` `{username: <email>, password}` (web `LoginRequest(Username, Password)`) → `{token, displayName, credits}`; `GET auth/me` → profile.
- Register: `POST auth/register {email, displayName, password (min 10), acceptedTerms}`; the account is **unverified** and cannot sign in until the mailed link is confirmed (web page `/overeni-emailu`). Login of an unverified account returns `403 {code:"email_unverified"}` (and re-sends the mail) → show a "check your inbox" state. Password reset runs entirely on the web (`/zapomenute-heslo`) — link out with a custom tab instead of re-implementing.
- **Google sign-in (decided: in scope):** the web callback sets an HttpOnly cookie and returns no token, so the app cannot piggyback on it. Plan: the web adds `POST auth/google-token` (Google ID token → student JWT, same shape as `student-login`); the app uses Credential Manager (`GetGoogleIdOption`) and sends the ID token. Ship email/password first, add the Google button when the endpoint is live. Web needs the Android OAuth client ID (package + signing SHA-1) registered as an accepted audience.
- Sessions are revocable: the JWT's `sst` stamp is validated each request, so "log out everywhere"/password reset yield 401 at any time. Store the token in `EncryptedSharedPreferences`, set `AuthSession.token` at app start; on 401 clear it and fall back to anonymous (and show a non-blocking "sign in again" prompt).
- The Bearer header **replaces** the app's Basic credentials (§0 auth contract); the cookie is ignored for non-browser clients, so no CSRF concern in the app.
- `GET user/bookmarks` (ids), `POST/DELETE user/bookmarks/{postId}` — reconcile with local `FavoritesRepositoryImpl` (union on first login, then server-authoritative).
- `POST user/read-progress {postId, scrollPercent}` — send throttled from the lazy-list scroll position; drives "Pokračovat ve čtení" on Home. Post screen already has a table-of-contents index; reading percent can derive from `firstVisibleItemIndex / itemCount`.

### 8.7 Classes and scoped calendar (requires sign-in)

**Data model (web):** `SchoolClass {id, name, gradeId?, gradeName?, groups[{id,name}]}`; `MyClassDto {classId, className, status: Pending|Approved|Rejected, groups, availableGroups, groupChangePending, pendingGroup?}`. One membership per user; joining another class replaces it. Admin approves in the admin app (the app never approves).

**Endpoints (all JWT, students only):**
| Call | Purpose |
|---|---|
| `GET classes` | list classes with groups (counts are zeroed for students) |
| `GET classes/mine` | my membership; `204` = none |
| `POST classes/join {classId, groupIds?}` | request to join → returns `MyClassDto` (Pending) |
| `PUT classes/mine/groups {groupIds}` | request a group change (applies after admin approval, `groupChangePending=true`) |
| `DELETE classes/mine` | leave |
| `GET events/mine?startDate&endDate` | global + my class + my groups + personal events |
| `POST events/student`, `PUT/DELETE events/student/{id}` | own events only; body `StudentEventRequest {title, description?, startDate, endDate?, type (Test default), scope (Personal default; Class/Group allowed, **Global not**), groupId?, themes[]}`; `429` when too many added in a day; `403` for other people's events |

**App work:**
- `ClassesScreen` (from Profile): states = no class (picker + group chips) / Pending (waiting card) / Approved (class, groups, "Požádat o změnu skupiny", "Opustit třídu") / Rejected. Contract file first, then `ClassesViewModel`.
- `Event` model: add `type`, `scope`, `classId/className`, `groupId/groupName`, `createdByName`, `customColor`, `themes`; color = `color` (already effective). Enums as strings.
- `CalendarScreen`: when signed in use `events/mine`, else `Events/range`; filter chips by type (Volno/Test/Domácí úkol/Jiné) and scope; show author for student-created events; FAB "Přidat událost" (scope Personal/Class/Group, type) only when signed in; edit/delete only own (`createdByUserId == me`).
- **Themes:** each theme is a post or category → chip on the event detail that opens `PostDetail` (by `postFilePath`/`postId`) or the category screen. Works offline only for cached posts.
- Offline: cache `events/mine` per user in Room (key by userId); queue student-event writes? — **no**, require connectivity for writes in v1.
- Notifications: the existing calendar notification worker should pick up class/group events once `events/mine` is the source; make the worker use the same repository.
- Not in app: admin class management, approvals (user-only decision).

### 8.8 Calendar feed (ICS) — decision
Web users subscribe Google/Apple calendar to `/api/calendar/{token}.ics` (token + type mask live on `AppUser`; no REST endpoint exposes them, only the Blazor `CalendarSyncCard`). The app already has local calendar notifications, so **skip ICS in-app**. If wanted later: link to the web Calendar page ("Synchronizovat s kalendářem") via a custom tab, or ask the web to expose `GET/POST user/calendar-feed`.

### Suggested order of the next commits
1. Event-model compat fix (Phase 2 step 0) — cheap and prevents breakage from the new nullable `color`.
2. §8.4 difficulty + §8.5 article of the day.
3. §8.2 grade picker.
4. §8.1 "Ověř si porozumění" (rename, `QuizCtaCard`, runner polish) — moved forward.
5. §8.6 login/register UI + 401 handling → Google sign-in (after the web endpoint) → bookmark/read-progress sync.
6. Gamification repository: server when signed in, local otherwise (needs web endpoints + merge rules).
7. §8.7 classes and scoped calendar.
8. Generate baseline profile when the device is attached (can happen any time).
9. Design phase: re-skin legacy screens, parser callouts/math, code-health items.
