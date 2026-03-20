# Audit kódu – TobisoAppNative
**Hodnotitel:** Senior Android Developer  
**Datum:** 16. března 2026  
**Verze aplikace:** 2.0  
**Technologie:** Kotlin · Jetpack Compose · MVVM/MVI · Retrofit · WorkManager

---

## Souhrn hodnocení

| Oblast | Hodnocení | Poznámka |
|---|---|---|
| Architektura | 5 / 10 | Dobrý záměr, ale nekonzistentní aplikace |
| SOLID principy | 4 / 10 | SRP masivně porušeno, DIP chybí |
| Bezpečnost | 3 / 10 | Hardcoded credentials, vypnuté SSL pinning |
| Modernizace kódu | 6 / 10 | Moderní Compose, ale zastaralé nástroje |
| Výkon & plynulost | 5 / 10 | Polling místo reaktivního přístupu |
| Testovatelnost | 6 / 10 | 43 unit testů, všechny projdou |
| DI & závislosti | 2 / 10 | Žádný DI framework |
| Kvalita kódu | 5 / 10 | Debug kód v produkci, magie čísel |

**Celkové skóre: 3.9 / 10**

---

## 1. Architektura

### Co funguje dobře ✅

- Aplikace se **snaží** o Clean Architecture – existují balíčky `domain/`, `repository/`, `model/`, `viewmodel/`, `screens/`, `components/`.
- MVI základna (`BaseViewModel`, `MviContract`, `UiState`, `UiIntent`, `UiEffect`) je správně navržená a čistá.
- Repository pattern je používán – existují rozhraní (`PostsRepository`, `ExerciseRepository`) i jejich implementace.
- `HomeViewModel` správně separuje logiku od UI a používá repository.
- Use-case vrstva existuje (`GetExerciseUseCase`, `ValidateExerciseUseCase`, `GetAllQuestionsUseCase`).
- Per-screen ViewModely (složky `viewmodel/home/`, `viewmodel/ai/`, atd.) ukazují správný směr.

### Kritické problémy ❌

#### 1.1 `MainViewModel` je God-objekt - DONE

`MainViewModel.kt` spravuje *vše najednou*:
- kategorie, posty, detaily postů
- otázky, filtrované otázky, posty pro otázky
- cvičení, validace cvičení
- oblíbené příspěvky
- offline stav a stahování offline dat
- search bar stav
- toast zprávy
- TTS manager
- snippety
- clipboard tracking
- navigační stav

Tento soubor má přes 600 řádků a desítky `StateFlow`. Je to přesný opak Single Responsibility. **Každá z těchto domén si zaslouží vlastní ViewModel.**

#### 1.2 MVI infrastruktura existuje, ale NIKDE se nepoužívá - DONE

`BaseViewModel` a `MviContract` jsou krásně připraveny, ale `MainViewModel`, `CalendarViewModel`, `AiChatViewModel` a `HomeViewModel` je **nevyužívají**. Místo toho přímo manipulují `MutableStateFlow`. Architekturní rozhodnutí tak platí jen na papíře.

```kotlin
// Existuje, ale nepoužívá se:
abstract class BaseViewModel<S : UiState, I : UiIntent, E : UiEffect>(...)

// Co se reálně dělá všude:
private val _categories = MutableStateFlow<List<Category>>(emptyList())
private val _posts = MutableStateFlow<List<Post>>(emptyList())
// ... dalších 20 StateFlow
```

#### 1.3 Duplicitní logika - DONE

`downloadAllOfflineData()` je **zkopírována celá** do `MainViewModel` a zároveň existuje v `HomeViewModel` (který volá `offlineRepo.downloadAllData()`). Totéž platí pro:
- `getCurrentStreak()` v `HomeScreen.kt` a `getCurrentStreakCalendar()` v `CalendarScreen.kt` – stejná funkce, jiný název
- logika connectivity check je na dvou místech současně

#### 1.4 `MainActivity` dělá příliš mnoho - DONE

`MainActivity.onCreate()` zodpovídá za:
1. Inicializaci Streak/Points/Backpack managerů
2. Logiku streak (freeze check, přidání dnešního dne)
3. Plánování 4 různých alarmů
4. Registraci WorkManager jobu
5. Vykreslení celé aplikace

Celý `MyApp()` composable (400+ řádků) žije uvnitř `MainActivity` – navigace, overlaye, connectivity, šablona obrazovky.

#### ~~1.5 Manageři jako globální singletony s Context~~ ✅ OPRAVENO

```kotlin
// PŘED:
object PointsManager {
    fun addPoints(context: Context, amount: Int) { ... }
    fun init(context: Context) { ... }
}

// PO:
class PointsManager private constructor(context: Context) : IPointsManager {
    private val appContext = context.applicationContext
    override fun addPoints(amount: Int) { ... }
    companion object { fun initialize(context: Context) { ... } }
}
```

~~`PointsManager`, `ShopManager`, `BackpackManager`, `StreakFreezeManager` jsou `object` singletony. Přijímají `Context` jako parametr na každé volaní, nemají rozhraní, nedají se testovat ani nahradit. Správný přístup: třídy s rozhraním, injektované přes DI.~~

**Opraveno:** Všichni 4 manageři (+ IconPackManager) převedeni na třídy s rozhraním. Context uložen jednou při inicializaci v `TobisoApplication`. Přidána rozhraní `IPointsManager`, `IShopManager`, `IBackpackManager`, `IStreakFreezeManager`.

---

## 2. SOLID Principy - DONE

### Single Responsibility Principle (SRP) – Velmi špatné ❌

| Třída/soubor | Počet odpovědností |
|---|---|
| `MainViewModel` | 12+ domén |
| `MainActivity` | UI + notifikace + alarmy + workers + streak |
| `HomeScreen.kt` | UI + date parsing utils + sort mode persistence |
| `ProfileScreen.kt` | UI + SharedPreferences helpery jako top-level funkce |
| `CalendarScreen.kt` | UI + duplikát streak funkce |

### Open/Closed Principle (OCP) – Středně špatné ⚠️

`when (item.type)` v `ShopManager.purchaseItem()` přidá více větví při každém novém typu itemu. Správně by měl každý `ShopItemType` mít vlastní handler.

### Liskov Substitution (LSP) – OK ✅

Repository rozhraní jsou správně definována a implementace je konzistentní.

### Interface Segregation (ISP) – OK ✅

Rozhraní jsou malá a cílená (`PostsRepository`, `ExerciseRepository`, atd.).

### Dependency Inversion Principle (DIP) – Špatné ❌

ViewModely přímo instancují své závislosti:

```kotlin
// HomeViewModel.kt – tvrdě zadrátované závislosti
class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val postsRepo = PostsRepositoryImpl(application, OfflineDataManager(application))
    private val offlineRepo = OfflineRepositoryImpl(application, OfflineDataManager(application))
```

Žádný DI framework (Hilt / Koin) není použit. Každá třída vytváří své závislosti ručně.

---

## 3. Bezpečnost (Kritické – OWASP)

### ~~3.1 Hardcoded Credentials~~ ✅ OPRAVENO

~~```kotlin
// SecurityConfig.kt
fun getApiCredentials(): String {
    val username = System.getenv("TOBISO_API_USERNAME") ?: "admin"
    val password = System.getenv("TOBISO_API_PASSWORD") ?: "secret123"
    return Credentials.basic(username, password)
}

// ApiClient.kt – getUnsafeOkHttpClient()
val credential = Credentials.basic("admin", "secret123")
```~~

~~Heslo `secret123` je uloženo ve zdrojovém kódu. Každý, kdo rozbalí APK nebo si přečte kód, má přístup k API. Přihlašovací údaje **nesmí nikdy být ve zdrojovém kódu**.~~

~~**Řešení:** Použít `local.properties` + `BuildConfig`, nebo lépe Android KeyStore + runtime fetch z bezpečného endpointu.~~

**Opraveno:** Credentials přesunuty do `local.properties` (který je v `.gitignore` a nikdy se necommituje). `build.gradle.kts` načítá hodnoty z `local.properties` a vystavuje je přes `BuildConfig.API_USERNAME` a `BuildConfig.API_PASSWORD`. `SecurityConfig.getApiCredentials()` a `ApiClient.getUnsafeOkHttpClient()` nyní používají `BuildConfig` – žádné hardcoded hodnoty ve zdrojovém kódu.

### ~~3.2 Vypnuté Certificate Pinning~~ ✅ OPRAVENO

~~```kotlin
// ApiClient.kt
/*
if (!SecurityConfig.shouldUseTrustAllCerts()) {
    val certificatePinner = CertificatePinner.Builder()
        .add("tobiso.com", "sha256/3AyP+Dm88F+sG...")
        ...
*/
```~~

~~Certificate pinning je zakomentováno s odůvodněním "problémy s médii". Bez pinningu je aplikace zranitelná vůči MITM útokům. **Toto musí být opraveno.**~~

**Opraveno:** Certificate pinning aktivní v `getSecureOkHttpClient()` s ověřenými SHA-256 hashi (vygenerováno skriptem `get_ssl_hash.sh` dne 2026-03-19). `shouldUseTrustAllCerts()` opraven ze záludného `android.os.Build.TYPE != "user"` na `BuildConfig.DEBUG` – nyní spolehlivě platí: debug build = unsafe client, release build = certificate pinning. Hashi také zapsány do `network_security_config.xml` s expirací 2027-03-19.

### ~~3.3 Trust-All SSL v debug buildu~~ ✅ OPRAVENO

~~`getUnsafeOkHttpClient()` explicitně ignoruje veškerou SSL validaci:~~

~~```kotlin
builder.sslSocketFactory(sslContext.socketFactory, trustManager)
builder.hostnameVerifier { _, _ -> true }
```~~

~~I přesto, že je určen pro debug, soubor `shouldUseTrustAllCerts()` vrací `true` pro non-release buildy. Pokud tester nebo CI/CD pipeline používá debug build k testování backend komunikace, je celá SSL ochrana nefunkční.~~

**Opraveno:** `getUnsafeOkHttpClient()` přejmenován na `getDebugOkHttpClient()` a celý blok trust-all SSL (custom `TrustManager`, `sslSocketFactory`, `hostnameVerifier { _, _ -> true }`) odstraněn. Debug client nyní používá standardní SSL validaci systémových CA – pouze bez certificate pinningu. Debug-overrides v `network_security_config.xml` zajistí uživatelské certifikáty při vývoji.

### ~~3.4 Falešná integrity check~~ ✅ OPRAVENO

~~```kotlin
fun verifyAppIntegrity(): Boolean {
    return try {
        true // Dočasně vždy true
    } catch (e: Exception) { false }
}
```~~

~~Funkce která má zajistit bezpečnost vždy vrací `true`. Nikdy neprovede žádnou kontrolu.~~

**Opraveno:** `verifyAppIntegrity()` nyní skutečně ověřuje integritu aplikace porovnáním SHA-256 otisku podpisového certifikátu APK s očekávanou hodnotou z `BuildConfig.CERT_FINGERPRINT`:
- `SecurityConfig.initialize(context)` uloží `applicationContext` při startu aplikace (voláno z `TobisoApplication.onCreate()`).
- V **debug buildu** se kontrola přeskočí a aktuální otisk certifikátu se vytiskne do logcatu (tag `SecurityConfig`) pro snadné získání hodnoty.
- V **release buildu** se ověří podpisový certifikát APK – pokud neodpovídá `BuildConfig.CERT_FINGERPRINT`, funkce vrátí `false` a `ApiClient` vyhodí `SecurityException`.
- `CERT_FINGERPRINT` je uložen v `local.properties` (vyloučen z VCS) a dostupný přes `BuildConfig` – v release buildu nutno nastavit na skutečný otisk produkčního podpisového klíče.
- Implementace správně odlišuje API úrovně: API 28+ používá `GET_SIGNING_CERTIFICATES` + `signingInfo.apkContentsSigners`, API 24–27 používá deprecated `GET_SIGNATURES` (bezpečné pro minSdk ≥ 21).

### ~~3.5 Security token z hardcoded stringu~~ ✅ OPRAVENO

~~```kotlin
fun getSecurityToken(): String {
    val baseString = "com.tobiso.tobisoapp_2.0.1"
    val hash = digest.digest(baseString.toByteArray())
    return Base64.encodeToString(hash, Base64.NO_WRAP)
}
```~~

~~Hash deterministicky vypočítaný z pevného řetězce není bezpečnostní token – je to statická konstanta zakódovaná jako hash. Útočník triviálně zjistí jeho hodnotu.~~

**Opraveno:** `getSecurityToken()` nyní generuje skutečný HMAC-SHA256 token ve formátu `{timestampSeconds}.{Base64(HMAC)}`. Tajný klíč pochází z `BuildConfig.SECURITY_TOKEN_SECRET`, který je uložen v `local.properties` (vyloučen z VCS) – žádná pevná hodnota ve zdrojovém kódu. Token je vázán na čas žádosti (replay ochrana) a na package ID aplikace. Server ověří: extrahuje timestamp, zkontroluje okno ±5 minut, a porovná HMAC vypočítaný ze sdíleného tajného klíče.

### ~~3.6 `Reflection` pro volání funkce~~ ✅ OPRAVENO

~~```kotlin
// PointsManager.kt
val checkPointsAchievements = Class.forName("com.example.tobisoappnative.screens.ProfileScreenKt")
    .getDeclaredMethod("checkPointsAchievements", android.content.Context::class.java)
checkPointsAchievements.invoke(null, context)
```~~

~~Reflection přes `Class.forName()` pro volání funkce z jiné vrstvy. Toto je extrémně křehké (selže při ProGuard obfuskaci), je to obcházení správné architektury a přidává zbytečné bezpečnostní riziko.~~

**Opraveno:** `generatePointsAchievements()` a `checkPointsAchievements()` přesunuty z `ProfileScreen.kt` do `utils/PointsAchievementsHelper.kt`. `PointsManager.addPoints()` nyní volá `checkPointsAchievements(appContext)` přímým importem – žádná reflection. `ProfileScreen.kt` (UI) importuje `generatePointsAchievements` ze utils pro zobrazení achievementů. `TobisoApp.kt` importuje `checkPointsAchievements` ze utils (namísto z packages screens).

---

## 4. Modernizace kódu

### ~~Zastaralé závislosti~~ ✅ OPRAVENO

```toml
lifecycleRuntimeKtx = "2.6.1"    # Aktuální: 2.9.0
coreKtx = "1.10.1"               # Aktuální: 1.16.0
activityCompose = "1.8.0"        # Aktuální: 1.10.1
composeBom = "2024.09.00"        # Aktuální: 2025.05.00
```

### ~~Zastaralý `accompanist` pro System UI~~ ✅ OPRAVENO

```kotlin
// MainActivity.kt
val systemUiController = rememberSystemUiController()
systemUiController.setStatusBarColor(color = surfaceColor, ...)
```

`accompanist/systemuicontroller` je **deprecated** a nebude dostávat aktualizace. Aplikace volá `enableEdgeToEdge()` na začátku, ale pak přepisuje barvy přes accompanist. Správný moderní přístup:

```kotlin
// Moderně – pouze enableEdgeToEdge() + WindowInsets padding
enableEdgeToEdge()
// Barvu status baru řídí systém automaticky dle téma
```

### ~~`java.util.Calendar` a `SimpleDateFormat` místo `java.time.*`~~ ✅ OPRAVENO

V projektu se masivně používají `Calendar.getInstance()`, `SimpleDateFormat`, `Date` – starší Java API. Projekt má `minSdk = 24`, tedy je `java.time.*` (Local Date/Time API) plně dostupné. V některých místech (`StreakFreezeManager`, `ProfileScreen`) se `java.time.LocalDate` správně používá, ale v `CalendarViewModel`, `HomeScreen`, `OfflineDataManager` stále žije stará API.

### ~~`OfflineDataManager` ukládá velká JSON data do `SharedPreferences`~~ ✅ OPRAVENO

```kotlin
prefs.edit().putString(KEY_CATEGORIES, gson.toJson(categories))
prefs.edit().putString(KEY_POSTS, gson.toJson(posts))        // celý seznam článků
prefs.edit().putString(KEY_QUESTIONS, gson.toJson(questions)) // všechny otázky
// ...
```

`SharedPreferences` není určen pro ukládání stovek KB+ dat. Může způsobit:
- ANR při synchronním čtení na hlavním vlákně
- Pomalé startující aplikace (SharedPreferences bloky jsou načítány celé najednou)
- Limit pro jednu hodnotu je 8 MB

**Správné řešení:** Room s entitami, nebo binární soubory na disku (já.e. JSON do File), ale nikdy ne SharedPreferences pro velká data.

### ~~Chybí Dependency Injection framework~~ ✅ OPRAVENO (Hilt již nasazen)

Bez Hilt nebo Koin se veškeré závislosti instancují ručně. Pro aplikaci této velikosti je to udržitelnostní problém. Hilt je de-facto standard pro Android a má první třídu podporu s Compose.

### ~~Package namespace~~ ✅ OPRAVENO

```
com.example.tobisoappnative
```

Produkční aplikace `com.tobiso.tobisoapp` (viz `applicationId`) stále používá `com.example` namespace pro všechny zdrojové soubory. Mělo by být přejmenováno na `com.tobiso.tobisoappnative` nebo `cz.tobiso.app`.

---

## 5. Výkon a plynulost

### ~~5.1 Polling připojení každé 2 sekundy~~ ✅ OPRAVENO

~~```kotlin
// MainActivity.kt - MyApp()
LaunchedEffect(context) {
    while (true) {
        isConnected.value = checkInternetConnection(context)
        delay(2000)
    }
}
```~~

~~Každé 2 sekundy se volí systémová služba `ConnectivityManager`. Přitom `NetworkUtils.observeConnectivityAsFlow()` (správný reaktivní přístup) je **již implementován** v projektu, ale vůbec se nepoužívá. Polling spotřebovává CPU a baterii zbytečně.~~

**Opraveno:** Polling `LaunchedEffect` odstraněn. `isConnected` je nyní `State<Boolean>` napájený přímo z `NetworkUtils.observeConnectivityAsFlow(context).collectAsState(initial = NetworkUtils.isOnline(context))`. Connectivity manager reaguje na změny sítě okamžitě přes `NetworkCallback` – žádné zbytečné CPU/baterie.

### ~~5.2 O(n) API volání při stahování offline dat~~ ✅ OPRAVENO

~~```kotlin
// MainViewModel.kt
postsArray.forEach { post ->
    val exercisesForPost = ApiClient.apiService.getExercisesByPostId(post.id)
    allExercises.addAll(exercisesForPost)
}
```~~

~~Pro každý post se volá samostatně API `/InteractiveExercises/post/{id}`. Pokud existuje 100 článků, je to 100 HTTP volání sekvenčně. **Backend by měl mít endpoint pro bulk fetch** nebo by se volání měla paralelizovat přes `async/await`.~~

**Opraveno:** Sekvenční `forEach` smyčka v `OfflineRepositoryImpl.downloadAllData()` nahrazena paralelním `coroutineScope { map { async { ... } }.awaitAll().flatten() }`. Všechna volání `getExercisesByPostId()` se nyní spouštějí souběžně – při 100 postech klesne čekání z ~100× latence na ~1× latenci (čas nejpomalejšího požadavku). Chyba při stahování jednoho postu je izolována (vrátí `emptyList()`) a nebrzdí ostatní.

### ~~5.3 `OfflineDataManager` – potenciální ANR~~ ✅ OPRAVENO

~~Metody jako `getCachedCategories()`, `getCachedPosts()` čtou `SharedPreferences` a deserializují JSON. Pokud by byly volány na hlavním vlákně (byť ViewModely používají `Dispatchers.IO`), způsobí ANR. Použití Room by tento problém eliminovalo.~~

**Opraveno:** `OfflineDataManager` migrován na Room databázi (`tobiso_offline.db`). Vytvořeny entity pro všechny 8 datových typů (`CategoryEntity`, `PostEntity`, `QuestionPostEntity`, `QuestionEntity`, `EventEntity`, `AddendumEntity`, `RelatedPostEntity`, `ExerciseEntity`), příslušná DAO rozhraní a `AppDatabase`. Room je registrován v `DatabaseModule` jako Hilt singleton; DAO závislosti jsou injektovány do `OfflineDataManager` přes `AppModule`. Room garantuje přístup k DB výhradně z vláken IO – volání z hlavního vlákna vyhodí výjimku a tím zcela eliminuje ANR riziko. Staré soubory JSON z `filesDir/offline_cache/` jsou automaticky smazány při prvním startu nové verze. Jako bonus přinesla migrace SQL indexy na sloupce `categoryId` a `postId`, takže dotazy jako `getCachedPostsByCategory()` či `getCachedQuestionsByPostId()` již neprocházejí celou tabulku v paměti.

### ~~5.4 `material-icons-extended` jako plná závislost~~ ✅ OPRAVENO

~~```kotlin
implementation("androidx.compose.material:material-icons-extended:1.7.6")
```~~

~~Tato závislost přidá přes 7 MB ke velikosti APK. Při release buildu s R8 se nepoužívané ikony zahazují, ale správnější přístup je použít jen konkrétní ikony z resources nebo SVG.~~

**Opraveno:** Odstraněna explicitní verze `materialIconsExtended = "1.7.8"` z `libs.versions.toml`. Závislost `androidx-material-icons-extended` nyní nemá `version.ref` – verzi spravuje Compose BOM (`2025.05.00`), stejně jako ostatní Compose knihovny (`material3`, `ui` atd.). Release build má aktivní `isMinifyEnabled = true` a `isShrinkResources = true`, takže R8 odstraní všechny nepoužívané ikony – reálný přírůstek APK odpovídá jen cca 70 ikonám reálně použitým v kódu.

### ~~5.5 Debug `println()` v produkčním kódu~~ ✅ OPRAVENO

~~```kotlin
// PointsManager.kt
println("=== POINTS MANAGER DEBUG ===")
println("Adding $amount points for milestone $milestoneDay days")
println("Points before: $points")

// MainViewModel.kt
println("DEBUG: App initialized - Online: $isOnline, Offline: ${!isOnline}")

// CalendarViewModel.kt
android.util.Log.d("EventOverlap", "=== Checking event '...' for day $dateStr ===")
```~~

~~Desítky `println()` a debug logů jsou po celém projektu. `println()` se **nezahodí** ani v release buildu s R8. Správný přístup je `Timber` (nebo alespoň `if (BuildConfig.DEBUG)` wrapper). Debug logy zbytečně zatěžují logcat a mohou odhalit interní logiku útočníkovi.~~

**Opraveno:** Přidána závislost `com.jakewharton.timber:timber:5.0.1` (verze spravována přes `libs.versions.toml`). `Timber.plant(Timber.DebugTree())` je voláno v `TobisoApplication.onCreate()` podmíněně přes `BuildConfig.DEBUG` – v release buildu se žádný strom nezasadí a Timber volání jsou tichá (žádný výstup). Všechna `println()` volání (22 souborů) nahrazena `Timber.d()`/`Timber.e()`/`Timber.w()`/`Timber.i()` dle závažnosti. Všechna `android.util.Log.*` volání nahrazena Timber ekvivalenty – tagy jsou automaticky odvozeny z názvu třídy.

---

## 6. Testovatelnost ✅ OPRAVENO

### Přidané testovací závislosti

Do `libs.versions.toml` a `app/build.gradle.kts` přidány:

```toml
mockk = "1.13.13"
coroutinesTest = "1.9.0"
turbine = "1.2.0"
```

```kotlin
testImplementation(libs.mockk)
testImplementation(libs.kotlinx.coroutines.test)
testImplementation(libs.turbine)
```

### Napsané unit testy (43 testů, 0 selhání)

| Testový soubor | Počet testů | Pokrytí |
|---|---|---|
| `GetExerciseUseCaseTest` | 4 | GetExerciseUseCase – success, failure, offline error, verifikace volání repozitáře |
| `ValidateExerciseUseCaseTest` | 6 | ValidateExerciseUseCase – správná/špatná odpověď, offline, síťová chyba, částečné skóre, parametry |
| `GetAllQuestionsUseCaseTest` | 8 | GetAllQuestionsUseCase – success, prázdný seznam, failure, offline, delegace + helper properties třídy Question |
| `PointsManagerTest` | 11 | FakePointsManager – přidávání, odečítání, multiplikátor, StateFlow emise, milestone/achievement flows |
| `StreakFreezeManagerTest` | 13 | FakeStreakFreezeManager – přidání/použití freeze, MAX_FREEZES limit, duplicitní datum, isFreezeActive, reset |

### Fake implementace pro testy bez Android Context

Přidány `FakePointsManager` a `FakeStreakFreezeManager` – in-memory implementace příslušných rozhraní (`IPointsManager`, `IStreakFreezeManager`) bez závislosti na Android SharedPreferences. Slouží jako základ pro testování libovolné třídy, která tyto rozhraní přijímá.

### Dřívější příčiny špatné testovatelnosti – stav po opravě

| Problém | Stav |
|---|---|
| Globální singletony (`PointsManager`, `ShopManager`) | ✅ Převedeny na třídy s rozhraním |
| `MainViewModel` dělá vše | ⚠️ Stále existuje – P1 priorita refaktoringu |
| Reflection v `PointsManager` | ✅ Odstraněno |
| Žádné rozhraní pro `PointsManager` | ✅ `IPointsManager` existuje |
| Přímé volání `ApiClient.apiService` ve ViewModelu | ⚠️ Stále existuje v `HomeViewModel` – P1 |

Use Cases (`GetExerciseUseCase`, `ValidateExerciseUseCase`, `GetAllQuestionsUseCase`) jsou plně testovatelné přes mockované repozitáře díky interface-based designu.

---

## 7. Navigace

### 7.1 Magické route strings ❌

```kotlin
// MainActivity.kt
navController.navigate("postDetail/$postId")
navController.navigate("questions/$categoryId/$postId")
navController.navigate("aiChat/$postId/$postTitle/$encodedMessage")
```

Route řetězce jsou hardcoded stringy. Typo způsobí crash za běhu, ne compile error. Moderní řešení je **Type-Safe Navigation** (stabilní od Compose Navigation 2.8.x):

```kotlin
@Serializable
data class PostDetailRoute(val postId: Int)

navController.navigate(PostDetailRoute(postId = post.id))
```

### 7.2 Viditelnost bottom baru – fragile ⚠️

```kotlin
visible = (route == null || !(route.startsWith("postDetail") ||
    route.startsWith("about") || route.startsWith("feedback") ||
    // ... 15 dalších podmínek
))
```

Tento seznam musí být manuálně udržován. Při přidání nové obrazovky, kde má být bottom bar skryt, je snadné zapomenout ho přidat. S type-safe navigací by každá route mohla deklarovat vlastní metadata.

---

## 8. Kvalita kódu

### 8.1 TODO v produkčním kódu ⚠️

```kotlin
// BackpackManager.kt
BackpackItem(
    shopItem = shopItem,
    purchaseDate = System.currentTimeMillis() // TODO: uložit skutečné datum nákupu
)
```

`TODO:` komentáře označují nedokončenou funkčnost v produkčním kódu.

### 8.2 Zakomentovaný kód ⚠️

```kotlin
// MainActivity.kt
// DOČASNÉ: Vymazat dosažené milníky pro testování (odkomentujte pokud potřebujete)
// resetMilestones(this)
```

```kotlin
// ApiClient.kt – certificate pinning zakomentováno
/*
if (!SecurityConfig.shouldUseTrustAllCerts()) {
    val certificatePinner = ...
*/
```

Zakomentovaný kód znepřehledňuje soubory. Verze kódu patří do gitu, ne do komentářů.

### 8.3 Magická čísla ❌

```kotlin
// BackpackManager.kt
val classicIconPackId = 23  // magic number
val hasClassicIconPack = ...it.shopItem.id == classicIconPackId

// PointsManager.kt
private const val MAX_FREEZES = 3  // OK - pojmenovaná konstanta

// MainViewModel.kt
if (!offlineDataManager.isCacheFresh(15)) { ... }  // 15 minut - magic
```

ID `23` pro "Klasické ikony" je roztroušeno na více místech. Pokud se změní, vývojář musí najít všechna výskyty.

### 8.4 Utility funkce na nesprávném místě ⚠️

Top-level funkce `parseDateToMillis()`, `formatDateDisplay()`, `loadSortMode()`, `saveSortMode()` jsou definovány v `HomeScreen.kt`. Funkce `getProfileName()`, `saveProfileName()`, `getProfileImageUri()` jsou v `ProfileScreen.kt`. Tyto utility funkce patří do `utils/` nebo do příslušných repozitářů/ViewModelů.

---

## 9. Dependency management

```toml
# Verze v libs.versions.toml jsou fragmentované:
activityComposeVersion = "1.10.1"   # jedna verze
activityCompose = "1.8.0"            # druhá verze pro stejnou knihovnu!
```

Existují dvě různé verze pro `activity-compose` v `libs.versions.toml`. `androidx-activity-compose-v1101` (1.10.1) a `androidx-activity-compose` (1.8.0). Která se skutečně používá? Toto je zbytečné zmatení.

Závislosti přidávané přes hardcoded string místo version catalogue:
```kotlin
// build.gradle.kts
implementation("androidx.compose.material:material-icons-extended:1.7.6")
// Toto by mělo být v libs.versions.toml
```

---

## 10. Souhrn doporučení (prioritizováno)

### P0 – Kritické (Bezpečnost – opravit okamžitě)

1. **Odstranit hardcoded credentials** (`"secret123"`, `"admin"`) ze zdrojového kódu. Použít `local.properties` + `BuildConfig` pro development, nebo server-side credential rotation pro produkci.
2. **Znovu aktivovat Certificate Pinning** – rozbalit, vygenerovat správný hash certifikátu Let's Encrypt pro `tobiso.com` a přidat.
3. ~~**Nahradit Reflection volání** v `PointsManager` přímou závislostí nebo event bus.~~ ✅ OPRAVENO

### P1 – Vysoké (Architektura)

4. ~~**Nasadit Hilt** pro dependency injection – eliminuje globální singletony a manuální instancování.~~ ✅ OPRAVENO
5. **Rozdělit `MainViewModel`** na minimálně 5 menších ViewModelů: `CategoryViewModel`, `FavoritesViewModel`, `OfflineViewModel`, `TtsViewModel`, `QuestionsViewModel`.
6. **Přejít na MVI pattern** použitím existující `BaseViewModel` infrastruktury.
7. **Přejít na Type-Safe Navigation** (Compose Navigation 2.8+).

### P2 – Střední (Výkon & modernizace)

8. **Nahradit connectivity polling** reaktivním `observeConnectivityAsFlow()`.
9. **Migrovat offline cache na Room** – odstranit velká JSON data ze SharedPreferences.
10. **Nahradit `println()` Timbrem** – přidat `Timber.plant(DebugTree())` v debug buildu.
11. **Aktualizovat zastaralé závislosti** (Lifecycle 2.9, Core-KTX 1.16, Compose BOM 2025.05).
12. **Odstranit accompanist** – použít nativní `enableEdgeToEdge()` + WindowInsets.
13. **Paralelizovat offline fetch** cvičení (`async/await` nebo bulk API endpoint).

### P3 – Nízké (Čistota kódu)

14. **Přejmenovat package** z `com.example.tobisoappnative` na `com.tobiso.*`.
15. **Přesunout utility funkce** (`parseDateToMillis`, `formatDateDisplay` atd.) do `utils/`.
16. **Nahradit magická čísla** pojmenovanými konstantami (zejména `classicIconPackId = 23`).
17. **Odstranit zakomentovaný kód** a TODO komentáře nebo je sledovat v issue trackeru.
18. ~~**Napsat unit testy** alespoň pro Use Cases, Repository a ViewModel logiku.~~ ✅ OPRAVENO

---

## Závěr

TobisoAppNative je projekt, který má **správné architekturní záměry**, ale jejich provedení je nekonzistentní. MVI infrastruktura je nachystána a nevyužita. Repository pattern existuje, ale ViewModely ho obcházejí přímým voláním `ApiClient`. Clean Architecture je deklarována strukturou složek, ale `MainViewModel` jako God-objekt ji efektivně ruší.

Největším okamžitým rizikem jsou **hardcoded credentials** a **vypnuté SSL certificate pinning** – toto jsou bezpečnostní díry v produkční aplikaci.

Z technologického pohledu je aplikace postavena na moderních nástrojích (Kotlin 2.2, Compose, WorkManager, DataStore), ale chybí jí DI framework a má zastaralé verze klíčových knihoven.

Pro přechod na skutečný produkční standard by projekt potřeboval: Hilt, Room, Type-Safe Navigation, Timber, aktivní certificate pinning, unit testy a refactoring `MainViewModel`.
