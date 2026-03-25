**Audit: Plynulost, práce s daty, ViewModely a obrazovky (senior developer)**

**Shrnutí**
- **Scope:** Rychlý audit zdrojového kódu Compose UI, ViewModelů, repozitářů a kerja s coroutines/Flow.
- **Závěr:** Kód ukazuje solidní architekturu (Hilt, Repository, StateFlow/DataStore), ale několik UI-layer anti-patternů a drobných výkonových rizik.

**Hodnocení (0-10)**
- **Performance:** 7/10 — většinou korektní, ale jsou místa s pollingem a těžkou IO v UI.
- **Architektura (ViewModel/Repo):** 8/10 — separace repo/VM dobrá, používá Flow/DataStore a DI.
- **Práce s daty:** 7/10 — DataStore/Room/Flow použity správně; doporučeno připravit migrace pro budoucí změny schématu.
- **UI / Compose:** 8/10 — Compose idiomy používány, ale některé Composable provádějí těžké IO nebo polling.

**Hlavní problémy a doporučení**
<!-- Removed: IO in Composables fixed by moving download logic into PostDetailViewModel -->
**DB migrace:** Připraveny základní migrace v `DatabaseModule` (no-op migration přidána), takže není použito `fallbackToDestructiveMigration()`; doporučeno doplnit reálné migrace při změnách schématu.
 

- **Favorites (DataStore + Flow):** [app/src/main/java/com/tobiso/tobisoappnative/repository/FavoritesRepositoryImpl.kt](app/src/main/java/com/tobiso/tobisoappnative/repository/FavoritesRepositoryImpl.kt#L1)



**Rychlé refaktory (první sprint)**
- **Refactor A:** Přesunout všechny síťové / diskové operace z Composables do `ViewModel`/`Repository` (low risk, vysoký přínos).
- **Refactor B:** Nahradit polling event-driven `Flow` (NetworkUtils) a upravit `LaunchedEffect` na `collect` z ViewModelu.
-- **Refactor C:** (hotovo) Základní no-op migrace přidána v `DatabaseModule`, doporučeno doplnit konkrétní migrace podle potřeby.

**Doporučené metriky a testy**
- **Měření:** přidat prosté časování dlouhých operací (log/analytics) a sledovat frame drops v release builds (Systrace/Android Studio profiler).
- **Testy:** integrační testy pro OfflineRepository (konkurentní stahování) a unit testy pro ViewModely (StateFlow efekty).

**Další kroky**
- **Chceš, abych:**
  - - **Napsal konkrétní PR** s přesunem download logic z `PostDetailScreen` do `PostDetailViewModel`? (vysoká priorita)
  - - **Vytvořil checklist migrací** pro Room DB? (střední priorita)

---
*Audit vytvořil senior review focused na rychlém nasazení oprav a bezpečných refaktorů. Pokud chceš, připravím PRy pro vybrané refaktory.*
