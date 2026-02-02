# Interactive components (Tobiso.Web.App) — Notes for Kotlin client (Claude)

This document summarizes the interactive exercise components in `Tobiso.Web.App`, how they work, their config/solution JSON formats and the API calls required. Use these notes to implement a Kotlin-only client that: loads exercises for a post, renders UI, and validates user solutions via the API. Server-side validation exists and Kotlin client should call it for checking.

---

## Overview
- Components used in posts are rendered inside `PostDetail.razor` when exercises exist for the post. The App loads exercises via `IInteractiveExerciseService.GetByPostIdAsync(postId)` and injects each `InteractiveExerciseResponse` into component instances.
- Exercise data shape: `InteractiveExerciseResponse` (see shared DTOs) contains: `Id`, `Title`, `Type`, `ConfigJson`, `InstructionsMarkdown`, `PostIds`, etc.
- API (public) endpoints used by the client:
  - GET /api/InteractiveExercises/post/{postId} — returns array of `InteractiveExerciseResponse` (active exercises for a post)
  - GET /api/InteractiveExercises/{id} — returns single `InteractiveExerciseResponse`
  - POST /api/InteractiveExercises/{id}/validate — body: `ValidateSolutionRequest { UserSolutionJson }` → returns `ExerciseValidationResult` ({ IsCorrect, Score, Feedback, Explanation, DetailedResults? })

Admin endpoints exist (create/update/delete/get-solution) but Kotlin client does not need them for read-and-validate flows.

---

## Components and notes for Kotlin implementation
For each component below: UI behavior, expected `ConfigJson` (what server sends), expected `UserSolutionJson` format to POST to validate, and implementation notes.

### 1) CircuitSimulator
- File: `Components/CircuitSimulator.razor`
- Type value: `circuit`
- Role: fully client-side interactive circuit builder + live simulation. Users drag components onto canvas, connect by clicking two components, double-click to configure, right-click to remove. The simulation (power, current, live connections, warnings) runs locally in component code.
- ConfigJson: not required by the current Blazor implementation (component initializes empty). Server may store `ConfigJson` for prebuilt circuits, but the component code does not parse it by default.
- UserSolutionJson (expected by server validator):
  - { "connections": [ { "from": "comp_1", "to": "comp_2" }, ... ] }
  - Note: validator compares `connections` to `correctConnections` in exercise's `SolutionJson`.
- Validation: server endpoint POST /api/InteractiveExercises/{id}/validate handles `circuit` by comparing connections. Return `ExerciseValidationResult`.
- Kotlin client notes:
  - Implement canvas with drag & drop, component palette, connection drawing and local simulation if desired (simulation can be entirely client-side).
  - For validation, create `connections` JSON from client state and POST to validate endpoint.
  - No special JS interop required beyond UI drawing (on Android/iOS: use native canvas/graphics).

### 2) TimelineExercise
- File: `Components/TimelineExercise.razor`
- Type value: `timeline`
- Role: drag events into chronological slots on a horizontal axis. Users drag items from source list onto slots; they can remove or reorder. Component shows min/max year labels and a timeline visualization.
- ConfigJson (server → `Exercise.ConfigJson`): example structure used in code:
  - {
    "timeRange": { "start": 1300, "end": 1700 },
    "events": [ { "id": "e1", "label": "Event label", "year": 1450 }, ... ]
    }
- UserSolutionJson (client → server validator):
  - { "order": [ "e3", "e1", "e2", ... ] }
  - Server compares against `SolutionJson` containing `correctOrder`.
- Validation: POST /api/InteractiveExercises/{id}/validate returns score and feedback.
- Kotlin client notes:
  - Implement draggable list (source) and droppable timeline slots. Visual slots can be positions determined from `timeRange` or equally spaced — server validates only order IDs.
  - When user checks, POST the `order` array.
  - UI: feedback panel showing `Score`, `IsCorrect`, `Feedback`, optional `Explanation`.

### 3) DragDropExercise
- File: `Components/DragDropExercise.razor`
- Type value: `drag-drop`
- Role: categorize items into buckets. UI shows categories (zones) and a source list. Drag items into categories, click to remove.
- ConfigJson (server side):
  - {
    "categories": [ { "id": "c1", "label": "Category A" }, ... ],
    "items": [ { "id": "i1", "text": "Word" }, ... ]
    }
- UserSolutionJson (client → server validator):
  - { "placements": { "i1": "c2", "i3": "c1", ... } }
  - Server compares to `SolutionJson.correctPlacements` (object mapping itemId → correctCategoryId).
- Validation: POST /api/InteractiveExercises/{id}/validate.
- Kotlin client notes:
  - Provide drag-and-drop or tap-to-select/tap-to-place UX for mobile; maintain internal map itemId→categoryId.
  - When checking, serialize `placements` object and POST.
  - Display score/feedback from server.

### 4) MatchingExercise
- File: `Components/MatchingExercise.razor`
- Type value: `matching`
- Role: pair left items with right items by selecting left + right or via drag connectors. The component stores `pairs` list.
- ConfigJson (server):
  - {
    "left": [ { "id": "l1", "text": "Question 1" }, ... ],
    "right": [ { "id": "r1", "text": "Answer 1" }, ... ]
    }
- UserSolutionJson (client → server validator):
  - { "pairs": [ { "leftId": "l1", "rightId": "r3" }, { "leftId":"l2","rightId":"r7" } ] }
  - Server compares to `SolutionJson.correctPairs`.
- Validation: POST /api/InteractiveExercises/{id}/validate.
- Kotlin client notes:
  - Implement selectable lists (tap left then right to form pair) or drag connectors.
  - Keep `pairs` array and send to server for validation. Display returned feedback.

---

## API details and shapes
- Request header: no special auth needed for public validation and fetching exercises; endpoints are marked AllowAnonymous. Base address configurable for your Kotlin client.

- GET /api/InteractiveExercises/post/{postId}
  - Response: 200 JSON array of `InteractiveExerciseResponse` (see shared DTO). Each contains `ConfigJson` (string) and `Id`, `Title`, `Type`, `InstructionsMarkdown`.

- POST /api/InteractiveExercises/{id}/validate
  - Body: JSON matching `ValidateSolutionRequest`: { "userSolutionJson": "<stringified JSON>" }
  - Important: server expects a string property `UserSolutionJson` which itself is a serialized JSON string matching the formats above. In the Blazor client they do: new ValidateSolutionRequest { UserSolutionJson = JsonSerializer.Serialize(userSolution) }.
  - Response: `ExerciseValidationResult` JSON: { "isCorrect": bool, "score": int, "feedback": string, "explanation": string? }

Implementation note: your Kotlin client can either send the nested JSON as a string field (mimic ValidateSolutionRequest) or call the endpoint with the same field structure. Example (pseudocode):
- build userSolution object (Map/array)
- serialize to string userSolutionJson
- POST { "userSolutionJson": "<json-string>" }

Server-side validator expects particular property names inside the inner JSON depending on exercise type (see earlier sections: `connections`, `order`, `placements`, `pairs`).

---

## UI / Behavior summary & priorities for Kotlin client
- Minimum viable feature set for each exercise:
  1. Load `InteractiveExerciseResponse.ConfigJson` and parse it.
  2. Render basic UI for exercise type (visuals can be native): drag/drop or tap interactions.
  3. Maintain local `userSolution` data structure matching server expectation.
  4. Call POST validate endpoint and show `Score`, `Feedback` and `Explanation`.

- Prioritization suggestions for Claude (Kotlin implementation plan):
  - Implement `drag-drop`, `timeline`, `matching` first (they follow similar pattern: load config, allow placing/pairing, POST validate).
  - Implement `circuit` after — it involves canvas drawing and local simulation; validation only needs `connections` list.

---

## Edge cases and testing tips for the Kotlin client
- Ensure IDs used in `ConfigJson` (item/event IDs) are preserved exactly when building `userSolution` arrays/objects.
- When serializing the `userSolution` for `ValidateSolutionRequest`, make sure to produce a string field `userSolutionJson` containing the serialized nested JSON (server expects string property named `UserSolutionJson`).
- Example flows to test:
  - Timeline: post with 4 events → shuffle on client → ordering → POST order array → expect score based on matching positions.
  - Drag-drop: ensure empty categories are allowed and placements object omits unplaced items or includes them as null (server expects property presence for items in `correctPlacements`). Prefer to include only placed items.
  - Matching: ensure duplicate pairs are prevented client-side (server expects unique leftIds).

---

## Files referenced in repo (implementation hints)
- `Tobiso.Web.App/Components/CircuitSimulator.razor` — full circuit UX + local simulation
- `Tobiso.Web.App/Components/TimelineExercise.razor` — drag-to-slot timeline
- `Tobiso.Web.App/Components/DragDropExercise.razor` — drag items into categories
- `Tobiso.Web.App/Components/MatchingExercise.razor` — left/right pairing UI
- `Tobiso.Web.Shared/DTOs/InteractiveExerciseDTOs.cs` — DTOs and `ValidateSolutionRequest` / `ExerciseValidationResult`
- `Tobiso.Web.App/Controllers/InteractiveExercisesController.cs` — public endpoints and routes
- `Tobiso.Web.Api/Services/InteractiveExerciseService.cs` — server-side validation logic and expected inner JSON shapes

---

If you want, I can now:
- produce Kotlin data classes for `InteractiveExerciseResponse`, `ValidateSolutionRequest` and `ExerciseValidationResult` and example API client code (Ktor or Retrofit), or
- produce UI skeleton snippets for each exercise type in Kotlin (Android Compose or multiplatform), or
- convert `ConfigJson` examples to typed Kotlin models for each exercise type.

Which of these should I generate next? (I can start with API client + DTOs.)
