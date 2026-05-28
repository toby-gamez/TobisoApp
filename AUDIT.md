# COMPREHENSIVE CODEBASE AUDIT: TobisoAppNative

**Date**: May 28, 2026  
**App Version**: 3.0.2  
**Target SDK**: 36 (Android 16)  
**Min SDK**: 24  
**Total Source Files**: ~110 Kotlin files + resources

---

## Scoring Summary

| Area | Score | Rationale |
|------|-------|-----------|
| **Architecture** | 6/10 | Solid MVI + Clean Layer foundation but anemic domain layer, god class, no SavedStateHandle |
| **Kotlin Quality** | 6/10 | Generally good idiom usage but MutableStateFlow races, fragile singletons, nullable chains |
| **Android-Specific** | 5/10 | No process death handling, AlarmManager deprecation, TTS init on main thread |
| **Security** | 4/10 | Trivial integrity bypass, exposed FileProvider, no cert pinning, SharedPreferences for game state |
| **Performance** | 5/10 | Full-list loads, no pagination, Calendar object churn, DateSerializer inefficiency |
| **Dependencies** | 7/10 | Modern stack but kotlin-reflect bloat, no version catalog verification |
| **UI/UX** | 6/10 | Functional but hardcoded subjects, SelectionContainer per-item, fixed card heights |
| **Networking** | 5/10 | No offline feedback queue, cert pinning disabled, GitHub API without auth |
| **Database** | 4/10 | JSON blobs break normalization, no migration tests, delete+insert without transaction |
| **Testing** | 3/10 | Only 6 test files for 80+ source files, no ViewModel/UI/integration tests |
| **Production Readiness** | 5/10 | Weak test coverage, no migration tests, Timber not stripped, legacy storage permission |
| **Overall** | **5.1/10** | Good foundation but significant gaps in security, testing, and scalability |

---

## Severity Legend

| Severity | Meaning |
|----------|---------|
| **CRITICAL** | Will cause crash, data loss, or security breach in production |
| **HIGH** | Significant risk to stability, security, or user experience |
| **MEDIUM** | Important concern but limited immediate impact |
| **LOW** | Minor issue or best-practice violation |

---

## 1. ARCHITECTURE AUDIT

### 1.1 CRITICAL: `FeedbackDto` missing `@Serializable` annotation — runtime crash

**File**: `app/src/main/java/com/tobiso/tobisoappnative/model/FeedbackDto.kt:3-8`  
**Issue**: `FeedbackDto` is a plain `data class` without `@Serializable` annotation, but `ApiService.sendFeedback()` at `ApiService.kt:81` passes it via Retrofit using `kotlinx.serialization` converter. This will throw `SerializationException` at runtime.  
**Fix**: Add `@Serializable` to `FeedbackDto`.

```kotlin
@Serializable
data class FeedbackDto(
    val name: String,
    val email: String,
    val message: String,
    val platform: String
)
```

### 1.2 HIGH: Anemic domain layer — use cases are pure pass-throughs

**Files**: `domain/usecase/GetAllQuestionsUseCase.kt:7-9`, `GetExerciseUseCase.kt:7-8`, `ValidateExerciseUseCase.kt:7-11`  
**Issue**: All three use cases are one-line delegations to repository, containing zero business logic, validation, error transformation, or logging. This is an anemic domain model anti-pattern — the domain layer adds no value.  
**Fix**: Move business rules into use cases (e.g., validate exercise IDs, transform errors to domain-specific sealed results, enforce business constraints).

### 1.3 HIGH: `OfflineRepositoryImpl` is a monolithic god class

**File**: `repository/OfflineRepositoryImpl.kt` (239 lines)  
**Issue**: `downloadAllData()` / `downloadAndSaveRemaining()` is a massive method handling ALL data types (categories, posts, questions, exercises, addendums, events, grades). It has hardcoded progress percentages, manual concurrency with `Semaphore`, and duplicated retry logic. Single responsibility violated.  
**Fix**: Split into dedicated fetchers per data type, or use a WorkManager-based sync pipeline with proper progress reporting.

### 1.4 MEDIUM: ViewModel does not use `SavedStateHandle`

**Files**: `MainViewModel.kt`, `CalendarViewModel.kt`, `HomeViewModel.kt` (all ViewModels)  
**Issue**: No ViewModel uses `SavedStateHandle`. After process death, all state is lost. While `rememberSaveable` in Compose preserves some UI state, the ViewModel data (loaded posts, questions, etc.) must be re-fetched. Combined with offline-first design, this can result in empty screens after process death.  
**Fix**: Inject `SavedStateHandle` and persist critical navigation/loading state.

### 1.5 MEDIUM: Duplicate repository interfaces

**Files**: `repository/PostsRepository.kt`, `repository/PostDetailRepository.kt`  
**Issue**: `PostsRepository.getPost()` and `PostDetailRepository.getPostDetail()` both fetch a single post with identical logic. Similarly `getQuestionsForPost` appears in both `QuestionsRepository` and `PostDetailRepository`. This violates DRY.  
**Fix**: Consolidate into a single PostRepository with focused methods; eliminate duplication.

### 1.6 LOW: `SnippetsRepository.kt` is an empty file

**File**: `repository/SnippetsRepository.kt`  
**Issue**: Empty file with no content. Dead code / left-over from refactoring.  
**Fix**: Remove the file.

---

## 2. KOTLIN CODE QUALITY AUDIT

### 2.1 HIGH: `MutableStateFlow.value` direct assignment instead of `update()`

**Files**: `PointsManager.kt` lines 38-43, 52-55, 65-68, etc.  
**Issue**: Throughout `PointsManager`, state is mutated with `_totalPoints.value = points` (direct assignment). This is NOT thread-safe — when multiple coroutines access the manager concurrently (e.g., milestone + achievement firing simultaneously), one update may be lost.  
**Fix**: Use `_totalPoints.update { points }` for atomic updates.

```kotlin
// BAD — not thread-safe
_totalPoints.value = points

// GOOD — atomic update
_totalPoints.update { points }
```

### 2.2 MEDIUM: Double-checked locking with nullable volatile

**Files**: `PointsManager.kt:153-166`, `StreakFreezeManager.kt:105-118`, `ShopManager.kt:137-150`, `BackpackManager.kt:114-127`, `IconPackManager.kt:108-121`, `EncryptionManager.kt:27-35`  
**Issue**: All manager singletons use the double-checked locking pattern with `@Volatile` on a nullable type. While technically correct, this pattern has subtle JMM visibility edge cases and is fragile.  
**Fix**: Use `by lazy` or make them `@Singleton` injected via Hilt instead of manual singletons.

### 2.3 MEDIUM: Companion object delegations create dual access paths

**Files**: `PointsManager.kt:168-187`, `ShopManager.kt:152-164`, etc.  
**Issue**: Each manager defines companion delegations like `val totalPoints get() = instance.totalPoints`. This creates two access paths (direct companion call and via `.instance`), adding confusion.  
**Fix**: Remove companion delegations; force usage through the `instance` property.

### 2.4 LOW: `EncryptionManager.encrypt()`/`decrypt()` return nullable

**File**: `security/EncryptionManager.kt:69-111`  
**Issue**: Both methods return `String?` on failure, but callers use `.orEmpty()` or just trust the result. If encryption fails (KeyStore issue, device migration), data is silently lost.  
**Fix**: Return `Result<String>` or throw domain-specific exception.

---

## 3. ANDROID-SPECIFIC AUDIT

### 3.1 CRITICAL: `AlarmManager.setRepeating()` — deprecated and imprecise

**File**: `NotificationScheduler.kt:41-46`, `NotificationScheduler.kt:74-79`  
**Issue**: `AlarmManager.setRepeating()` is deprecated since API 19 (KitKat). From Android 12+, alarms scheduled with `setRepeating()` may be delayed or batched. Streak notification alarms may fire unpredictably.  
**Fix**: Use `WorkManager` with `PeriodicWorkRequest` instead of AlarmManager + BroadcastReceiver for daily notifications.

```kotlin
// BAD — deprecated, imprecise
alarmManager.setRepeating(
    AlarmManager.RTC_WAKEUP,
    calendar.timeInMillis,
    AlarmManager.INTERVAL_DAY,
    pendingIntent
)

// GOOD — use WorkManager
val dailyRequest = PeriodicWorkRequestBuilder<NotificationWorker>(1, TimeUnit.DAYS)
    .setInitialDelay(calculateDelayTo(hour, minute), TimeUnit.MILLISECONDS)
    .build()
WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "notification_work",
    ExistingPeriodicWorkPolicy.KEEP,
    dailyRequest
)
```

### 3.2 HIGH: No process death handling for gamification state

**Files**: `PointsManager.kt`, `StreakFreezeManager.kt`, `ShopManager.kt`  
**Issue**: The managers load state in `init {}` from `SharedPreferences`, but the `TobisoApplication` initializes them in `onCreate()`. If the process dies and restores, the ViewModel `StateFlow`s were not persisted, so the UI briefly shows stale state.  
**Fix**: Use `SavedStateHandle` in ViewModels for critical UI state, or expose loading states.

### 3.3 MEDIUM: `TtsManager` creates TTS engine synchronously in constructor

**File**: `tts/TtsManager.kt:56-58` + `viewmodel/tts/TtsViewModel.kt:12`  
**Issue**: `TtsManager` constructor calls `initializeTts()` synchronously, creating the `TextToSpeech` engine on the calling thread. TTS engine initialization involves disk I/O and can cause jank. When `hiltViewModel()` creates `TtsViewModel` in `TobisoApp`, this blocks the composition.  
**Fix**: Use `viewModelScope.launch` to initialize TTS lazily.

### 3.4 MEDIUM: Multiple LaunchedEffects with shared mutable state race

**File**: `TobisoApp.kt:108-114`, `152-209`  
**Issue**: Mutable state variables (`showOverlay`, `showMilestoneOverlay`, `showAchievementOverlay`) are controlled by separate `LaunchedEffect` blocks that race with each other. `LaunchedEffect(lastMilestone)` and `LaunchedEffect(lastAchievement)` may both fire concurrently, both calling `PointsManager.resetLastAddedPoints()`, causing a race where one overlay steals the other's points.  
**Fix**: Consolidate into a single effect processor that queues overlays sequentially.

---

## 4. SECURITY AUDIT

### 4.1 CRITICAL: `SecurityConfig` integrity check is trivially bypassable

**File**: `config/SecurityConfig.kt:55-61`  
**Issue**: `verifyAppIntegrity()` compares the APK signing certificate's SHA-256 fingerprint against `BuildConfig.CERT_FINGERPRINT`. Since `CERT_FINGERPRINT` is a compile-time constant embedded in the APK, a repackager can recompile with their own fingerprint or patch the check. This provides no real security — it is security theater.  
**Fix**: Either remove the check or use Play Integrity API for actual app integrity verification.

### 4.2 CRITICAL: `file_paths.xml` exposes entire external storage

**File**: `res/xml/file_paths.xml:3-4`  
**Issue**: `<external-path name="external_files" path="." />` grants the `FileProvider` access to the entire external storage directory. Combined with `grantUriPermissions="true"`, any app receiving a content URI from this provider can read arbitrary files.  
**Fix**: Restrict the path to a specific subdirectory.

```xml
<!-- BAD — exposes everything -->
<external-path name="external_files" path="." />

<!-- GOOD — restrict to specific directory -->
<external-files-path name="downloads" path="Download/" />
<external-cache-path name="cache" path="pdf/" />
```

### 4.3 HIGH: Gamification SharedPreferences included in cloud backup

**File**: `res/xml/data_extraction_rules.xml`  
**Issue**: `StreakData.xml`, `streak_freeze_prefs.xml`, `shop_prefs.xml`, `backpack_prefs.xml`, and `points_prefs.xml` are all included in cloud backup. On a rooted device or via ADB restore, an attacker could restore a known-good state to gain unlimited points/freezes/items.  
**Fix**: Exclude gamification SharedPreferences from backup, or move to DataStore with backup disabled.

### 4.4 MEDIUM: ProGuard keeps too many classes un-obfuscated

**File**: `proguard-rules.pro`  
**Issue**: Sweeping rules like `-keep class androidx.compose.** { *; }`, `-keep class okhttp3.** { *; }`, `-keep class coil.** { *; }` prevent obfuscation of entire libraries, increasing APK size and making reverse engineering easier.  
**Fix**: Use targeted `-keep` rules only for specific classes/methods needed at runtime. Remove blanket rules.

### 4.5 MEDIUM: Timber logs not stripped in production builds

**File**: `proguard-rules.pro:149-156`  
**Issue**: The ProGuard rules strip `android.util.Log` calls, but the app uses `Timber`. Timber uses `Log` internally, so `Timber.e()` still produces output. No `ReleaseTree` is planted for crash reporting.  
**Fix**: Plant a crash-reporting tree (e.g., Firebase Crashlytics) in release builds. Add Timber-specific stripping rules if needed.

### 4.6 LOW: Emulator detection is trivial to bypass

**File**: `config/SecurityConfig.kt:157-174`  
**Issue**: The `isRunningOnEmulator()` check uses build properties that any emulator can set arbitrarily. Provides no real security.  
**Fix**: Remove or replace with Play Integrity API device verification.

---

## 5. PERFORMANCE AUDIT

### 5.1 HIGH: `getCachedExercisesByPostId` loads ALL exercises then filters in-memory

**File**: `model/OfflineDataManager.kt:295-299`  
**Issue**: `getCachedExercisesByPostId()` calls `getCachedExercises()` which loads ALL exercise entities from Room, then filters in Kotlin memory. With hundreds of exercises, this wastes memory and IO. The `postIdsJson` and `categoryIdsJson` are JSON blobs — Room cannot query inside them efficiently.  
**Fix**: Add a proper many-to-many join table `exercise_post` and `exercise_category` for indexed querying.

### 5.2 MEDIUM: No pagination for any list queries

**Files**: `db/dao/PostDao.kt`, `QuestionDao.kt`, `ExerciseDao.kt`, etc.  
**Issue**: All DAO queries return complete `List<T>` with no `LIMIT`/`OFFSET`. Posts, questions, and exercises are loaded entirely into memory. For an app with thousands of items, this will cause OOM on low-end devices.  
**Fix**: Implement pagination with `LIMIT :pageSize OFFSET :offset` and expose `PagingSource` for Compose `collectAsLazyPagingItems()`.

### 5.3 MEDIUM: `DateSerializer` creates new `SimpleDateFormat` per call

**File**: `model/DateSerializer.kt:16`  
**Issue**: `formatter()` creates a new `SimpleDateFormat` instance on every serialize/deserialize call. `SimpleDateFormat` is expensive to create.  
**Fix**: Use `java.time.format.DateTimeFormatter` (immutable and thread-safe) since `minSdk = 24` and desugaring is enabled.

```kotlin
object DateSerializer : KSerializer<Date> {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
    // ...
}
```

### 5.4 MEDIUM: `generateRecurringInstances` uses mutable `Calendar` with per-iteration allocations

**File**: `model/LocalEventManager.kt:141-181`  
**Issue**: Uses `Calendar.getInstance()` which is allocation-heavy. Each `calendar.time` getter creates a new `Date`. For yearly events spanning decades, this generates hundreds of `Date` objects.  
**Fix**: Use `java.time.LocalDate` with `Period` for date arithmetic.

### 5.5 MEDIUM: `OfflineRepositoryImpl` creates N async tasks for N posts

**File**: `repository/OfflineRepositoryImpl.kt:194-210`  
**Issue**: The `coroutineScope` block creates `async` tasks for EVERY post to fetch exercises concurrently. Even with `Semaphore(10)`, the overhead of creating N coroutine objects is significant. For 200 posts, this creates 200 coroutines.  
**Fix**: Use `channelFlow` or a bounded `mapAsync` pattern to create at most `MAX_CONCURRENT` coroutines.

---

## 6. DEPENDENCY & GRADLE AUDIT

### 6.1 MEDIUM: `kotlin-reflect:2.2.10` adds ~2.5 MB to APK

**File**: `app/build.gradle.kts:144`  
**Issue**: `kotlin-reflect` is added "for kotlinx.serialization runtime lookups used by retrofit serializer." However, kotlinx.serialization uses compiler-generated serializers and shouldn't need reflection at runtime.  
**Fix**: Verify whether kotlin-reflect is actually needed. If not, remove it.

### 6.2 MEDIUM: Room schema export disabled (`exportSchema = false`)

**File**: `db/AppDatabase.kt:38-39`  
**Issue**: `exportSchema = false` means no schema JSON is generated. This disables automated migration testing and makes it impossible to verify migration correctness across versions.  
**Fix**: Set `exportSchema = true`, configure `room.schemaLocation` in `build.gradle.kts`.

### 6.3 MEDIUM: Version catalog not verified

**File**: Project root — `gradle/libs.versions.toml`  
**Issue**: The project uses `libs.plugins.*` and `libs.*` notation which requires a version catalog at `gradle/libs.versions.toml`. Build success depends on this file existing and being correctly configured.  
**Fix**: Verify the version catalog exists and is up to date. Publish it to auditors.

---

## 7. UI/UX AUDIT

### 7.1 MEDIUM: `SelectionContainer` wrapped per-item breaks text selection

**File**: `screens/PostDetailScreen.kt:406-407`  
**Issue**: Each item in `LazyColumn` is wrapped in its own `SelectionContainer`. This creates N containers for N elements, making cross-element text selection impossible.  
**Fix**: Wrap the entire `LazyColumn` or parent `Column` in a single `SelectionContainer`.

### 7.2 MEDIUM: Hardcoded subjects and colors in HomeScreen

**File**: `screens/HomeScreen.kt:151-164`, `187-197`  
**Issue**: Subject names and colors are hardcoded. If the server adds a new subject, the app must be updated to show it. The subject grid should be driven by API `categories` data.  
**Fix**: Drive the subject grid from API categories, with dynamic color assignment.

### 7.3 LOW: Fixed card height may truncate text with large font scaling

**File**: `screens/HomeScreen.kt:468`  
**Issue**: `.height(100.dp)` is fixed. On devices with accessibility font scaling, text may be truncated.  
**Fix**: Use `heightIn(min = ...)` or let the card wrap content.

### 7.4 LOW: Column count doesn't consider foldable/multi-window

**File**: `screens/HomeScreen.kt:139-146`  
**Issue**: Column count based on screenWidthDp only — doesn't handle foldable hinge or multi-window mode changes.  
**Fix**: Use `WindowSizeClass` from Material 3 adaptive library.

---

## 8. NETWORKING AUDIT

### 8.1 HIGH: No offline queue for feedback

**File**: `model/ApiService.kt:81`  
**Issue**: `sendFeedback()` is a direct network call with no offline queuing. If the user is offline, feedback data is lost silently.  
**Fix**: Implement an offline feedback queue using Room + WorkManager.

### 8.2 MEDIUM: Certificate pinning disabled

**File**: `model/ApiClient.kt:24-25`  
**Issue**: Certificate pinning was removed "per request." The log says "Certificate pinning disabled; using system trust anchors." The `CERT_PINS` and `CERT_PINS_BACKUP` build config fields exist but are unused.  
**Fix**: Re-implement certificate pinning via OkHttp `CertificatePinner` using the existing build config fields.

### 8.3 MEDIUM: `fetchLatestVersionFromGithub` unauthenticated

**File**: `UpdateCheckWorker.kt:76-91`  
**Issue**: Calls `api.github.com/.../releases/latest` without authentication. GitHub API rate limit for unauthenticated requests is 60 req/hour per IP. Uses raw `HttpURLConnection` instead of OkHttp.  
**Fix**: Use OkHttp, add `User-Agent` header, and cache the result for at least 24 hours.

### 8.4 LOW: Hardcoded `BASE_URL` vs `BuildConfig.API_BASE_URL`

**File**: `model/ApiClient.kt:15`  
**Issue**: `ApiClient.kt` hardcodes `BASE_URL` while `build.gradle.kts` defines `API_BASE_URL` in build config. The build config field is never used.  
**Fix**: Use `BuildConfig.API_BASE_URL` for consistency and per-environment configuration.

---

## 9. DATABASE AUDIT

### 9.1 HIGH: JSON blobs for related data (anti-pattern)

**Files**: `CategoryEntity.kt` (parentJson, childrenJson), `ExerciseEntity.kt` (postIdsJson, categoryIdsJson), `PostEntity.kt` (versionsJson)  
**Issue**: Storing serialized JSON in Room columns breaks relational integrity, prevents indexed queries, and makes migration difficult. If a category name changes on the server, the offline cache contains stale data.  
**Fix**: Normalize the schema — use foreign keys and join tables.

```sql
-- Instead of postIdsJson TEXT in exercises:
CREATE TABLE exercise_post (
    exerciseId INTEGER REFERENCES exercises(id),
    postId INTEGER REFERENCES posts(id),
    PRIMARY KEY (exerciseId, postId)
)
```

### 9.2 MEDIUM: No database migration tests

**File**: `db/AppDatabase.kt:30-65` (migrations defined)  
**Issue**: Two migrations (1→2, 2→3) exist but no migration tests are present. An oversight in a migration could corrupt user data for thousands of users.  
**Fix**: Add Room migration tests using `MigrationTestHelper`.

### 9.3 MEDIUM: `deleteAll()` + `insertAll()` without transaction in Phase 1

**File**: `model/OfflineDataManager.kt:78-81`  
**Issue**: `saveCategoriesAndPosts()` calls `categoryDao.deleteAll()` and `postDao.deleteAll()` followed by inserts but is NOT wrapped in `db.withTransaction`. If the app crashes between delete and insert, the cache is in an inconsistent state.  
**Fix**: Wrap all delete+insert pairs in `@Transaction`.

### 9.4 LOW: Unused `count()` methods in DAOs

**Files**: `CategoryDao.kt:21`, `PostDao.kt:27`, `QuestionDao.kt:24`, `QuestionPostDao.kt:21`  
**Issue**: `count()` methods exist but are not called anywhere. Dead code.  
**Fix**: Remove unused DAO methods.

---

## 10. TESTING AUDIT

### 10.1 HIGH: Severely inadequate test coverage

**Test classes**: `ExampleUnitTest.kt`, `GetAllQuestionsUseCaseTest.kt`, `GetExerciseUseCaseTest.kt`, `ValidateExerciseUseCaseTest.kt`, `PointsManagerTest.kt`, `StreakFreezeManagerTest.kt`  
**Issue**: Only 6 test classes for 80+ source files (~7.5% coverage).  

**Untested critical components**:
- All 6 ViewModels (Main, Calendar, Home, PostDetail, Tts, AiChat)
- All repository implementations (5 repos)
- All 9 DAOs (no Room integration tests)
- `OfflineDataManager` (336 lines, zero tests)
- `OfflineRepositoryImpl` (239 lines, zero tests)
- `SecurityConfig`, `EncryptionManager`
- All notification workers (3 workers, 2 receivers)
- All Compose UI (no compose UI tests)
- Migration tests (none)
- Navigation tests (none)

**Fix**: Prioritize ViewModel and repository tests. Add Room integration tests.

### 10.2 MEDIUM: Fakes are not injectable into production code

**Files**: `fake/FakePointsManager.kt`, `fake/FakeStreakFreezeManager.kt`  
**Issue**: The fakes implement interfaces (`IPointsManager`, `IStreakFreezeManager`), but production code calls the singleton objects statically (e.g., `PointsManager.addPoints()`). The fakes cannot be injected into ViewModels because ViewModels bypass the interfaces.  
**Fix**: Inject manager interfaces into ViewModels via Hilt constructor injection.

---

## 11. PLAY STORE & PRODUCTION READINESS

### 11.1 HIGH: `WRITE_EXTERNAL_STORAGE` permission declared but ineffective on modern Android

**File**: `AndroidManifest.xml:15`, `screens/PostDetailScreen.kt:287-301`  
**Issue**: The permission `WRITE_EXTERNAL_STORAGE` (up to API 28) does not work on Android 10+ with scoped storage. The code conditionally uses scoped storage but the manifest still declares the legacy permission, which may trigger Play Store scrutiny.  
**Fix**: Remove `WRITE_EXTERNAL_STORAGE` from manifest. Use `MediaStore.Downloads` for PDF saving on all API levels.

### 11.2 MEDIUM: No crash reporting in production

**File**: `TobisoApplication.kt:13-14`  
**Issue**: Timber is only planted with `DebugTree` in debug builds. In production, no crash reporting is configured. Crashes are invisible to developers.  
**Fix**: Integrate Firebase Crashlytics and plant a Crashlytics tree in release builds.

### 11.3 MEDIUM: `android:largeHeap="false"` may cause OOM on low-end devices

**File**: `AndroidManifest.xml:30`  
**Issue**: While good practice, the app loads full post content (potentially large HTML with embedded images) into memory. On devices with 1-2 GB RAM, this can cause OOM.  
**Fix**: Monitor crash-free rate before adjusting. Consider virtualized list rendering for post content.

### 11.4 LOW: Android 15 edge-to-edge readiness

**File**: `MainActivity.kt:31`  
**Issue**: `enableEdgeToEdge()` is called, but not all screens verify proper `WindowInsets` handling, especially `imePadding()` on screens with text input (AiChatScreen, FeedbackScreen).  
**Fix**: Audit all screens for proper system bar inset handling.

---

## 12. PRIORITIZED REMEDIATION ROADMAP

### Quick Wins (1-2 days)

| # | Fix | File(s) | Effort | Impact |
|---|-----|---------|--------|--------|
| 1 | Add `@Serializable` to `FeedbackDto` | `model/FeedbackDto.kt` | 5 min | Prevents runtime crash |
| 2 | Remove empty `SnippetsRepository.kt` | `repository/SnippetsRepository.kt` | 1 min | Dead code cleanup |
| 3 | Add `@Transaction` to `saveCategoriesAndPosts` | `model/OfflineDataManager.kt` | 10 min | Prevents cache corruption |
| 4 | Add Room `@Transaction` annotation | `model/OfflineDataManager.kt:78` | 5 min | Data consistency |
| 5 | Remove unused `count()` DAO methods | All DAO files | 10 min | Dead code cleanup |

### Medium Effort (1-2 weeks)

| # | Fix | Impact |
|---|------|--------|
| 6 | Replace `AlarmManager.setRepeating()` with WorkManager `PeriodicWorkRequest` | Reliable notifications on Android 12+ |
| 7 | Restrict FileProvider path to specific subdirectory | Close path traversal vulnerability |
| 8 | Normalize Room schema: extract JSON blobs to join tables | Query performance, data integrity |
| 9 | Implement pagination in DAOs (LIMIT/OFFSET) | OOM prevention on large datasets |
| 10 | Add `SavedStateHandle` to all ViewModels | Process death resilience |
| 11 | Implement offline feedback queue (Room + WorkManager) | No user data loss |
| 12 | Add Room migration tests | Prevent data corruption on upgrade |
| 13 | Certificate pinning via `CertificatePinner` | Network security |

### High-Impact Refactors (2-4 weeks)

| # | Refactor | Impact |
|---|----------|--------|
| 14 | Split `OfflineRepositoryImpl` into dedicated data sync services | Maintainability, testability |
| 15 | Move business logic into use cases (stop being pass-throughs) | Domain layer value, testability |
| 16 | Inject manager interfaces via Hilt instead of static singletons | Testability, DI consistency |
| 17 | Replace `Date`/`Calendar` with `java.time` everywhere | Thread safety, performance |
| 18 | Drive subject grid from API categories instead of hardcoded values | Server-driven UI, no app update needed |

### Long-Term Architectural Recommendations (1-3 months)

| # | Recommendation |
|---|---------------|
| 19 | Modularize the app by feature (post, quiz, calendar, shop) to improve build time and team scalability |
| 20 | Move to full offline-first sync with WorkManager background sync and conflict resolution |
| 21 | Integrate Firebase Crashlytics + Performance Monitoring for production observability |
| 22 | Implement server-side gamification (not SharedPreferences) for points/shop/streaks |
| 23 | Migrate from manual MVI to a standardized framework like Orbit MVI or Circuit |
| 24 | Add CI/CD pipeline with lint, test, and build verification on every PR |
| 25 | Implement Play Integrity API for real app integrity verification (remove security theater) |
| 26 | Add UI screenshot tests with Paparazzi or Roborazzi for visual regression detection |

---

*This audit was generated by automated codebase analysis. All findings reference actual source files and line numbers. The remediation roadmap is ordered by impact vs. effort priority.*
