# Tobiso App Native — Status vs. Tobiso.Web

_Originally written 2026-10-16 (static review of both repos). Updated the same day after Phase 0/1 work. Chronicle (timeline) is **out of scope** by decision._

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
| – | Fixed stale unit tests | ✅ | `0f706f0` |
| – | Numbered lists (`1. foo`) were never parsed (over-escaped regex) | ✅ fixed | `16324ca` |
| – | `<sub>`/`<sup>` rendered as raw text | ✅ fixed + spacing after bullet lists | `bfc5d7b` |

**Corrections to the original audit:** post body was already lazy; `ThemeKt` *was* applied (via `TobisoApp.kt`), it just carried template colors.

**Auth contract (verified in Tobiso.Web `Program.cs`):** `SmartAuth` picks the scheme from the `Authorization` prefix — `Bearer …` → JWT, anything else → Basic. A signed-in request therefore **replaces** the app's Basic credentials; `user/*` endpoints must accept JWT only. Verify that public endpoints the app calls are `[AllowAnonymous]` or JWT-accepting before enabling login.

---

## 1. Feature parity (remaining gaps)

| Web capability | Web endpoints / pages | App today |
|---|---|---|
| User accounts (email, Google, student login) | `api/auth/*` | ❌ (seam exists, no UI) |
| Server-side progress, bookmarks, export | `api/user/*` | ❌ local `FavoritesScreen` only |
| Grade-versioned posts | `postversions`, `ai/rewrite-grade` | ⚠️ `gradeId` + `activeVersion`; no picker / reviewed badge |
| ~30 AI tools | `api/ai/*` | ⚠️ 8 wired |
| Article of the day / random | `pages/article-of-day`, `pages/random` | ❌ |
| Post difficulty rating | `posts/{id}/difficulty`, `rate-difficulty` | ❌ |
| Reading progress ("continue reading") | `user/read-progress` | ❌ |
| "Ověř si porozumění" as a first-class per-article feature | `Quiz.razor`, `PostDetail.razor` | ⚠️ exists as "Prověrka" — see §8.1 |
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
- Token storage for login should be `EncryptedSharedPreferences`; add a 401 → clear `AuthSession.token` handler (web: `docs/auth-401-review.md`).

---

## 7. Roadmap (updated)

**Phase 0 — done.** **Phase 1 — done except baseline profile generation** (needs the phone attached).

**Phase 2 — Parity**
1. **"Ověř si porozumění"** (§8.1) — rename, article CTA card, runner polish. Smallest, highest-visibility, no backend work.
2. Difficulty rating (§8.4), article of the day / random (§8.5) — anonymous endpoints, no login needed.
3. Grade-version picker + reviewed badge (§8.2).
4. Optional accounts (email + Google), `AuthSession` persistence, bookmark/read-progress sync (§8.6).
5. AI: `ask-stream`, then the remaining tools via a data-driven registry in `AiToolsBottomSheet`.

**Phase 3 — Redesign**
6. Re-skin legacy screens on tokens; split mega-screens; Lucide icon set.
7. Callouts + math in the parser.
8. Decide on shared rendering (pre-rendered HTML/AST from the API).

**Decided:** accounts are **optional** (anonymous stays usable). **Open:** is gamification server-authoritative or local?

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
- Login: `POST auth/login` → JWT. Store in `EncryptedSharedPreferences`, set `AuthSession.token` at app start; on 401 clear it and fall back to anonymous.
- `GET user/bookmarks` (ids), `POST/DELETE user/bookmarks/{postId}` — reconcile with local `FavoritesRepositoryImpl` (union on first login, then server-authoritative).
- `POST user/read-progress {postId, scrollPercent}` — send throttled from the lazy-list scroll position; drives "Pokračovat ve čtení" on Home. Post screen already has a table-of-contents index; reading percent can derive from `firstVisibleItemIndex / itemCount`.

### Suggested order of the next commits
1. §8.1 A + B small fixes ("Ověř si porozumění" rename, CTA card, always-show explanation, "Otázka i ze n" + progress bar, Hotovo/Začít znovu).
2. §8.4 difficulty + §8.5 article of the day.
3. §8.2 grade picker.
4. Generate baseline profile when the device is attached.
