# What the app still lacks vs. Tobiso.Web

_Written 2026-10-23 from a fresh read of `/home/tobias/RiderProjects/Tobiso.Web` (HEAD `2bfc6db`) against this repo (HEAD `09f0e82`). It is a **gap list only**; for history and already-landed work see `AUDIT_STATUS.md`, and for server asks see `WEB_SPEC.md`. Decisions already made stay in force: accounts are optional and user-only (no admin features), the Chronicle (`/kronika`) is out of scope, and ICS calendar feeds are skipped._

Effort: **S** ≈ a day or less, **M** ≈ a few days, **L** ≈ a week or more. "Server" means the web must add or change something first.

---

## 1. Content viewing (the biggest visible gap)

| # | Web feature | Where on the web | App today | Effort |
|---|---|---|---|---|
| 1.1 | **Graph viewer** for articles: `[graf1] … [/graf1]` blocks draw axes, grid, curves, points and **function plots** (`y = 2x^2 - 3sin(x)`, implicit multiplication, `tg`, `cotg`, `\|x\|`, π, decimal commas) with captions/notes. Colours follow light/dark. | `Tobiso.Web.Shared/Graphs/GraphRenderer.cs` (710 l.), `GraphExpression.cs` (204 l.), called from `MarkdownContent.razor:75/268`; authoring guide in admin `FormattingLegend.razor` | `ContentFormatter` has no graph block, so the raw `[graf1]` text is shown as plain paragraphs | **L** |
| 1.2 | **Mind map / concept map** viewer (pan, zoom, drag nodes, tap a node to focus its branch, organic orbit layout, theme-aware) for the AI tool "Myšlenková mapa" | `wwwroot/js/mind-map.js` (486 l.), `GET ai/concept-map/{postId}` | Not present (no endpoint, no renderer) | **L** (Compose `Canvas` + gesture layer) |
| 1.3 | **Image lightbox** (tap image → full-screen, pinch-zoom, caption/source) | `wwwroot/js/image-lightbox.js` | Images render inline only; no zoom/pan viewer | **S–M** |
| 1.4 | **Reading settings** panel: font (Figtree/Serif/Mono), size (S/M/L), column width | `PostDetail.razor` `.reading-panel` | Only the global font scale, if any; no per-reader font/size panel | **S** |
| 1.5 | **Per-article notes** ("Moje poznámky k článku", auto-save on blur, preview chip under the toolbar) | `PostDetail.razor` notes panel | No note-taking at all. _Where the web persists notes (server vs. browser) was not verified; if it is server-side an endpoint is needed._ | **M** (+ Server?) |
| 1.6 | **Math rendered by KaTeX** (full LaTeX) | `katex-init.js`, `MathText.razor` | Unicode approximation (`LatexLite`); matrices, aligned equations, big fractions and anything unknown degrade to text. Math inside tables/code is not converted | **M–L** (decision: no WebView so far) |
| 1.7 | Post **breadcrumb** (category path) and **word count · read time** line | `PostDetail.razor` header | No breadcrumb; check for read-time line | **S** |
| 1.8 | **Embedded YouTube video** at the top of the post (`posts/{id}/video`) | `PostDetail.razor` `post-video-embed` | `VideoPlayerScreen` exists for article videos; the post-level `GET posts/{id}/video` endpoint is not in `ApiService` | **S** |
| 1.9 | **Docs/video links** in markdown (`*[alt](videos/x.mp4)`, `_[label](documents/x.docx)`) rendered as cards; image + caption + `Zdroj:`/`Autor:` grouping; table fallbacks | `MarkdownContent.razor` | Partial; verify caption/source grouping and document cards | **S–M** |

## 2. AI tools (web: ~30 endpoints; app: 8 wired)

Already in the app: `ask`, `credits`, `flashcards`, `real-world`, `what-if`, `evaluate-comprehension` (Feynman), `explain-sentence`, `practice-problems`, `detect-persons`, `person`.

Missing, in the order they appear in the web's "Více" menu:

| Tool | Endpoint | Notes |
|---|---|---|
| **Co může přijít v testu** (exam) | `GET ai/exam-questions/{postId}`, `GET ai/exam-summary/{postId}` | Probably the most useful for students; pure read, easy UI |
| **Porovnat s…** (compare table) | `POST ai/compare` | Needs a table renderer for the AI output |
| **Krok za krokem** (step solver) | `GET ai/step-solver/{postId}` | Needs math rendering (see 1.6) |
| **Myšlenková mapa** | `GET ai/concept-map/{postId}` | Needs the viewer from 1.2 |
| **Jiný ročník** (AI rewrite to another grade) | `POST ai/rewrite-grade` | Linked from the grade dropdown; the picker only lists existing versions |
| Key terms | `GET ai/key-terms/{postId}` | Glossary chips / bottom sheet |
| Fun facts | `GET ai/fun-facts/{postId}` | The web's `_factsOpen` panel |
| "Why?" | `POST ai/why` | Follow-up on a sentence/answer |
| Rewrite register | `POST ai/rewrite-register` | Simpler/more formal wording |
| Formula variables | `GET ai/formula-vars/{postId}` | Legend for formulas |
| Cross-connections / related suggestions | `GET ai/cross-connections/{postId}`, `GET ai/related-suggestions/{postId}`, `GET ai/suggest-related/{postId}` | "Souvisí s…" suggestions beyond the manual `RelatedPosts` |
| Generate demo | `POST ai/generate-demo/{postId}` | Interactive demo; likely needs a WebView or is skippable |
| Evaluate answer | `POST ai/evaluate-answer` | Grading of free-text answers outside the Feynman flow |
| **Streaming chat** | `POST ai/ask-stream` | App uses blocking `ai/ask`; streaming gives the typing effect and avoids timeouts |
| Server chat history | `GET ai/history`, `GET ai/history/{sessionId}` | App keeps chat history only in Room; with an account the web history is separate and not shown |
| Credit/usage display | `ai/credits` + Profile "Využití AI" | App shows credits; no usage breakdown |

Admin-only (`grammar-check`, `generate-question`, `classify-flashcard-eligibility`, `diag`) are **intentionally excluded**.

The sheet in `AiToolsBottomSheet.kt` hard-codes six tools; moving to a data-driven registry (already in the roadmap) makes each of the rows above a small addition.

## 3. Study tools on the post screen

| Web feature | App today | Effort |
|---|---|---|
| **Pomodoro timer** widget (`pomodoro.js`, floating) | Missing. A foreground-service/notification timer would be a natural native fit | **M** |
| **Print / PDF of the article** (`GET Pdf/generate/post/{id}` — the app has the endpoint declared at `ApiService.kt:76`) | Declared, but nothing calls it; no "Vytisknout (PDF)" action | **S** |
| **Tahák (cheat sheet) PDF** (`GET Pdf/cheatsheet/{id}`) | Missing (no endpoint, no UI) | **S** |
| **Listen (Poslech)** | App is *ahead* here (TtsPlayer); the web button is disabled | — |
| Share article | `ACTION_SEND` exists in `PostDetailScreen`; verify it shares the canonical web URL (`/post/{slug}`) | **S** |
| **Related-post graph** (`PostsGraphModalService`) | Registered on the web but the modal is not wired to any UI there either, so there is nothing to port yet | — |

## 4. Practice and quizzes

| Web feature | App today |
|---|---|
| **Practice hub** (`/practice`): "Doporučeno pro tebe" based on weak categories, per-subject progress, shortcuts to quiz / flashcards / AI practice | No hub screen. Quiz entry points are per post, plus `AllQuestionsScreen` |
| **Quiz by category** (`/practice/quiz/category/{id}`) and **`mode=recommended`** (needs account; session of 25, prefers unmastered) | `MixedQuizScreen` takes explicit ids; no category mode, no recommended mode |
| **Server-side attempt tracking** (`RecordAttempt` for signed-in users) so "recommended" works across devices | App keeps `QuestionProgressManager` locally only |
| **Flashcards page** (`/practice/flashcards[/{categoryId}]`): whole-category decks, "knew it / didn't" buttons, "Hotovo → Projít znovu" | Flashcards exist only as the per-post AI tool, no category deck, no spaced-repetition-like known/unknown tracking |
| **AI practice page** (`/practice/ai[/{postId}]`) | Only inside the AI sheet |
| **Free-text answer matching** diacritics/case-insensitive (spec §8.1) | Verify in `QuestionsScreen` |

## 5. Account and profile

| Web feature | App today |
|---|---|
| **Profile sections**: progress by subject, **badges (Odznaky)**, AI usage, "Moje data", sign-in/out, **delete account** | Account section has stats, sign-out, sign-out-everywhere. **No account deletion** (also a Google Play requirement once accounts exist), no per-subject progress view fed by the server |
| **Data export** (`GET user/export`) | Not wired |
| **Daily bonus** (`POST auth/daily-bonus`) | Not wired; only the read-progress side effect triggers credits |
| **Password reset / forgot password / email verification** | Opens the web by design; deep-link back (`/overeni-emailu`, `/obnova-hesla`) into the app is not handled |
| **Register flow** polish: consent text, resend verification mail | Basic version only |
| **Class membership card** | ✅ done (`ClassesScreen`) |

## 6. Navigation, search, discovery

| Web feature | App today |
|---|---|
| **Full-text search modal** across titles, categories **and text inside posts** (`pages/search`, `SearchModal.razor`) | `HomeSearchBar` / `FloatingSearchBar` search titles; confirm whether body-text search is available offline and online |
| **Latest posts** page (`/latest`) | Missing as a screen (Home may show a few) |
| **Subject tree rail** (`pages/tree`, `pages/categories/ancestors/{id}`) | Category list screen only; no tree browse, no breadcrumb |
| **Bookmarks page** (`/zalozky`) | ✅ (`FavoritesScreen`) |
| **Cookie consent + legal-change banner** (`LegalChangeBanner`) | No legal screens in-app; links out. Needed before publishing accounts on Play |
| **Addendum modal** | ✅ (`AddendumDialog`) |
| **Person modal** | ✅ (`PersonBottomSheet`) |
| **Feedback** | ✅ (`FeedbackScreen`) |

## 7. Calendar polish (small leftovers)

- Offline writes for student events (by design not done).
- Rate-limit (`429`) and `403` messages for student events: check they are shown in Czech, not generic errors.
- Sync-to-calendar card (`CalendarSyncCard`): skipped by decision; an optional "Synchronizovat s kalendářem" link-out is the cheapest option.

## 8. Design parity (not features)

- Lucide icons (web) vs. Material Icons Extended (app).
- Legacy screens still on the old styling: Profile (≈1.9k lines), Shop, Calendar, AllQuestions, `FullScreenPointsOverlay`.
- Skeleton loaders (web uses `.skeleton` placeholders everywhere; app mostly spinners).
- Baloo 2 font subsetting (680 KB).

## 9. Server-side asks that would unblock more

1. Expose the calendar feed token (`GET/POST user/calendar-feed`) if in-app ICS is ever wanted.
2. A REST endpoint (or confirmation) for **post notes** if they are not already server-side, so notes sync with the account.
3. Optional pre-rendered graph data: e.g. `GET posts/{id}/graphs` returning parsed series/points, so the app does not have to re-implement `GraphExpression` parsing. Alternative: port the C# parser to Kotlin (≈200 lines, self-contained) and test against the web's cases.
4. Pre-rendered HTML/AST for posts (roadmap item "shared rendering") would eliminate 1.1, 1.6 and most of 1.9 in one go, at the cost of changing the offline cache format.

---

## Suggested order

1. **Graph viewer (1.1)** — most visible gap, content is currently broken on any article that uses `[graf]`. Start with: parser for the block syntax → data classes → `Canvas` composable (axes, grid, curves, points, captions) → port `GraphExpression` to Kotlin with tests.
2. **Exam tool + key terms + fun facts (§2)** — cheap wins once the AI registry exists.
3. **PDF print / cheat sheet (§3)** — endpoints exist, `S`.
4. **Account deletion + export + daily bonus (§5)** — compliance and parity.
5. **Reading settings + image lightbox + notes (§1.3–1.5)**.
6. **Practice hub + category quiz + category flashcards (§4)**.
7. **Mind map viewer + `concept-map`, compare, step solver (§1.2, §2)**.
8. **Streaming chat (§2)** and **Pomodoro (§3)**.
9. **KaTeX-grade math (§1.6)**, only if Unicode math proves insufficient in real articles.

## Verification notes

- Confirmed absent in the app by search: graph blocks, mind map, Pomodoro, `ask-stream`, exam tools, key terms, fun facts, formula vars, cheat-sheet PDF, reading-font settings, pinch-zoom images, `user/export`.
- Items marked "verify" or "check" were not confirmed either way; read the named file before starting them.
- The web's admin app (`Tobiso.Web.App.Admin`) was only inspected for authoring syntax (graph guide); none of its features belong in the app.
