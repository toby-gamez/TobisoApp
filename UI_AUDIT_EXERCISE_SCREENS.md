# UI Audit: Exercise Screens

**Date:** 2026-05-29
**Scope:** All 4 interactive exercise screens + shared components
**Auditor:** Claude Code

---

## Table of Contents

1. [Executive Summary](#executive-summary)
2. [Timeline Exercise](#1-timeline-exercise)
3. [Matching Exercise](#2-matching-exercise)
4. [Drag & Drop Exercise](#3-drag--drop-exercise)
5. [Circuit Exercise](#4-circuit-exercise)
6. [Shared Components](#5-shared-components)
7. [Cross-Cutting Issues](#6-cross-cutting-issues)
8. [Priority Recommendations](#7-priority-recommendations)

---

## Executive Summary

The app has 4 exercise screen types (Timeline, Matching, DragDrop, Circuit) following MVI architecture with `BaseAndroidViewModel`. **Timeline, Matching, and DragDrop** share a near-identical pattern (Load → Interact → Validate via API → Award points). **Circuit** is a fundamentally different sandbox with real-time physics simulation and no server validation.

**Key findings:**
- High code duplication across Timeline/Matching/DragDrop screens (~70% identical structure)
- Consistency issues with color/theme usage (hardcoded vs. Material theme colors)
- Missing edge states (empty states, keyboard handling, accessibility)
- Circuit exercise diverges in UX patterns and is missing points integration
- No TTS integration in any exercise screen
- No retry mechanism for failed API calls visible to user

---

## 1. Timeline Exercise

**Files:** `TimelineExerciseScreen.kt`, `TimelineExerciseViewModel.kt`, `TimelineExerciseContract.kt`

### Layout Structure
```
Scaffold(TopAppBar) → Column
  ├── Loading indicator
  ├── Offline notice
  ├── Error card (config load failure)
  ├── Instructions (ContentRenderer)
  ├── Timeline visualization (horizontal bar + dots)
  ├── Assigned events (LazyColumn, weight 1f)
  ├── Available events (LazyColumn, weight 1f)
  ├── Validate button
  └── Validation result card
```

### Issues

| # | Issue | Severity | File:Line |
|---|-------|----------|-----------|
| 1.1 | **Hardcoded success colors** — `Color(0xFFE8F5E9)` and `Color(0xFF1B5E20)` for success card instead of Material `tertiaryContainer`/`onTertiaryContainer` | Medium | `TimelineExerciseScreen.kt:360-361` |
| 1.2 | **Hardcoded score text color** — result card body text uses implicit default instead of `MaterialTheme.colorScheme.onSurface` for score/feedback/explanation | Low | `TimelineExerciseScreen.kt:378-394` |
| 1.3 | **Event shuffling inconsistency** — available events are shuffled via `remember(state.availableEvents)`, but the ViewModel already returns them in original order from config. Shuffle happens on recomposition, not just initial load — could cause confusion if user re-adds removed events | Low | `TimelineExerciseScreen.kt:313-315` |
| 1.4 | **No empty state for "assigned events"** — shows "Přiřazené události k rokům" header even with no assigned events, with only empty year labels. Could be confusing. | Low | `TimelineExerciseScreen.kt:240-298` |
| 1.5 | **No Reset button** — Unlike Circuit, there is no way to reset/clear all placements without navigating away | Low | — |
| 1.6 | **Missing scrollbar/indicators** — if many events, LazyColumns can be tall but user gets no visual hint of scrollability | Low | — |
| 1.7 | **No keyboard handling** — no `ImmediateFocusable` or `FocusRequester`; if instruction contains interactive elements, keyboard state is ignored | Low | — |
| 1.8 | **`NavigateBack` effect unused** — defined in contract, never collected in the screen (unlike Matching which does collect effects) | Low | `TimelineExerciseScreen.kt` vs `MatchingExerciseScreen.kt:71-80` |
| 1.9 | **Points awarded with integer division** — `score / 10` truncates. If score is 0-9, 0 points; 10-19 = 1 point, etc. Max 10 points at 100%. This is intentional but worth noting. | Info | `TimelineExerciseScreen.kt:49` |

### Strengths
- Timeline visualization (horizontal bar with dots) is well implemented with `BoxWithConstraints`
- `ContentRenderer` integration for rich instructions
- Disabled validate button when offline is clear UX

---

## 2. Matching Exercise

**Files:** `MatchingExerciseScreen.kt`, `MatchingExerciseViewModel.kt`, `MatchingExerciseContract.kt`

### Layout Structure
```
Scaffold(TopAppBar, SnackbarHost) → Column
  ├── Loading indicator
  ├── Offline notice
  ├── Error card
  ├── Instructions (ContentRenderer)
  ├── Created pairs (LazyColumn, weight 0.4f)
  ├── Two-column layout (Row, weight 1f)
  │   ├── Left column (LazyColumn)
  │   └── Right column (LazyColumn)
  ├── Validate button
  └── Validation result card
```

### Issues

| # | Issue | Severity | File:Line |
|---|-------|----------|-----------|
| 2.1 | **Inconsistent result colors** — uses `Color(0xFF4CAF50)` / `Color(0xFFFF5722)` with alpha for result card AND pair backgrounds, while Timeline uses a different shade (`Color(0xFFE8F5E9)`) for the same purpose | Medium | `MatchingExerciseScreen.kt:188-189, 344-345` |
| 2.2 | **Left/Right selection uses asymmetric container colors** — left selected uses `primaryContainer`, right selected uses `secondaryContainer`. Inconsistent — one should be used for the active state on both sides. User may be confused about which side is "active" for selection. | Medium | `MatchingExerciseScreen.kt:258-261, 297-300` |
| 2.3 | **No visual feedback when both sides selected and pair auto-creates** — the pair appears in the "Vytvořené páry" list above, but there's no animation or highlight. The items disappear from left/right columns without fanfare. | Low | — |
| 2.4 | **Hardcoded pair arrow color** — `" ↔ "` uses `MaterialTheme.colorScheme.primary` (good), but overall pair display is plain text with no icon | Low | `MatchingExerciseScreen.kt:213` |
| 2.5 | **Pairs section visible above game area** — takes `weight(0.4f)` which can be large when many pairs, pushing interactive area below the fold | Low | `MatchingExerciseScreen.kt:178` |
| 2.6 | **No counter showing remaining pairs to match** — user doesn't know how many items remain on each side | Low | — |
| 2.7 | **`selectedLeft`/`selectedRight` reset on pair creation gives no visual confirmation** — items just disappear from columns, could feel like a glitch if user blinks | Low | — |
| 2.8 | **RemovePair blocked when `showResult`** — user cannot adjust pairs after seeing results, must reset manually (no reset mechanism exists) | Low | `MatchingExerciseViewModel.kt:111` |

### Strengths
- Only screen that collects effects (`NavigateBack` and `ShowSnackbar`) properly via `LaunchedEffect`
- `SnackbarHost` for error messages
- One of the better-structured screens for effect handling

---

## 3. Drag & Drop Exercise

**Files:** `DragDropExerciseScreen.kt`, `DragDropExerciseViewModel.kt`, `DragDropExerciseContract.kt`

### Layout Structure
```
Scaffold(TopAppBar) → Column
  ├── Loading indicator
  ├── Offline notice
  ├── Error card
  ├── Instructions (ContentRenderer)
  ├── Categories header + LazyColumn (weight 1f)
  ├── Available items header + LazyColumn (weight 0.6f)
  ├── Validate button
  └── Validation result card
```

### Issues

| # | Issue | Severity | File:Line |
|---|-------|----------|-----------|
| 3.1 | **Same hardcoded color inconsistency** — uses `Color(0xFF4CAF50)`/`Color(0xFFFF5722)` with alpha, same as Matching. Timeline uses different green. | Medium | `DragDropExerciseScreen.kt:206-208` |
| 3.2 | **Category highlighting misleading** — categories highlight with `primaryContainer` when ANY item is selected, not when hovering over a valid drop target. Could confuse users into thinking the category itself is selected. | Medium | `DragDropExerciseScreen.kt:179-183` |
| 3.3 | **No drag gesture** — despite the name "Drag & Drop", interaction is tap-to-select then tap-category. This is actually a "tap-to-place" exercise, not drag-and-drop. | Low | — |
| 3.4 | **Available items selected color uses `secondaryContainer`** — but categories use `primaryContainer`. Inconsistent with Matching where left=primaryContainer and right=secondaryContainer. Three different patterns across two files. | Medium | `DragDropExerciseScreen.kt:259` |
| 3.5 | **"Prázdné" (empty) text doesn't disappear after items placed** — category always shows the header + "Prázdné" text if empty, but once items are placed, "Prázdné" remains until recomposition. Actually rechecks fine, but no animation on transition. | Low | `DragDropExerciseScreen.kt:194-199` |
| 3.6 | **Unused imports** — `kotlinx.serialization.decodeFromString`, `encodeToString`, `Json` imported in screen but never used (used in ViewModel) | Very Low | `DragDropExerciseScreen.kt:20-22` |
| 3.7 | **No effect collection** — unlike Matching, DragDrop does not collect effects. Snackbar on validation failure is silent (emitted but never collected). | High | `DragDropExerciseScreen.kt` |
| 3.8 | **No "Reset" button** — same as Timeline; no way to clear all placements | Low | — |

### Strengths
- "Selected item" pattern is clear: user taps an item (highlights), then taps category to place it
- `selectedItem` state provides clear visual feedback of what's about to be placed

---

## 4. Circuit Exercise

**Files:** `CircuitExerciseScreen.kt`, `CircuitExerciseViewModel.kt`, `CircuitExerciseContract.kt`

### Layout Structure
```
Scaffold(TopAppBar) → Column (verticalScroll)
  ├── Loading indicator
  ├── Offline notice (wrong text — says "kontrola vyžaduje internet" but there is no validation)
  ├── Instructions (plain Text, NOT ContentRenderer)
  ├── Component palette (horizontalScroll Row of buttons)
  ├── Playground canvas (320dp height Card + Box + Canvas)
  ├── Connections list (Card)
  ├── Component settings (Card)
  ├── Circuit calculations (Card, conditional)
  └── Clear button
```

### Issues

| # | Issue | Severity | File:Line |
|---|-------|----------|-----------|
| 4.1 | **Instructions rendered as plain `Text`** — unlike all other exercises that use `ContentRenderer`/`parseContentToElements`. Markdown formatting, images, tables, links, and video are NOT supported in circuit instructions. | High | `CircuitExerciseScreen.kt:103` |
| 4.2 | **Offline message is misleading** — says "kontrola vyžaduje internet" but circuit has no server validation. No control requires internet. | Medium | `CircuitExerciseScreen.kt:90-97` |
| 4.3 | **No points awarded** — Circuit is a sandbox with no validation, so users spend time without gamification reward. This may reduce engagement. | Medium | — |
| 4.4 | **No `showResult` or `validationResult` in state** — consistent for a sandbox, but means no feedback mechanism exists at all | Info | `CircuitExerciseContract.kt` |
| 4.5 | **Fixed 320dp playground height** — does not adapt to screen size or number of components; components can easily overflow or be hidden below the visible area | Medium | `CircuitExerciseScreen.kt:146` |
| 4.6 | **Component labels overlap** — `compSizeDp = 80.dp` is fixed. When many components are close together, labels and hit targets overlap. No collision detection in drag handler. | Medium | `CircuitExerciseScreen.kt:150` |
| 4.7 | **No delete/remove gesture on components** — remove is done via the settings panel (X button), not on the canvas itself. The connections list also shows "Smazat" button. This is inefficient for power users. | Low | `CircuitExerciseScreen.kt:314-318` |
| 4.8 | **`OutlinedTextField` in settings loses focus on every keystroke** — `var textVal by remember(comp.id)` resets when component ID changes, but `onValueChange` calls `UpdateComponentValue` on every change AND sets `textVal`. However, the `remember(comp.id)` key means it's stable per component. Actually this works correctly — but the `onValueChange` triggers `reEvaluate()` on every keystroke which could be expensive with many components. | Low | `CircuitExerciseScreen.kt:286-306` |
| 4.9 | **No `contentDescription` for Canvas drawing** — connection lines and live indicators are not accessible to TalkBack | Medium | `CircuitExerciseScreen.kt:161` |
| 4.10 | **"Clear" button is alone in a Row with `Spacer(weight(1f))`** — unnecessary complexity, could just be a standalone Button | Very Low | `CircuitExerciseScreen.kt:357-358` |
| 4.11 | **`NavigateBack` effect unused** — defined in contract, never collected | Low | `CircuitExerciseContract.kt:77` |
| 4.12 | **No `error` field in state** — load errors are silently ignored (`onFailure` only sets `isLoading = false`) | Medium | `CircuitExerciseViewModel.kt:71-73` |

### Strengths
- Advanced real-time circuit physics simulation is impressive
- Brightness/glow effects on components provide rich visual feedback
- Component palette with horizontal scroll is functional
- Settings panel with unit labels (V, Ω, W, μF) is clear

---

## 5. Shared Components

### 5.1 ExerciseButtonsRow

**File:** `ExerciseButtonsRow.kt`

| # | Issue | Severity | File:Line |
|---|-------|----------|-----------|
| 5.1.1 | **Complex branching logic** — 4 states (loading, empty+hasExercises, has exercises, no exercises) in a single composable with nested if/else. Hard to test and maintain. | Medium | `ExerciseButtonsRow.kt:23-57` |
| 5.1.2 | **Disabled loading button** — shows a disabled button with text "Cvičení…" while loading, which is not clickable. This is fine for UX but the text with ellipsis is hardcoded Czech. | Low | `ExerciseButtonsRow.kt:30` |
| 5.1.3 | **All buttons use `tertiary` color** — all exercise buttons are `tertiary`, questions button is `secondary`. If many exercises, the row becomes a sea of same-colored buttons. | Low | `ExerciseButtonsRow.kt:32, 36, 52` |
| 5.1.4 | **No horizontal scroll** — if many exercises exist, buttons will overflow off-screen or wrap (Row does not scroll) | Low | `ExerciseButtonsRow.kt:24` |

### 5.2 FullScreenPointsOverlay

**File:** `FullScreenPointsOverlay.kt`

| # | Issue | Severity | File:Line |
|---|-------|----------|-----------|
| 5.2.1 | **Emoji in code** — `FullScreenMilestoneOverlay` uses 🎉 emoji (line 240) and `FullScreenAchievementOverlay` uses 🏆 emoji (line 476). This may render differently across platforms. | Low | `FullScreenPointsOverlay.kt:240, 476` |
| 5.2.2 | **Code duplication across 4 overlay composables** — `FullScreenPointsOverlay`, `FullScreenMilestoneOverlay`, `FullScreenTotalPointsOverlay`, `FullScreenAchievementOverlay` share ~80% identical animation/state boilerplate. Could be extracted into a shared `AnimatedOverlay` base. | Medium | `FullScreenPointsOverlay.kt:24-539` |
| 5.2.3 | **Hardcoded timing** — 1800ms, 2200ms, 2500ms delays scattered across screens. Exercise screens have their own 2500ms delay (in `LaunchedEffect(showPointsOverlay)`), while overlay internally waits 1800ms. This means the overlay auto-hides after 1800ms internally + 2500ms externally = inconsistent behavior. | Medium | `TimelineExerciseScreen.kt:60`, `FullScreenPointsOverlay.kt:33` |
| 5.2.4 | **No accessibility** — overlay content not announced by TalkBack; no `contentDescription` on animated elements | Medium | `FullScreenPointsOverlay.kt` |

### 5.3 ContentRenderer

**File:** `ContentRenderer.kt`, `ContentFormatter.kt`

| # | Issue | Severity | File:Line |
|---|-------|----------|-----------|
| 5.3.1 | **Unused import** — `Color` imported in `ContentRenderer.kt` but not directly used (only via `linkColor` parameter) | Very Low | `ContentRenderer.kt:20` |
| 5.3.2 | **Deep nesting in paragraph rendering** — `RenderParagraph`, `RenderInlineText`, and `RenderBulletList` have deeply nested if/else chains for addendum handling, URL annotations, and fraction detection. High cyclomatic complexity. | Low | `ContentRenderer.kt:203-372` |
| 5.3.3 | **Hardcoded font sizes** — some elements use `MaterialTheme.typography` (good), but table cells use implicit `bodyMedium` without explicit color (line 465) | Low | `ContentRenderer.kt:465` |

---

## 6. Cross-Cutting Issues

### 6.1 UI Consistency

| # | Issue | Severity | Screens Affected |
|---|-------|----------|------------------|
| 6.1.1 | **3 different "success green" colors** across exercise screens: `Color(0xFFE8F5E9)` Timeline, `Color(0xFF4CAF50).copy(alpha = 0.1f)` Matching/DragDrop, `Color(0xFF1B5E20)` Timeline text, `Color(0xFF4CAF50)` Matching/DragDrop text | High | All 3 validated exercises |
| 6.1.2 | **3 different selection highlight patterns**: Timeline (none), Matching (primaryContainer left / secondaryContainer right), DragDrop (secondaryContainer items / primaryContainer categories) | Medium | Timeline, Matching, DragDrop |
| 6.1.3 | **Result card rendering inconsistency**: Timeline uses `CardDefaults.cardColors()`, Matching/DragDrop use `Color.copy(alpha = 0.1f)` directly on the card container | Medium | All 3 validated exercises |
| 6.1.4 | **Title fallback text inconsistency**: "Timeline cvičení" vs "Matching cvičení" vs "Drag & Drop cvičení" vs "Cvičení: obvod" — some use colon, some use Czech declension | Low | All 4 screens |

### 6.2 Missing Features

| # | Issue | Severity | Screens Affected |
|---|-------|----------|------------------|
| 6.2.1 | **No TTS integration** — `TtsViewModel` is not passed to any exercise screen. Users who rely on text-to-speech cannot have instructions or labels read aloud. | High | All 4 |
| 6.2.2 | **No Reset button** in Timeline, Matching, or DragDrop (only Circuit has Clear) | Low | Timeline, Matching, DragDrop |
| 6.2.3 | **No "score from previous attempt" persistence** — once user navigates away and back, all progress is lost. No state restoration beyond what `rememberSaveable` provides (which is minimal). | Medium | All 4 |
| 6.2.4 | **No pull-to-refresh or retry on load failure** — error card is static; user must navigate back and re-enter. | Low | All 4 |

### 6.3 Code Quality

| # | Issue | Severity | Screens Affected |
|---|-------|----------|------------------|
| 6.3.1 | **Duplicated points awarding logic** (16 lines each) across 3 screens — could be extracted to a shared composable or utility | Medium | Timeline, Matching, DragDrop |
| 6.3.2 | **Duplicated loading/offline/error sections** — ~50 lines nearly identical in each screen | Medium | All 4 |
| 6.3.3 | **Duplicated validate button + result card** — ~50 lines nearly identical in each validated screen | Medium | Timeline, Matching, DragDrop |
| 6.3.4 | **Screen-level `Box(modifier = Modifier.fillMaxSize())` wrapper** serves no purpose in Timeline/Matching/DragDrop — it wraps the entire Scaffold unnecessarily | Low | Timeline, Matching, DragDrop |

### 6.4 Accessibility

| # | Issue | Severity | Screens Affected |
|---|-------|----------|------------------|
| 6.4.1 | **No `contentDescription` on dynamically rendered items** — events, matching items, drag items use plain `Text` that is read by TalkBack but provides no semantic context | Medium | All 4 |
| 6.4.2 | **No keyboard navigation for D-pad** — items are in LazyColumns but no `focusable()` modifiers or `FocusRequester` for TV/keyboard input | Medium | All 4 |
| 6.4.3 | **Canvas elements not accessible** — Circuit components rendered on Canvas with `drawLine`, `drawCircle` etc. are invisible to accessibility services | High | Circuit |

### 6.5 Performance

| # | Issue | Severity | Screens Affected |
|---|-------|----------|------------------|
| 6.5.1 | **`reEvaluate()` called on every interaction in Circuit** — tap, drag, toggle, value change all trigger full graph traversal. With many components, this could cause jank. | Medium | Circuit |
| 6.5.2 | **`shuffled()` in `remember`** — Timeline shuffles available events on every recomposition keyed by `state.availableEvents`. If state is recomposed unnecessarily, shuffle re-runs. Could use `remember` with stable key. | Low | Timeline |
| 6.5.3 | **All 3 validated screens collect `PointsManager.instance.totalPoints`** — this is a StateFlow that triggers recomposition of the entire screen on every points change | Low | Timeline, Matching, DragDrop |

### 6.6 Theming

| # | Issue | Severity | Screens Affected |
|---|-------|----------|------------------|
| 6.6.1 | **Widespread use of hardcoded `Color` values** instead of Material theme colors (see individual issues above) | High | All 4 |
| 6.6.2 | **No dark mode verification** — hardcoded light greens (`0xFFE8F5E9`, `0xFF4CAF50`) will be jarring in dark theme | Medium | Timeline, Matching, DragDrop |
| 6.6.3 | **Surface variant with `alpha = 0f` used as "transparent"** — `MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0f)` at line 150 in ContentRenderer.kt to achieve transparent row backgrounds instead of using `Color.Transparent` or omitting the background entirely. | Low | ContentRenderer |

---

## 7. Priority Recommendations

### P0 — Bugs / Functional Issues

1. **[3.7] DragDrop does NOT collect effects** — `ShowSnackbar` is emitted by ViewModel on validation failure but never collected in the screen. User sees NO error feedback. **Fix:** Add `LaunchedEffect` to collect effects (like Matching does).

2. **[4.1] Circuit instructions as plain Text** — Markdown content (images, tables, formatting) is lost. **Fix:** Use `parseContentToElements` + `ContentRenderer` like the other 3 screens.

3. **[4.2] Circuit offline message wrong** — Says "kontrola vyžaduje internet" but there is no validation. Misleading. **Fix:** Change to "Offline režim: cvičení je plně funkční i offline."

### P1 — Consistency & UX

4. **[6.1.1] Unify result colors across Timeline/Matching/DragDrop** — Use `MaterialTheme.colorScheme.tertiaryContainer` for success, `MaterialTheme.colorScheme.errorContainer` for failure across all 3 screens. Remove all hardcoded `Color(0xFF...)` values.

5. **[6.1.2] Unify selection highlight colors** — Pick one convention: either `primaryContainer` for all selected items, or `primaryContainer` for source items and `secondaryContainer` for target items. Apply consistently.

6. **[6.2.1] Add TTS to exercise screens** — Pass `ttsViewModel` through Navigation or use a shared ViewModel at app level. At minimum, add a "Read instructions aloud" button.

### P2 — Polish

7. **[5.2.3] Fix points overlay timing** — The 2500ms delay in each exercise screen's `LaunchedEffect(showPointsOverlay)` conflicts with the 1800ms internal delay in `FullScreenPointsOverlay`. Unify the timing.

8. **[1.5, 3.8] Add Reset button to Timeline/Matching/DragDrop** — Users should be able to clear their work without navigating away.

9. **[6.3.1] Extract shared exercise UI patterns** — Create a composable for the common "validate button + result card + points overlay" pattern. This would reduce ~80 lines per screen.

### P3 — Nice to Have

10. **[6.2.3] State persistence** — Save exercise state to `SavedStateHandle` so navigating away and back preserves work.

11. **[4.3] Add sandbox scoring for Circuit** — Even without server validation, add a "Check circuit" button that evaluates locally (is the circuit closed? are all required components connected?) and awards participation points.

12. **[5.2.2] Refactor FullScreenOverlays** — Extract shared animation logic into a base composable to reduce duplication across the 4 overlay variants.

---

## File Reference Summary

| File | Lines | Issues |
|------|-------|--------|
| `screens/TimelineExerciseScreen.kt` | 408 | 1.1-1.9 |
| `screens/MatchingExerciseScreen.kt` | 386 | 2.1-2.8 |
| `screens/DragDropExerciseScreen.kt` | 343 | 3.1-3.8 |
| `screens/CircuitExerciseScreen.kt` | 363 | 4.1-4.12 |
| `components/ExerciseButtonsRow.kt` | 59 | 5.1.1-5.1.4 |
| `components/FullScreenPointsOverlay.kt` | 539 | 5.2.1-5.2.4 |
| `components/ContentRenderer.kt` | 602 | 5.3.1-5.3.3 |
| `components/ContentFormatter.kt` | 539 | — |
| `viewmodel/*/TimelineExerciseViewModel.kt` | 123 | — |
| `viewmodel/*/MatchingExerciseViewModel.kt` | 151 | — |
| `viewmodel/*/DragDropExerciseViewModel.kt` | 115 | — |
| `viewmodel/*/CircuitExerciseViewModel.kt` | 296 | — |
| `viewmodel/*/*Contract.kt` | ~40-78 each | — |
| `model/InteractiveExercise.kt` | 152 | — |
