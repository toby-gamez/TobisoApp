# What the app still lacks vs. Tobiso.Web

_Rewritten 2026-11-17 after a fresh comparison with `/home/tobias/RiderProjects/Tobiso.Web` (HEAD `c0d6b41`). The 2026-10-23 version of this file was badly out of date. This is a **gap list only**; for history see `AUDIT_STATUS.md`, and for server asks see `WEB_SPEC.md`. Decisions in force: accounts are optional and user-only (no admin features), the Chronicle (`/kronika`) is out of scope, ICS calendar feeds are skipped, no WebView for math._

Nothing below was tried on a device (none was available); "done" means it builds and its JVM tests pass.

---

## 1. Needs a server deploy before it works

These app features call endpoints that exist only in the **uncommitted working tree of Tobiso.Web** (each has a task in the `Tobiso.Web` Taskino project, status `verification`). Until the owner reviews and deploys them the app degrades quietly (local grade, an error with retry, a failure toast, or a hidden banner).

| Web endpoint (new) | App feature |
|---|---|
| `GET auth/me` now returns `preferredGradeId`; `PUT user/preferred-grade` `{gradeId}` | Grade syncs with the account (`AuthRepository.syncPreferredGrade`, grade chip) |
| `DELETE user` `{confirm:true}` | Profile → "Smazat účet" |
| `GET user/legal-notice`, `POST user/legal-notice/dismiss` | Home banner "Změna právních dokumentů" |
| `GET practice/flashcards`, `GET practice/recommended`, `GET practice/stats`, `POST practice/attempt` | Flashcard decks, "Doporučené z účtu", account stats line in the practice hub |

## 2. Done since the previous version of this list

Already built before: graph blocks, flowcharts and Venn diagrams, article video, duty roster, daily bonus, points wallet and game-state sync, continue-reading, activity/streak days, per-article notes, data export, calendar sync, class groups, Pomodoro, streaming chat, exam summary and fun facts.

Added now:

- **Manual post order** (`SortOrder` / `SortLabel`): Room migration 9→10, articles ordered like the web in labelled categories, "Řazeno: …" under the heading.
- **Cheat sheet PDF** ("Tahák (PDF)") next to the existing article PDF.
- **AI tools** (post AI sheet): exam questions, key terms, compare (markdown table renderer), why, rewrite (register + grade), formula calculator (sliders + live result via the new multi-variable `FormulaEvaluator`), cross-connections, step solver, concept map (Canvas viewer with pan/zoom/drag/focus, layout ported from `mind-map.js`).
- **Reading**: image lightbox (pinch-zoom, caption/source, also for images in table cells) and reading settings (font, size, column width).
- **Math/rendering**: `LatexLite` handles matrices, `cases`, aligned equations, `\binom`; AI answers now convert `$…$` math; rewrite output goes through the article renderer.
- **Accounts**: sign-in prompt in the notes tool, account deletion, legal-change banner.
- **Practice**: category flashcard decks with "Umím / Musím zopakovat", account-wide recommended practice, account stats. (The hub itself, per-category quizzes and weak spots already existed in the practice tab; the previous version of this file wrongly said they did not.)

## 3. Still different or missing

| Item | Notes |
|---|---|
| **KaTeX-grade math** | `LatexLite` is Unicode text, not typeset math: no stacked fractions or big operators, matrix columns are not pixel-aligned. Deliberate (no WebView); revisit only if real articles need it. |
| **Web "AI zkoušení" page** (`/practice/ai`) | Not ported; the AI sheet covers per-article AI practice. |
| **Cookie consent** | Not applicable: it only governs Google Analytics in the browser. |
| **Related-post graph modal** | Registered on the web but not wired to any UI there either. |
| **Concept map polish** | No collision resolution (like the web, the layout is only a starting point); not tested with large maps. |
| **Formula calculator** | Depends on what the server's AI writes into `expression`; unparseable formulas show the reason instead of a result. |
| **Interactive demo** (`ai/generate-demo`) | Not ported (needs a WebView or a rewrite). |
| **Calendar polish** | Student-event writes are online-only by design; the optional "Synchronizovat s kalendářem" link-out is the only sync UI. |

## 4. Never verified in this pass

These items from the 2026-10-23 list were **not** checked again and may or may not still be gaps: body-text search (online and offline), a "latest posts" screen, a subject tree rail, article breadcrumb and read-time line, caption/`Zdroj:`/`Autor:` grouping and document cards in articles, diacritics/case-insensitive free-text answer matching, whether "Sdílet" uses the canonical web URL (it builds `https://www.tobiso.com/post<slug>`), Czech 429/403 messages for student events.

## 5. Design parity (unchanged, not features)

- Lucide icons (web) vs. Material Icons Extended (app).
- Legacy screens still on the old styling: Profile (≈1.9k lines), Shop, Calendar, AllQuestions, `FullScreenPointsOverlay`.
- Skeleton loaders (web) vs. spinners (app).
- Baloo 2 font subsetting (680 KB).

## 6. Risks to keep in mind

- The Room 9→10 migration (`posts.sortOrder`, `categories.sortLabel`) compiles and the schema is exported, but there is no migration test (it needs an instrumented test, and `connectedAndroidTest` wipes the app on the phone).
- Compose UI (lightbox, reading sheet, concept map gestures, flashcard deck, banners, sliders) has no automated tests; the logic behind them does (table parser, layout, evaluator, view models, sync).
- Server changes were tested with SQLite in-memory only, not against SQL Server or over real HTTP.
