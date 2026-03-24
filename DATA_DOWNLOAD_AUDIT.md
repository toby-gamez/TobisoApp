# Audit: Stahování a práce s daty — TobisoAppNative

Datum: 2026-03-24
Role: senior developer — stručné, praktické zjištění a doporučení

## Shrnutí
Krátce: síťová vrstva je solidně navržena (Retrofit + OkHttp), má certificate pinning a bezpečnostní hlavičky. Hlavní rizika a možnosti zlepšení se týkají streamování a ukládání velkých souborů (PDF), atomických zápisů do databáze a bezpečného nakládání s citlivými credentials.

## Hlavní nálezy (severita / dopad)
- PDF stahování v UI načte celé tělo do paměti přes `ResponseBody.bytes()` — vysoké riziko OOM pro velké PDF (soubor: [app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt](app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt)).
- Retrofit/OkHttp klient je centralizovaný v `ApiClient` a obsahuje certificate pinning + bezpečnostní hlavičky — to je dobré (viz [app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt)), ale piny musí být spravované/rotované a je vhodné přidat záložní piny (backup CA).
- `OfflineRepositoryImpl` provádí masivní paralelní stahování s lokálním řízením concurrency (Semaphore 10) — dobrý přístup; ovšem DB zápisy v `OfflineDataManager.saveRemainingData` prováděné jako sekvence `deleteAll()` + `insertAll()` nejsou explicitně v transakci — riziko částečného zápisu při selhání (viz [app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt](app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt) a [app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt](app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt)).
- Bezpečnostní konfigurace (`SecurityConfig`) generuje HMAC token a drží credentials v `BuildConfig` — funguje, ale citlivé hodnoty v BuildConfig/local.properties nejsou ideální pro produkci (viz [app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt](app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt)).
- `NetworkUtils.isOnline()` používá moderní API a `observeConnectivityAsFlow()` — OK.
- PDF ukládání používá MediaStore pro Android Q+ a starý přístup pro starší verze; permission handling existuje, ale implementace načítá celý byte array před zápisem (viz PostDetailScreen) — zlepšit na streaming a progress.

## Doporučení (konkrétní kroky)
1. Nahradit `ResponseBody.bytes()` streamovacím zápisem.
   - Problém: `bytes()` alokuje celý soubor v paměti.
   - Doporučení: použít `responseBody.byteStream()` a kopírovat do `OutputStream` po blocích (buffer 8k-32k), zavřít `ResponseBody` v finally/`use` a aktualizovat UI podle přečtených bajtů (pokud je dostupný `contentLength`).
   - Příklad (přibližný):

```kotlin
val body = vm.downloadPostPdf(id)
body.byteStream().use { input ->
  val resolver = context.contentResolver
  resolver.openOutputStream(uri).use { output ->
    val buffer = ByteArray(8192)
    var read: Int
    var total = 0L
    val len = body.contentLength()
    while (input.read(buffer).also { read = it } != -1) {
      output?.write(buffer, 0, read)
      total += read
      // volání onProgress(total / len.toFloat()) pokud len > 0
    }
  }
}
```

2. Zavést transakce pro zápisy do Room v `OfflineDataManager.saveRemainingData()` a `saveCategoriesAndPosts()`.
   - Buď anotovat DAO metodu `@Transaction`, nebo použít `withTransaction { ... }` z Room DB instance tak, aby `deleteAll()` + `insertAll()` byly atomické.

3. Přidat postupné zpětné volání / retry s exponenciálním backoff pro transient HTTP chyby ve velkých dávkách (Phase 2). Logovat a reportovat selhání (telemetry).

4. Pro certificate pinning:
   - Mít připravený proces rotace pinů (skript už existuje: `get_ssl_hash.sh`).
   - Doporučit záložní pin (backup) a/nebo pinovat CA, ne jen leaf, aby aktualizace certifikátu serveru nezlomila aplikaci.

5. Credentials a tokeny:
   - Neukládat produkční hesla/secret přímo do VCS nebo BuildConfig. Preferovat secure storage nebo vydávání krátkodobých tokenů přes secure backend.
   - Pokud nelze, ověřit, že `local.properties`/CI secrets nejsou v repu a přidat kontrolu do CI.

6. PDF UX a bezpečnost:
   - Streamovat a ukazovat progress. Zajistit, že soubor je uložen s pravými MIME typy a že `FLAG_GRANT_READ_URI_PERMISSION` se správně používá.
   - Pro Android 11+ preferovat MediaStore / SAF a vyhnout se WRITE_EXTERNAL_STORAGE fallbackům (scoped storage). Zvažte sdílení do interního soukromého adresáře a sdílení přes `FileProvider` místo ukládání přímo do Downloads, pokud intentování k prohlížeči není kritické.

7. Concurrency tuning:
   - Semaphore(10) je rozumný. Pokud se projeví backlog nebo špičky požadavků, omezit ještě klientské počty: `OkHttp` `Dispatcher` `maxRequests`/`maxRequestsPerHost`.

8. Testy
   - Přidat unit/integration testy pro `OfflineRepositoryImpl` s mockovaným `ApiService` a pro `OfflineDataManager` ověřit atomicitu zápisů.

## Rychlé odkazy na relevantní soubory
- API definice: [app/src/main/java/com/tobiso/tobisoappnative/model/ApiService.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiService.kt)
- HTTP klient + pinning: [app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt)
- Offline stahování: [app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt](app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt)
- DB + cache manager: [app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt](app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt)
- PDF download + save (UI): [app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt](app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt)
- Security config: [app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt](app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt)

## Prioritizovaná opravná práce (rád/a bych to udělal/a jako PR)
1. Fix: streamovat PDF místo `bytes()` + přidat progress (v UI). (vysoká priorita)
2. Ensure Room writes are transactional v `OfflineDataManager` (střední–vysoká)
3. Review and document certificate pin rotation + add backup pins (střední)
4. Move secrets out of BuildConfig/local.properties for production or document secure process (vysoká)
5. Add retry/backoff + telemetry for bulk downloading (střední)

---
Pokud chceš, mohu okamžitě:
- vytvořit malý PR, který opraví PDF stahování v `PostDetailScreen.kt` na streamovací implementaci (doplním test a manuální krok pro zkoušku), nebo
- upravit `OfflineDataManager` tak, aby používal Room transaction (`withTransaction`) při ukládání kompletních dat.

Napiš, co má prioritu, a já to rovnou implementuji. 
