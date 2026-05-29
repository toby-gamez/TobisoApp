# UI/UX Audit — Tobiso App Native

**Date:** 2026-05-29  
**Auditor:** Claude Code  
**App:** Android (Jetpack Compose, Material Design 3)  
**Screens audited:** 26 screens, 19 components  

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Design System](#2-design-system)
3. [Screen Inventory](#3-screen-inventory)
4. [Pattern Analysis](#4-pattern-analysis)
5. [Inconsistencies](#5-inconsistencies)
6. [Strengths](#6-strengths)
7. [Recommendations](#7-recommendations)
8. [Design Consistency — Modern Reference vs. Other Screens](#8-design-consistency--modern-reference-vs-other-screens)

---

## 1. Executive Summary

The app is built on a solid Material Design 3 foundation with a consistent navigation architecture, a well-defined typography scale, and animated transitions throughout. However, two distinct visual languages exist in the app simultaneously: the **modern design** (Procvičování — flat 0dp cards, semantic container colours, icon-bearing rows, clean section headers) and the **older design** (ProfileScreen and most other screens — 4dp shadow cards, identical-looking tiles, no icons, no grouping). Closing that gap is the single most impactful improvement available.

**Overall rating:** Good — functional and coherent, but two design generations co-exist and the older one needs to be retired.

---

## 2. Design System

### 2.1 Color Palette

| Token | Light | Dark | Semantic use |
|---|---|---|---|
| Primary | #6650A4 (Purple40) | #D0BCFF (Purple80) | Buttons, selected states, key actions |
| Secondary | #625B71 (PurpleGrey40) | #CCC2DC (PurpleGrey80) | Secondary buttons, supplementary UI |
| Tertiary | #7D5260 (Pink40) | #EFB8C8 (Pink80) | Exercise/practice actions |
| Error | System red | System red | Errors — but also used for warnings and offline (see §5.3) |
| Surface / SurfaceVariant | System | System | Card backgrounds |

Dynamic color (Material You) is enabled on Android 12+, which may override the above palette for some users. No visual regression tests exist for dynamic color variants.

### 2.2 Typography

All text uses **Poppins** (Medium and Regular weights).

| Style | Size | Weight | Use |
|---|---|---|---|
| titleLarge | 35sp | Bold | Page titles |
| headlineLarge | 28sp | Medium | Section headings |
| headlineSmall | 22sp | Medium | Sub-section headings |
| displayLarge | 25sp | Bold | Overlay numbers (points) |
| titleMedium | 16sp | Medium | Card titles |
| bodyLarge | 16sp | Regular | Primary body text |
| bodyMedium | 14sp | Regular | Secondary body text |
| labelSmall | 13sp | Medium | Labels, bottom bar |

**Issue:** `headlineLarge` (28sp) and `displayLarge` (25sp) sizes are inverted relative to their names — display should be the largest. This is non-standard and may confuse contributors.

### 2.3 Spacing

The codebase uses the following values without a named constant system:

`6dp` · `8dp` · `12dp` · `16dp` · `24dp` · `32dp`

Standard horizontal screen padding appears to be `16dp`, but `12dp` and `24dp` are also used depending on the screen. No central spacing scale file exists.

### 2.4 Elevation

Cards consistently use `2dp` elevation; interactive cards use `4dp`. Some screens use the `.shadow()` modifier directly instead of the `Card` elevation parameter, causing visual inconsistency. No elevation scale is defined or documented.

### 2.5 Shape

Cards use `RoundedCornerShape(12.dp)` consistently. Dialogs use Material 3 defaults. The `MultiplierIndicator` uses `RoundedCornerShape(20.dp)`, which is inconsistent with the card radius.

---

## 3. Screen Inventory

### 3.1 Bottom-Bar Screens (always show navigation bar)

| Screen | Route | Notes |
|---|---|---|
| HomeScreen | HomeRoute | Subject/category grid |
| AllQuestionsScreen | AllQuestionsRoute | Practice all questions |
| CalendarScreen | CalendarRoute / CalendarWithDateRoute | Event calendar |
| ProfileScreen | ProfileRoute | Stats and settings |
| CategoryListScreen | CategoryListRoute | Posts within a category |

### 3.2 Content Screens

| Screen | Route | Notes |
|---|---|---|
| PostDetailScreen | PostDetailRoute(postId) | Rich HTML post with TTS, AI, exercises |
| QuestionsScreen | QuestionsRoute(postId) | Questions for a post |
| VideoPlayerScreen | VideoPlayerRoute(videoUrl) | HTTPS video |
| AiChatScreen | AiChatRoute(…) | Per-post AI assistant |
| EventDetailScreen | EventDetailRoute(eventId) | Calendar event detail |

### 3.3 Exercise Screens

| Screen | Route |
|---|---|
| CircuitExerciseScreen | ExerciseCircuitRoute |
| DragDropExerciseScreen | ExerciseDragDropRoute |
| MatchingExerciseScreen | ExerciseMatchingRoute |
| TimelineExerciseScreen | ExerciseTimelineRoute |
| MixedQuizScreen | MixedQuizRoute(questionIds) |

### 3.4 Gamification / User Screens

| Screen | Route |
|---|---|
| StreakScreen | StreakRoute |
| ShopScreen | ShopRoute |
| BackpackScreen | BackpackRoute |
| FavoritesScreen | FavoritesRoute |
| AiChatHistoryScreen | AiChatHistoryRoute |

### 3.5 Utility / System Screens

| Screen | Route |
|---|---|
| OfflineManagerScreen | OfflineManagerRoute |
| NoInternetScreen | (shown before NavHost) |
| FeedbackScreen | FeedbackRoute |
| AboutScreen | AboutRoute |
| ChangelogScreen | ChangelogRoute |
| UpdaterScreen | UpdaterRoute |

---

## 4. Pattern Analysis

### 4.1 Loading States

**Pattern used:** `Box(contentAlignment = Alignment.Center) { CircularProgressIndicator() }`

- Appears in: HomeScreen, ProfileScreen, PostDetailScreen, CategoryListScreen, AllQuestionsScreen, CalendarScreen, all quiz/exercise screens.
- Most are full-screen centered; a few are inline (e.g., the "Thinking…" bubble in AiChatScreen uses a different `ThinkingBubble` component).
- Button-level loading is indicated by appending `…` to the label (e.g., `"Cvičení…"`) with `enabled = false`.

**Gap:** No skeleton screens. Spinners give no hint of what content is incoming, increasing perceived wait time.

### 4.2 Empty States

**Pattern used:** `Box(contentAlignment = Alignment.Center) { Text("…") }`

Examples:
- FavoritesScreen (snippets): `"Nemáte žádné útržky."`
- FavoritesScreen (posts): `"Nemáte žádné oblíbené články."`

No icon is paired with the empty state message. There is no reusable component — each screen that needs an empty state writes it inline.

### 4.3 Error States

Three distinct approaches exist:

| Approach | Where used |
|---|---|
| `Icon(Icons.Filled.Error, 64dp) + Text` (centered, error color) | MixedQuizScreen, AllQuestionsScreen |
| Error-colored text inside a `Card` | CategoryListScreen |
| Inline `Text` in secondary/gray color | CircuitExerciseScreen |

The MixedQuizScreen approach (icon + title + message, centered) is the most informative and should be the standard.

### 4.4 Offline Indication

| Style | Color | Where |
|---|---|---|
| `"Jste v offline režimu."` | Error (red) | OfflineManagerScreen |
| `"V offline režimu jsou dostupné…"` | Warning-styled card | MixedQuizScreen |
| `"offline režim: kontrola vyžaduje internet"` | Secondary (gray) | CircuitExerciseScreen |

Red (error) and gray (secondary) carry opposite severity signals. Users who see gray may not realize they're offline; users who always see red may experience alarm fatigue.

### 4.5 Top App Bars

- `LargeTopAppBar` — main/top-level screens (HomeScreen, AllQuestionsScreen, ProfileScreen)
- `TopAppBar` with back button — secondary and detail screens

The `MultiplierIndicator` badge appears in the `actions` slot of the top bar on ProfileScreen, AllQuestionsScreen, and FavoritesScreen, but the styling differs (background `Row` in AllQuestionsScreen vs. bare indicator elsewhere).

### 4.6 Navigation

- 4 bottom bar items; labels are Czech (`Učivo`, `Procvičování`, `Kalendář`, `Profil`).
- The `BOTTOM_BAR_ROUTES` set drives both the bottom bar and the `FloatingSearchBar` visibility.
- Bottom bar enters/exits with a 250ms/200ms vertical slide animation.
- All screen-to-screen transitions use a 400ms `tween` horizontal slide.
- `StreakRoute` uses a vertical slide — the only exception; this inconsistency may feel unintentional.

### 4.7 Floating Search Bar

- Shown only on bottom-bar routes.
- Bottom padding: `100dp` when the bottom bar is visible, `16dp` otherwise.
- Supports debounced search (400ms) and AI mode with post-attachment.
- Visibility uses `AnimatedVisibility` with `fadeIn + slideInVertically` — consistent with other overlays.

### 4.8 Gamification Overlays

Three overlay types are stacked in `TobisoApp.kt`:

1. `FullScreenPointsOverlay` — points gained (scale + circle expansion)
2. `FullScreenMilestoneOverlay` — streak milestone
3. `FullScreenAchievementOverlay` — achievement unlocked

All three are center-screen and triggered from state flows. Priority/sequencing logic lives inline in `TobisoApp.kt`. If multiple events fire simultaneously the overlay stacking order is not clearly defined, which can cause visual overlap.

### 4.9 Dialogs

| Type | Where used |
|---|---|
| `AlertDialog` (Material 3) | Most confirmation/info dialogs |
| `Dialog(DialogProperties(usePlatformDefaultWidth = false))` | AddEditEventDialog, ImageCropper |
| Custom full-screen | AiConsentDialog |

No shared dialog theme wrapper exists.

### 4.10 Cards and Lists

- `Card(cardColors(containerColor = surfaceVariant), shape = RoundedCornerShape(12.dp))` is the standard list card.
- Horizontal padding: `16dp`; vertical between items: `8dp`.
- No consistent `Divider` usage — some lists use cards separated by padding, others have no visual separator.
- The `LazyVerticalGrid(GridCells.Adaptive(300.dp))` in HomeScreen adapts columns automatically, which is good for tablets.

---

## 5. Inconsistencies

### 5.1 No Shared Empty/Error/Loading Components

Every screen implements its own inline empty state, error state, and loading spinner. A change to the visual style requires edits across 26 files.

**Affected screens:** All 26.

### 5.2 Typography Scale Name Mismatch

`headlineLarge` (28sp) is smaller than `displayLarge` (25sp? — actually displayLarge at 25sp is smaller than headlineLarge at 28sp). Additionally `displayLarge` at 25sp is smaller than standard MD3 `displayLarge` (57sp). These names do not match Material Design 3 semantics, which will confuse contributors using the MD3 spec as reference.

### 5.3 Offline/Warning Color Semantics

Error color (red) is used for both hard errors *and* offline warnings. This conflates severity levels. A dedicated warning color (amber/yellow) or a secondary-tone chip would better distinguish "you're offline" from "an operation failed."

### 5.4 MultiplierIndicator Placement

Appears in the top bar `actions` slot on some screens, and as a standalone `Row` with a coloured background on others. The visual weight and interactive affordance differ between placements.

### 5.5 Transition Direction for StreakScreen

All screens use horizontal slide transitions. StreakRoute uses a vertical slide. This is the only exception and appears unintentional — it makes the streak screen feel like a modal/sheet rather than a navigation peer.

### 5.6 Elevation: `.shadow()` vs. Card Elevation

Some screens apply `.shadow(elevation)` directly on `Modifier` instead of using the `elevation` parameter of `Card`. This can result in double shadows (the card draws its own elevation colour tint on MD3) or misaligned visual weight.

### 5.7 Icon Choices

- Back navigation uses `Icons.Default.ArrowBack` in most screens but `Icons.Default.ArrowUpward` in StreakScreen — semantically wrong (up ≠ back).
- No icon audit has been done for semantic consistency.

### 5.8 Scroll / Refresh Capability

`PullToRefreshBox` is used in PostDetailScreen and MixedQuizScreen, but not in HomeScreen, AllQuestionsScreen, CalendarScreen, or CategoryListScreen, which are the most commonly visited and most likely to have stale data.

### 5.9 Dialog Width Consistency

`Dialog(DialogProperties(usePlatformDefaultWidth = false))` creates full-width dialogs; `AlertDialog` uses platform defaults (~80% screen width). Mixing these in the same flow creates a jarring width jump.

### 5.10 Section Header Duplication

ShopScreen and CalendarScreen both implement a section-header pattern (label above a group of items) inline. HomeScreen does the same for subject categories. No shared `SectionHeader` composable exists.

---

## 6. Strengths

1. **Material Design 3 adoption is thorough** — `NavigationBar`, `TopAppBar`, `Card`, `Button`, `AlertDialog` all use MD3 APIs.
2. **Type-safe navigation** — `@Serializable` route objects eliminate stringly-typed argument bugs.
3. **Consistent card radius** — `12dp` corners are applied uniformly across all card types.
4. **Adaptive grid layout** — `GridCells.Adaptive(300.dp)` in HomeScreen scales correctly on large screens and tablets.
5. **Animated bottom bar** — smooth enter/exit with `AnimatedVisibility` + slide transitions.
6. **Dark mode** — dynamic color + system theme detection is correctly wired.
7. **Persistent TTS player** — rendered once at the app level, survives navigation, correct bottom padding offset.
8. **Component extraction** — `PostActionsRow`, `ExerciseButtonsRow`, `MultiplierIndicator`, `GradeBadge` are already extracted and reused.
9. **Offline architecture** — dedicated `OfflineManagerScreen` and per-screen graceful degradation.
10. **Consistent card-level loading** — buttons use `enabled = false` + ellipsis text uniformly for async actions.

---

## 7. Recommendations

### Priority 1 — Component Extraction (high impact, low risk)

| # | Action | Benefit |
|---|---|---|
| P1-1 | Create `EmptyState(icon, title, subtitle)` composable in `components/` and replace all inline empty states | Single-source styling; icon + text always present |
| P1-2 | Create `ErrorState(icon, title, message, onRetry)` composable and replace all inline error states | Consistent retry affordance across all screens |
| P1-3 | Create `LoadingState(modifier)` composable wrapping `CircularProgressIndicator` in a centred box | Allows later upgrade to skeleton screens in one place |
| P1-4 | Create `OfflineBanner` composable with amber/warning colour distinct from error red | Separates severity levels; reduces alarm fatigue |
| P1-5 | Create `SectionHeader(text)` composable | Consistent section heading style |

### Priority 2 — Design System Fixes

| # | Action |
|---|---|
| P2-1 | Rename `Type.kt` styles to match MD3 naming (`displayLarge` → `heroLarge` or similar) or adjust sizes to match MD3 semantics |
| P2-2 | Define a spacing scale as named constants (`SpacingXs = 4dp`, `SpacingSm = 8dp`, `SpacingMd = 16dp`, `SpacingLg = 24dp`, `SpacingXl = 32dp`) |
| P2-3 | Define an elevation scale (`ElevationCard = 2.dp`, `ElevationCardRaised = 4.dp`) and replace all `.shadow()` usages |
| P2-4 | Standardise `MultiplierIndicator` into a single layout that can be placed in a `TopAppBar` actions slot without a surrounding coloured `Row` |

### Priority 3 — Navigation & Transitions

| # | Action |
|---|---|
| P3-1 | Change `StreakRoute` transition from vertical to horizontal slide to match all other screens |
| P3-2 | Change `StreakScreen` back button icon from `ArrowUpward` to `ArrowBack` |
| P3-3 | Add `PullToRefresh` to HomeScreen, AllQuestionsScreen, CalendarScreen, and CategoryListScreen |

### Priority 4 — Overlay & Dialog Polish

| # | Action |
|---|---|
| P4-1 | Extract gamification overlay sequencing from `TobisoApp.kt` into a `GamificationOverlayManager` composable with an explicit queue so events don't stack visually |
| P4-2 | Standardise all dialogs to use `AlertDialog` (MD3); reserve `Dialog(usePlatformDefaultWidth=false)` only for image/media pickers |

### Priority 5 — Accessibility & Feedback

| # | Action |
|---|---|
| P5-1 | Audit all `Icon` calls for missing `contentDescription` — many use `contentDescription = null` which hides actions from screen readers |
| P5-2 | Add `HapticFeedback` on primary interactions (correct answer, points awarded, card tap) |
| P5-3 | Ensure minimum touch target of 48×48dp on all icon buttons (some use 20–24dp icon size with no explicit minimum touch target modifier) |

---

---

## 8. Design Consistency — Modern Reference vs. Other Screens

The **Procvičování** screen (`AllQuestionsScreen.kt`) is the most recently designed screen and sets the visual language the rest of the app should converge toward. This section compares it against the other screens and calls out where they diverge.

### 8.1 What makes Procvičování the reference

| Design decision | What Procvičování does |
|---|---|
| Card elevation | `0.dp` — flat, filled with container colour only |
| Card colours | Semantic: `primaryContainer`, `secondaryContainer`, `tertiaryContainer`, `surfaceVariant` per purpose |
| Card corner radius | `16dp` for action tiles; `12dp` for category grid cards |
| List rows | Plain `Row` + `.clip(RoundedCornerShape(12.dp))` + `.clickable` — **not** a `Card` |
| Section headers | `Text(titleMedium, Bold)` with `padding(start=16dp, top=20dp, bottom=8dp)` — no wrapper card |
| Progress indicator | Inline 4dp `LinearProgressIndicator` on cards |
| Secondary text | `onSurface.copy(alpha=0.5f)` or `0.6f` — consistent opacity scale |
| Icons in rows | Leading icon, `ChevronRight` trailing — every row has both |
| Horizontal scroll sections | Cards in a `Row + horizontalScroll` for quick actions and weak spots |

### 8.2 ProfileScreen — the "too many cards" problem

ProfileScreen is the clearest divergence from the modern design. It wraps **every single navigation link** in its own `Card(elevation=4dp, surfaceVariant)`:

```
Offline Manager    → Card(elevation=4dp)
Favorites          → Card(elevation=4dp)
AI Chat History    → Card(elevation=4dp)
Updater            → Card(elevation=4dp)
Feedback           → Card(elevation=4dp)
About              → Card(elevation=4dp)
Changelog          → Card(elevation=4dp)
Post items         → Card(elevation=4dp) each
```

**Problems with this:**
- `4dp` elevation creates visible drop shadows on every card, making the screen feel heavy and visually noisy.
- All cards are identical — same `surfaceVariant` colour, same `titleMedium + bodySmall` text, no icon. Nothing differentiates one from another at a glance.
- These are navigation rows, not content tiles. Procvičování handles the same pattern (`ExerciseRow`) as a plain flat `Row`, not a `Card`.
- The navigation cards sit inside a `LazyVerticalGrid(GridCells.Fixed(1))` in portrait — effectively a `LazyColumn` of cards, which adds card border+shadow overhead for no benefit.
- There is a mix: `ProfileSection` and `GradeSelectorSection` use `Card(elevation=0dp, surface)`, while the navigation links use `Card(elevation=4dp, surfaceVariant)` — two visual tiers with no semantic reason.

**What it should look like (aligned with Procvičování):**
- Navigation links → plain `Row` with leading icon + `ChevronRight`, grouped under section headers, zero elevation.
- Group them: **Gamification** (Backpack, Shop), **Library** (Favorites, AI Chats), **App** (Updater, Feedback, About, Changelog, Offline Manager).
- The sections themselves should use a `Card(elevation=0dp, surfaceVariant, RoundedCornerShape(16dp))` as a container with rows separated by `HorizontalDivider` inside — the iOS/MD3 settings pattern.

### 8.3 Screen-by-screen consistency gap table

| Screen | Elevation used | Row style | Icons on rows | Section headers | Matches Procvičování? |
|---|---|---|---|---|---|
| **AllQuestionsScreen** | 0dp | Flat rows + clip | ✓ | ✓ (plain Text) | ✓ Reference |
| **HomeScreen** | 2dp | Grid cards | partial | ✓ | Partial |
| **ProfileScreen** | 0dp + 4dp mixed | Full Card per link | ✗ | ✗ | ✗ Needs full rework |
| **CategoryListScreen** | 2dp | Card per post | ✗ | ✗ | Partial |
| **FavoritesScreen** | 2dp | Card per item | ✗ | Tab only | Partial |
| **ShopScreen** | 2dp | Card per item | ✗ | ✓ | Partial |
| **BackpackScreen** | 2dp | Card per item | ✗ | ✗ | Partial |
| **StreakScreen** | 2dp | Calendar + cards | N/A | ✗ | Partial |
| **AiChatHistoryScreen** | 2dp | Card per chat | partial | ✗ | Partial |

### 8.4 Elevation is the biggest visual divider

Procvičování uses `0dp` elevation everywhere, relying purely on `containerColor` to distinguish surfaces. Most other screens use `2dp` or `4dp`, which creates visible shadows — particularly jarring on ProfileScreen (`4dp` on every nav card). The result is two distinct visual languages in the same app.

**Rule to apply:** elevation `0dp` for all cards; colour alone creates the surface hierarchy.

### 8.5 Missing icons on navigation rows

Every interactive row in Procvičování has:
1. A leading icon (purpose identifier)
2. Title + subtitle
3. A trailing `ChevronRight`

ProfileScreen's navigation cards have: title + subtitle only — no leading icon, no chevron. A user cannot distinguish "Offline Manager" from "About" until they read the text. Icons add orientation and scan-ability.

The icons already exist in the codebase for most destinations:
- Offline Manager → `Icons.Default.WifiOff` / `CloudDownload`
- Favorites → `Icons.Default.Favorite` / `Star`
- AI Chat History → `Icons.Default.Forum` / `Chat`
- Updater → `Icons.Default.SystemUpdate` / `Refresh`
- Feedback → `Icons.Default.Feedback` / `RateReview`
- About → `Icons.Default.Info`
- Changelog → `Icons.Default.History`

### 8.6 Section grouping is missing from Profile

Procvičování groups content visually: "Rychlý start", "Slabá místa", "Interaktivní cvičení", "Procvičovat dle tématu". Each section has a header and the content follows naturally.

ProfileScreen has no grouping. The current order (ProfileSection → Quote → Grade → Achievements → Offline → Favorites → AI → Updater → Feedback → About → Changelog → Posts) is neither alphabetical, by frequency of use, nor by category. It reads as an accumulation of features added in the order they were built.

**Suggested grouping:**
```
[Profile card — avatar, name, pet]
[Ročník chips]

──── Aktivita ────
[Odznaky / Achievements]
[Streak] (link to StreakScreen)

──── Knihovna ────
[Oblíbené]
[Nedávné AI chaty]

──── Nastavení ────
[Správce offline dat]
[Aktualizátor]

──── Informace ────
[Zpětná vazba]
[O aplikaci]
[Deník změn]
[Posts from category 42]
```

### 8.7 Card corner radius inconsistency in Profile

Within ProfileScreen alone:
- `ProfileSection` card: `RoundedCornerShape(20.dp)`
- `GradeSelectorSection` card: `RoundedCornerShape(20.dp)`
- Navigation cards: `cardShape = RoundedCornerShape(16.dp)`
- `AchievementsSection` card: `RoundedCornerShape(16.dp)`
- `BadgeCard`: `RoundedCornerShape(12.dp)`
- Pet bubble card: `RoundedCornerShape(20.dp)`

Procvičování uses `16dp` for action tiles and `12dp` for smaller/inline cards consistently. ProfileScreen uses `20dp`, `16dp`, and `12dp` in the same scroll, creating subtle but perceivable jaggedness.

### 8.8 Summary: what to change on ProfileScreen

| Change | Effort | Impact |
|---|---|---|
| Replace 7 nav-link Cards with flat rows + icons + chevron | Low | High — removes the "wall of identical cards" |
| Drop elevation on all cards from 4dp to 0dp | Trivial | High — aligns with Procvičování visual language |
| Add section headers (Aktivita / Knihovna / Nastavení / Informace) | Low | High — scanability |
| Standardise corner radius to 16dp (containers) / 12dp (inline) | Low | Medium — visual polish |
| Group rows inside section containers (one card, dividers inside) | Medium | High — settings-list pattern, MD3 compliant |

---

## Appendix: File Reference

| File | Purpose |
|---|---|
| `ui/theme/Color.kt` | Color palette |
| `ui/theme/Type.kt` | Typography scale |
| `ui/theme/Theme.kt` | Theme composition, dynamic color |
| `navigation/Routes.kt` | All route definitions, `BOTTOM_BAR_ROUTES` set |
| `navigation/BottomBar.kt` | Bottom navigation bar composable |
| `TobisoApp.kt` | NavHost, overlay state, TtsPlayer, FloatingSearchBar |
| `components/` | All shared composables (19 files) |
| `screens/` | All screens (26 files) |
