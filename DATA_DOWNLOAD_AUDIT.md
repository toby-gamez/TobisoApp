# Audit: Stahování a práce s daty — TobisoAppNative

Datum: 2026-03-24
Role: senior developer — stručné, praktické zjištění a doporučení

## Shrnutí
Krátce: síťová vrstva je solidně navržena (Retrofit + OkHttp), má certificate pinning a bezpečnostní hlavičky. Hlavní rizika a možnosti zlepšení se týkají streamování a ukládání velkých souborů (PDF), atomických zápisů do databáze a bezpečného nakládání s citlivými credentials.

## Hlavní nálezy (severita / dopad)
-- Retrofit/OkHttp klient je centralizovaný v `ApiClient` a obsahuje certificate pinning + bezpečnostní hlavičky — to je dobré (viz [app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt)).
- `OfflineRepositoryImpl` provádí masivní paralelní stahování s lokálním řízením concurrency (Semaphore 10) — dobrý přístup.
<!-- Credentials handling item resolved: moved to secure storage fallback in SecurityConfig -->
- `NetworkUtils.isOnline()` používá moderní API a `observeConnectivityAsFlow()` — OK.
PDF ukládání používá MediaStore pro Android Q+ a starý přístup pro starší verze; permission handling existuje.

## Doporučení (konkrétní kroky)
1. (PONECHÁNO) Přehled ostatních doporučení a testování na atomicitu zápisů.

2. PDF UX a bezpečnost: (řešeno)
   - Implementováno: pro Android 10+ se používá MediaStore; pro starší verze nyní ukládáme do app-specific external files, čímž se vyhneme WRITE_EXTERNAL_STORAGE fallbackům. Doporučení ohledně `FileProvider` je nadále platné pro sdílení mezi aplikacemi.

3. Concurrency tuning: (řešeno)
   - Limit pro paralelní stahování cvičení byl extrahován do konfigurované konstanty `MAX_CONCURRENT_EXERCISE_DOWNLOADS` v `OfflineRepositoryImpl`.
4. Testy
   - Přidat unit/integration testy pro `OfflineRepositoryImpl` s mockovaným `ApiService` a pro `OfflineDataManager` ověřit atomicitu zápisů.

## Rychlé odkazy na relevantní soubory
- API definice: [app/src/main/java/com/tobiso/tobisoappnative/model/ApiService.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiService.kt)
- HTTP klient + pinning: [app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt)
- Offline stahování: [app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt](app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt)
- DB + cache manager: [app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt](app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt)
- PDF download + save (UI): [app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt](app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt)
- Security config: [app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt](app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt)

## Prioritizovaná opravná práce (rád/a bych to udělal/a jako PR)
1. (Odstarněno) Move secrets out of BuildConfig/local.properties — implementováno ve `app/build.gradle.kts` (release nyní neobsahuje tajné hodnoty v BuildConfig).

---


Napiš, co má prioritu, a já to rovnou implementuji. 
