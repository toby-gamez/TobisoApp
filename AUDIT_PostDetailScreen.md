# Audit výkonu — PostDetailScreen

Datum: 2026-03-27
Autor: GitHub Copilot (audit)

## Shrnutí
Aplikace při načítání obrazovky `PostDetailScreen` pociťuje výrazné „sekání“ (jank). Hlavní podezření: těžké synchronní výpočty a časté rekombinace běžící na hlavním vlákně Compose, plus veškeré re-rendery celé obrazovky při aktualizaci stavů, které by měly ovlivnit jen malé části UI.

Soubor kontrolovaný: [app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt](app/src/main/java/com/tobiso/tobisoappnative/screens/PostDetailScreen.kt)

## Symptomy (co uživatel vidí)
- Zadrhávání při otevření obrazovky.
- Náhlé prodlevy při scrollování obsahu.
- Zjevné „zamrznutí“ UI při parsování nebo při startu stahování PDF.

## Důkazy z kódu (rychlý přehled problémových míst)
- Parsování obsahu voláním `parseContentToElements(content, ...)` přímo v Composable pomocí `remember(content, ...) { ... }` — to vykonává práci synchronně na UI vlákně.
- Výpočet počtu slov / minut čtení prováděný inline v Composable (s trim/ split/regex) — může být náročné pro velký text.
- Mnoho `collectAsState()` (downloadProgress, downloadUri, posts, relatedPosts, exercises, atd.) na vrcholu Composable. Pokud některý z těchto Flow často emituje, způsobí to celkovou rekombinaci obrazovky.
- `Column(...).verticalScroll(rememberScrollState())` používá celé content v paměti a vykreslí vše naráz; pro dlouhé články by se lépe hodil lazy přístup.
- Formátování datumu (`SimpleDateFormat.parse/format`) se děje v Composable při každém recomposition — měl by být předpočítán v ViewModelu.
- `LinearProgressIndicator` pro `downloadProgress` je navázaný přímo na `downloadProgress` hodnotu; frekventované update budou recomposeovat parent composable, pokud není enkapsulováno.

## Pravděpodobné root-cause body
1. Synchronous heavy work on main thread
   - Parsování markdown/HTML do `ContentElement` probíhá synchronně v `remember` uvnitř Composable. I když je `remember`, výpočet se spustí na hlavním vlákně a blokuje při prvním zobrazení.
2. Příliš široké recomposition
   - Velké množství `collectAsState()` udržovaných ve vrcholovém Composable znamená, že update kteréhokoliv Flow může zrekomponovat celou obrazovku.
3. Ne-líné vykreslování dlouhého obsahu
   - Použití `Column` + `verticalScroll` místo `LazyColumn`/lazy řešení způsobí, že Compose musí vykreslit kompletní strom najednou.
4. Opakované CPU-bound operace v Composable
   - Word count, date parsing a další stringové operace prováděné přímo v UI vrstvě.
5. Neoptimalizované obrázky a layouty
   - Pokud `ContentRenderer` nebo `AsyncImage` nemají pevné rozměry/placeholdery, mohou způsobit relayout a měnit výkon při scrollu.

## Doporučení (rychlé opravy — priorita A)
- Přesuňte veškeré těžké parsování (např. `parseContentToElements`) do ViewModelu a vykonávejte jej na background dispatcher (`Dispatchers.Default`). Výsledek publikujte jako StateFlow/LiveData a v Compose pouze odebírejte již zpracovaná data.
- Předpočítejte „word count“ a formátované datum v ViewModelu, místo parsování/počtů v Composable.
- Omezte rozsah recomposition: přesunout často aktualizované stavy (např. `downloadProgress`) do menších, izolovaných composables. Nebo použijte `derivedStateOf`/`remember` tak, aby velké části UI nebyly závislé na těchto stavech.
- Použijte `LazyColumn` / lazy composables pro dlouhý obsah či seznamy souvisejících článků, aby se nevykreslovalo vše najednou.
- Pokud parsing může chvíli trvat, zobrazte placeholder skeleton a poté přepněte na hotový obsah — zlepší perceived performance.

## Doporučení (konkrétní implementační kroky)
1. Ve `PostDetailViewModel` vytvořit suspend funkci `parseContentAsync(content)` která běží na `Dispatchers.Default` a vystavuje `parsedContent: StateFlow<List<ContentElement>>`.
2. V Composable použít `val contentElements by vm.parsedContent.collectAsState()` místo volání `parseContentToElements(...)` v UI.
3. Přesunout formátování datumu a výpočet `wordCount/minutes` do ViewModelu a vystavit hotové stringy.
4. Izolovat `downloadProgress` UI do samostatného malého composable, například `DownloadProgressBar(downloadProgressFlow)`, který jediný odebírá progress flow.
5. Nahradit `Column(...).verticalScroll(...)` část obsahující hlavní článek `LazyColumn` s položkami, kde velké bloky (např. obrázky, long text blocks) se renderují jako jednotlivé položky.
6. Použít `AsyncImage` s pevnou velikostí nebo `placeholder` a `contentScale` parametry pro minimalizaci relayoutů.

## Nástroje pro profilaci (jak ověřit opravy)
- Android Studio Profiler (CPU + System trace) — zkontrolovat hlavní vlákno při otevření obrazovky.
- `adb shell perfetto` / System Tracing pro detailní trace Compose a GC.
- Compose tooling: recomposition counts a Layout Inspector (Compose): zapnout `Show recomposition counts` a sledovat které composables se recomposeují často.
- Logování časů: krátkodobě přidat měření času (Chrono) kolem parsing kódu ve ViewModelu, abyste ověřili přesun na pozadí.

## Rychlé patch návrhy (pseudokód)
- Ve ViewModelu:
  - launch(Dispatchers.Default) { val parsed = parseContentToElements(content); _parsedContent.emit(parsed) }
- V Composable:
  - val parsed by vm.parsedContent.collectAsState(initial = emptyList())
  - ContentRenderer(parsed)

## Další poznámky a next steps
- Audit doporučil první změnu: přesun parsing do ViewModelu. To je největší win pro plynulost.
- Po nasazení změn znovu profilujte a měřte, zaměřte se pak na obrazovky s obrázky a network-bounded operace.

---
Pokud chcete, mohu rovnou: 1) navrhnout konkrétní patch do `PostDetailViewModel` a `PostDetailScreen.kt`, nebo 2) připravit změny krok za krokem a otestovat (profilovat). Dejte vědět, kterou možnost preferujete.
