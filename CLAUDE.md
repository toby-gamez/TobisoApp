# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
