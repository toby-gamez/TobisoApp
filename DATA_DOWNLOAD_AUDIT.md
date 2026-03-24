# Audit: Stahování a práce s daty — TobisoAppNative

Datum: 2026-03-24
Role: senior developer — stručné, praktické zjištění a doporučení

## Shrnutí
Krátce: síťová vrstva je solidně navržena (Retrofit + OkHttp), má certificate pinning a bezpečnostní hlavičky. Hlavní rizika a možnosti zlepšení se týkají streamování a ukládání velkých souborů (PDF), atomických zápisů do databáze a bezpečného nakládání s citlivými credentials.

## Hlavní nálezy (severita / dopad)
- Retrofit/OkHttp klient je centralizovaný v `ApiClient` a obsahuje certificate pinning + bezpečnostní hlavičky — to je dobré (viz [app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt)), ale piny musí být spravované/rotované a je vhodné přidat záložní piny (backup CA).
- `OfflineRepositoryImpl` provádí masivní paralelní stahování s lokálním řízením concurrency (Semaphore 10) — dobrý přístup.
- Bezpečnostní konfigurace (`SecurityConfig`) generuje HMAC token a drží credentials v `BuildConfig` — funguje, ale citlivé hodnoty v BuildConfig/local.properties nejsou ideální pro produkci (viz [app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt](app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt)).
- `NetworkUtils.isOnline()` používá moderní API a `observeConnectivityAsFlow()` — OK.
PDF ukládání používá MediaStore pro Android Q+ a starý přístup pro starší verze; permission handling existuje — doporučuji přidat progress indikaci při stahování a ověřit streaming (pokud není již implementován).

## Doporučení (konkrétní kroky)
1. (Opraveno) Streamování PDF bylo řešeno ve `PostDetailScreen.kt`; ověřit případné UI progress požadavky.

2. (PONECHÁNO) Přehled ostatních doporučení a testování na atomicitu zápisů.

3. Pro certificate pinning:
   - Mít připravený proces rotace pinů (skript už existuje: `get_ssl_hash.sh`).
   - Doporučit záložní pin (backup) a/nebo pinovat CA, ne jen leaf, aby aktualizace certifikátu serveru nezlomila aplikaci.

4. Credentials a tokeny:
   - Neukládat produkční hesla/secret přímo do VCS nebo BuildConfig. Preferovat secure storage nebo vydávání krátkodobých tokenů přes secure backend.
   - Pokud nelze, ověřit, že `local.properties`/CI secrets nejsou v repu a přidat kontrolu do CI.

5. PDF UX a bezpečnost:
   - Streamovat a ukazovat progress. Zajistit, že soubor je uložen s pravými MIME typy a že `FLAG_GRANT_READ_URI_PERMISSION` se správně používá.
   - Pro Android 11+ preferovat MediaStore / SAF a vyhnout se WRITE_EXTERNAL_STORAGE fallbackům (scoped storage). Zvažte sdílení do interního soukromého adresáře a sdílení přes `FileProvider` místo ukládání přímo do Downloads, pokud intentování k prohlížeči není kritické.

6. Concurrency tuning:
   - Semaphore(10) je rozumný. Pokud se projeví backlog nebo špičky požadavků, omezit ještě klientské počty: `OkHttp` `Dispatcher` `maxRequests`/`maxRequestsPerHost`.

7. Testy
   - Přidat unit/integration testy pro `OfflineRepositoryImpl` s mockovaným `ApiService` a pro `OfflineDataManager` ověřit atomicitu zápisů.

## Rychlé odkazy na relevantní soubory
- API definice: [app/src/main/java/com/tobiso/tobisoappnative/model/ApiService.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiService.kt)
- HTTP klient + pinning: [app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt](app/src/main/java/com/tobiso/tobisoappnative/model/ApiClient.kt)
- Offline stahování: [app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt](app/src/main/java/com/tobiso/tobisoappnative/repository/OfflineRepositoryImpl.kt)
- DB + cache manager: [app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt](app/src/main/java/com/tobiso/tobisoappnative/model/OfflineDataManager.kt)
- PDF download + save (UI): [app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt](app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt)
- Security config: [app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt](app/src/main/java/com/tobiso/tobisoappnative/config/SecurityConfig.kt)

## Prioritizovaná opravná práce (rád/a bych to udělal/a jako PR)
1. Review and document certificate pin rotation + add backup pins (střední)
2. Move secrets out of BuildConfig/local.properties for production or document secure process (vysoká)

---


Napiš, co má prioritu, a já to rovnou implementuji. 
