# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Governance: Taskino Is the System of Record

Web UI: <http://localhost:8081>. The MCP server is registered in `.mcp.json` (local `node` build at
`~/Projects/external/taskino/mcp/dist/index.js`), and every tool defaults to the project `TobisoApp`.
Tool reference: `~/Projects/external/taskino/mcp/docs/agent-guide.md`.

- **Preflight first,** before any work that creates, changes, or completes something in this
  repo: make a real `list_tasks` call and check that it returns. Seeing the tools listed doesn't
  prove they work. Answering questions or reading code needs no check.
- **If the call fails:** say that Taskino is unreachable, then **stop**. No fallback task file, no
  TODO list, and no carrying on with the plan to file tasks later. **Never start, restart, or
  rebuild Taskino or the MCP build.** They belong to the owner (started via the systemd user
  service `taskino`, or `PORT=8081 NODE_ENV=production mise exec node@22 -- node server/dist/server.js`
  in `~/Projects/external/taskino`). Only the owner can override this, explicitly, and only for
  that conversation.
- **Work from tasks, not from prompts.** When the owner asks for work in a prompt, find the
  matching Taskino task or create one first, then work from it. Put scope, progress and results
  in the task, not only in the chat. Skip this only when the owner explicitly says to work
  without a task.
- **Code modification guard:** change code only when a Taskino task exists in `TobisoApp`
  and the change fits its scope. No task: propose one instead of changing code. Authorized but
  not filed: create the task first.
- Before starting, check whether the task is already partly or fully built. If it's done, say so
  and stop. If it's partial, build only what's missing. Set the task `in_progress` when you
  start, and log progress with `add_note`.
- **Finishing:** run `./gradlew assembleDebug` and `./gradlew test` (plus `./gradlew installDebug`
  only if the owner wants it on the phone; never `connectedAndroidTest`: it uninstalls the app and wipes its data on the phone). Then add a
  note saying what changed and how it was verified, and set the task to `verification`. **Never
  set `done` (or `isDone`) without the user's explicit confirmation.**
- **Channels are not interchangeable:**
  - The **Developer Journal** is lasting engineering context. Write it once the work is verified.
  - `add_note` is progress on one task.
  - **Documents** (HTML) are long-form docs and plans.
- **Identity:** you write as the agent account `claude`. Its key reaches the MCP server
  through `TASKINO_API_KEY` in the gitignored `.claude/settings.local.json`. If your notes show
  **Guest**, the key isn't loaded, so tell the owner. Never copy the key into a tracked file.
- Never link a task to a version unless the owner names one.
- **Roadmap:** what's next lives in Taskino. Read the open tasks in `TobisoApp`, and don't
  keep a roadmap in this file. (`docs/AUDIT_STATUS.md` and `docs/WEB_PARITY_GAPS.md` are history and
  reference, not the task list.)

## Build Commands

```bash
# Debug build
./gradlew assembleDebug

# Release build (requires app/keystore.properties)
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.tobiso.tobisoappnative.domain.usecase.GetAllQuestionsUseCaseTest"

# Install debug APK on connected device
./gradlew installDebug

# Lint check (configured with abortOnError = false)
./gradlew lint
```

## Local Development Setup

Debug credentials go in `local.properties` (never committed):
```
API_USERNAME=admin
API_PASSWORD=secret123
SECURITY_TOKEN_SECRET=<your_secret>
CERT_FINGERPRINT=<sha256_base64_of_signing_cert>
GOOGLE_WEB_CLIENT_ID=<web OAuth client id, same as the server's Google:ClientId>   # optional; empty hides the Google button
```

For release builds, `app/keystore.properties` must also exist with `keyAlias`, `keyPassword`, `storeFile`, `storePassword`.

## Architecture

The app follows **MVI (Model-View-Intent)** with a clean layered architecture:

### MVI Base Classes (`base/`)
Every ViewModel extends `BaseViewModel<S: UiState, I: UiIntent, E: UiEffect>` from `base/BaseViewModel.kt`. State lives in a `StateFlow`, one-shot effects go through a buffered `Channel`. Each screen has a `*Contract.kt` file defining its `*State`, `*Intent`, and `*Effect` sealed types. When adding a new screen, create the contract file first, then extend `BaseViewModel`.

### Dependency Injection (Hilt)
- `TobisoApplication` is the `@HiltAndroidApp` entry point
- `di/AppModule.kt` wires repositories and use cases as `@Singleton`
- `di/DatabaseModule.kt` provides the Room database and DAOs
- All ViewModels use `@HiltViewModel` and are injected via `hiltViewModel()` in Compose

### Data Flow
```
ApiService (Retrofit) ──► Repository ──► UseCase ──► ViewModel ──► Screen
                              │
                         OfflineDataManager (Room cache)
```

Repositories (`repository/`) abstract online vs. offline: they call the API when online and fall back to `OfflineDataManager` (which persists to Room) when offline. `OfflineDataManager` is the single point of contact with the Room database.

### Network Layer (`model/`)
- `ApiClient` — Retrofit singleton using `kotlinx.serialization` as converter (not Gson). Debug builds use system CA trust; release builds go through `SecurityConfig`.
- `ApiService` — All list endpoints return `List<T>` (safe with kotlinx.serialization + R8). **Do not switch back to `Array<T>` or Gson** — the `TypeToken` issue no longer exists with the current serializer.
- `SecurityConfig` — reads credentials from `EncryptedSharedPreferences` / `BuildConfig`. **Never embed production credentials in `BuildConfig`** for release; use the secure store.

### Room Database (`db/`)
Single `AppDatabase` with entities for Category, Post, Question, QuestionPost, Event, Addendum, RelatedPost, Exercise. Schema migrations live in `db/migrations/`.

### Navigation (`navigation/`)
Type-safe Compose Navigation using `@Serializable` route objects (`Routes.kt`). The `NavHost` and all `composable<Route>` entries are in `TobisoApp.kt`. To show the bottom bar on a screen, add its route class to `BOTTOM_BAR_ROUTES` in `Routes.kt` — no other changes needed.

### Gamification Singletons
`PointsManager`, `BackpackManager`, `ShopManager`, `StreakFreezeManager`, `StreakMilestoneManager` are top-level `object` singletons that persist state to `SharedPreferences`. They expose `StateFlow` properties observed in `TobisoApp.kt` for overlay animations (points earned, milestones, achievements).

### Key Patterns
- **TTS**: `TtsManager` / `TtsViewModel` — shared across screens via the `hiltViewModel()` call in `TobisoApp`. The `TtsPlayer` composable is always rendered at the bottom of the app so it persists across navigation.
- **Offline mode**: The `NoInternetScreen` is shown before the `NavHost` renders when there is no connectivity and the user has not dismissed it. `MainViewModel` drives the `isOffline` / `hasUserDismissedNoInternet` state.
- **Content rendering**: Post HTML content is parsed and rendered via `ContentRenderer` / `ContentFormatter` composables in `components/`.

## Testing

Unit tests use **MockK** for mocking, **Turbine** for Flow testing, and `kotlinx-coroutines-test` for coroutine control. Test fakes for manager interfaces are in `app/src/test/.../fake/`. Use `runTest` for all coroutine-based tests.
