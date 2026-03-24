
# Performance & Smoothness Audit — TobisoAppNative

Datum: 2026-03-23

Tento dokument shrnuje výsledky rychlého auditu zaměřeného na plynulost, efektivitu a stabilitu aplikace.

**Nízka priorita / celkové doporučení pro plynulost**

- Zapnout `StrictMode` v debug buildu pro odhalení hlavních chyb (main-thread I/O, disk reads).
- Integrovat LeakCanary pro odhalení paměťových úniků.
- Použít Android Studio profiler (CPU, Memory, System Trace) na kritických screens (timeline, chat, heavy lists).
- Optimalizace Gradle: povolit build cache, ověřit `kapt.incremental=true`, zkontrolovat nepotřebné závislosti a velikost APK.
- Centralizovat coroutines patterny (structured concurrency) a error handling v `ViewModel` (vyhnout se `GlobalScope`).
- Využití Coil pro obrázky (caching, downsampling, transformations) místo manuálních operací tam, kde je to možné.

**Konkrétní quick-fixes (doporučené pořadí)**

1. Přepsat `EventNotificationWorker` na `CoroutineWorker` (největší dopad na stabilitu a škálování).
2. Opravit `performCrop` v `components/ImageCropper.kt` — načítat bitmapu efektivně s `inSampleSize` nebo použít Coil/ImageDecoder a zajistit dekódování v `Dispatchers.IO`.
3. Přidat `key` do všech `LazyColumn`/`items` kde se položky dynamicky mění (AI chat, timeline, seznamy postů apod.).
4. Prověřit `ApiClient` pro možnost povolení HTTP/2 a vytvořit proces pro správu aktualizace pinů.
5. Zapnout `StrictMode` a spustit profilování na hlavních obrazovkách.

**Doplňující poznámky**

- V `ApiClient` je certificate pinning implementován — ujistit se, že existuje dokumentovaný proces pro aktualizaci hashů a fallback pro případy rotace certifikátů.
- V `ImageCropper` se po ukládání souboru volá `recycle()` na obou bitmapách — to je dobré, ale je nutné zajistit, že všechny cesty (chyba / výjimky) také uvolní zdroje.
- V `PostDetailScreen` se PDF ukládá bezpečně přes `MediaStore` pro novější Androidy; zkontrolovat případné zablokování UI pokud se velký soubor ukládá bez asynchronního I/O.

**Další kroky (moje návrhy)**

- Implementovat quick-fixes 1–3 a vytvořit PR s patchemi.
- Spustit `StrictMode` + profilování a poskytnout trace soubory pro detailní analýzu.
- Po opravách spustit end-to-end měření: cold start, scroll smoothness (60fps drop), GC/paměťové snapshoty.

---

Pokud chcete, mohu teď automaticky implementovat quick-fixes 1–3 a vytvořit patche.
