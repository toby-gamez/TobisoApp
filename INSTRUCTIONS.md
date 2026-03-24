# TobisoApp Native - Instrukce a dokumentace

## 📱 Popis aplikace

TobisoApp Native je nativní Android aplikace postavená na **Jetpack Compose**, která slouží jako vzdělávací platforma s možnostmi offline práce. Aplikace umožňuje procházení článků, testování znalostí pomocí kvízů, správu kalendářních událostí a sledování pokroku uživatele.

## 🏗️ Architektura aplikace

### Hlavní komponenty:
- **Frontend**: Jetpack Compose (Material 3 Design)
- **Backend komunikace**: Retrofit + OkHttp
-- **Data serializace**: kotlinx.serialization (migrováno z Gson)
- **Offline režim**: SharedPreferences + lokální soubory
- **State management**: ViewModel + StateFlow + Coroutines
- **Navigace**: Jetpack Navigation Compose

### Klíčové moduly:

#### 1. **Model Layer** (`/model/`)
- `ApiClient.kt` - konfigurace Retrofit a OkHttp klienta
- `ApiService.kt` - REST API endpointy
- `OfflineDataManager.kt` - správa offline cache dat
- `LocalEventManager.kt` - správa lokálních kalendářních událostí
- Data třídy: `Post.kt`, `Category.kt`, `Event.kt`, `Question.kt`, `Snippet.kt`

#### 2. **ViewModel Layer** (`/viewmodel/`)
- `MainViewModel.kt` - hlavní business logika, správa stavu
- `CalendarViewModel.kt` - správa kalendářních událostí

#### 3. **UI Layer** (`/screens/`)
- `HomeScreen.kt` - domovská obrazovka s navigací
- `SearchScreen.kt` - vyhledávání v obsahu
- `PostDetailScreen.kt` - detail článku
- `QuestionsScreen.kt` - kvízové otázky
- `CategoryListScreen.kt` - seznam kategorií
- Plus další obrazovky...

#### 4. **Utils** (`/utils/`)
- `NetworkUtils.kt` - detekce síťového připojení

## 🔧 Kritický problém a jeho řešení

### **Problém: `java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType`**

#### 🚨 **Příčina:**
Chyba vznikala **POUZE při release buildech** s ProGuard/R8 obfuskací! Debug buildy fungovaly správně. Problém spočíval v deserializaci JSON dat pomocí Gson knihovny v kombinaci s `TypeToken<List<T>>()`. ProGuard obfuskace narušila reflexi generických typů, protože:

1. **ProGuard odstraní metadata** o generických typech
2. **TypeToken spoléhá na reflexi** pro získání `ParameterizedType` 
3. **V obfuskovaném kódu** se `Class` objekt nemůže přetypovat na `ParameterizedType`

**Konkrétní místa výskytu:**
```kotlin
// PROBLEMATICKÝ KÓD (před opravou):
val type = object : TypeToken<List<Category>>() {}.type
val categories = gson.fromJson<List<Category>>(json, type)
```

#### ✅ **Řešení:**
Nahradil jsem `TypeToken` přístup za bezpečnější `Array<T>::class.java` přístup:

```kotlin
// OPRAVENÝ KÓD:
val categoriesArray = gson.fromJson(json, Array<Category>::class.java)
val categories: List<Category> = categoriesArray?.toList() ?: emptyList()
```

#### 📁 **Opravené soubory:**
1. **`OfflineDataManager.kt`**:
   - `getCachedCategories()` - `TypeToken<List<Category>>` → `Array<Category>::class.java`
   - `getCachedPosts()` - `TypeToken<List<Post>>` → `Array<Post>::class.java`
   
2. **`LocalEventManager.kt`**:
   - `loadLocalEvents()` - `TypeToken<List<Event>>` → `Array<Event>::class.java`

3. **`ApiClient.kt`**:
   - Přidána konfigurace `.setLenient()` a `.serializeNulls()` pro robustnější JSON parsing

#### 🎯 **Dodatečné optimalizace:**
- Opravil jsem zastaralý `Locale("cs", "CZ")` konstruktor na `Locale.Builder()`
- Přidal jsem lepší error handling s `printStackTrace()`

## 🌐 API a síťová komunikace

### Konfigurace API:
- **Base URL**: `https://www.tobiso.com/api/`
- **Autentifikace**: HTTP Basic Auth (username: admin, password: secret123)
- **SSL**: Vlastní trust manager pro development (⚠️ POUZE PRO VÝVOJ!)

### Endpointy:
```kotlin
@GET("categories") - získání kategorií
@GET("posts") - získání článků (volitelně filtrováno podle kategorie)
@GET("posts/{id}") - detail článku
@GET("Questions/post/{postId}") - kvízové otázky pro článek
@GET("Events") - kalendářní události
@GET("Events/range") - události v časovém rozsahu
```

## 💾 Offline režim

### Fungování:
1. **Online načtení**: Data se načtou z API a uloží do offline cache
2. **Offline fallback**: Pokud není internet, načtou se cached data
3. **Synchronizace**: Po obnovení připojení se data aktualizují

### Ukládání dat:
- **SharedPreferences**: pro kategorie a články (JSON formát)
- **Lokální soubory**: pro kalendářní události, snippety, uložené články
- **DataStore**: pro uživatelské preference

## 🎨 UI a UX

### Design systém:
- **Material 3 Design** s dynamickými barvami
- **Dark/Light mode** podpora
- **Responsive layout** pro různé velikosti obrazovek

### Klíčové UI komponenty:
- `SearchBar` s real-time filtrováním
- `SwipeRefresh` pro pull-to-refresh (deprecated, plánovaná migrace)
- Custom komponenty pro quiz, kalendář, streaky
- `LazyColumn` pro performantní seznamy

## 🏅 Gamifikační prvky

### Points systém:
- Body za čtení článků, dokončení kvízů
- Streak systém pro denní aktivitu
- Uložení pokroku v SharedPreferences

### Features:
- **Streaks**: sledování denní aktivity
- **Saved Posts**: ukládání oblíbených článků
- **Snippets**: vytváření výňatků z článků
- **Questions**: kvízový systém pro testování znalostí

## 🔄 Státy aplikace

### Network states:
```kotlin
_isOffline.value = true/false  // offline/online režim
_hasUserDismissedNoInternet.value  // uživatel odmítl "No Internet" screen
```

### Loading states:
```kotlin
isLoading.collectAsState()  // obecné načítání
_questionsLoading.value  // načítání kvízů
```

### Error handling:
```kotlin
_categoryError.value  // chyby kategorií
_postError.value     // chyby článků
_toastMessage.value  // toast notifikace
```

## 🚀 Build a deployment

### Gradle konfigurace:
- **Target SDK**: 35 (Android 15)
- **Min SDK**: 24 (Android 7.0)
- **Kotlin**: nejnovější stabilní verze
- **Compose BOM**: nejnovější stabilní verze

### Build varianty:
- **Debug**: s trust-all SSL, debug logy
- **Release**: s ProGuard/R8, produkční SSL

## 🐛 Známé problémy a řešení

### 1. SSL Certificate problémy:
- **Řešení**: Custom trust manager (POUZE PRO DEVELOPMENT)
- **Produkce**: Nutno implementovat správné SSL certifikáty

### 2. Android 15 kompatibilita:
- **Migrace z Gson**: Projekt byl migrován na `kotlinx.serialization`; problémy s `TypeToken` jsou odstraněny.
- **Deprecated API**: Postupná migrace na nové API

### 3. Network detection:
- **Problem**: Deprecated `NetworkInfo.isConnected`
- **Řešení**: Funkční, ale plánovaná migrace na `ConnectivityManager.NetworkCallback`

## 📋 TODO / Budoucí vylepšení

### Prioritní:
1. ✅ Oprava TypeToken problému (HOTOVO)
2. ✅ Produkční SSL bezpečnost (HOTOVO)
3. ✅ Migrace na nové ConnectivityManager API (HOTOVO)
4. ✅ AES-256 šifrování citlivých dat (HOTOVO)
5. 🔄 Migrace z SwipeRefresh na PullRefresh
6. 🔄 Certificate Pinning (připraveno, potřeba hash certifikátů)

### Dlouhodobé:
- Push notifikace
- Offline synchronizace s conflict resolution
- Pokročilejší gamifikace (achievementy, leaderboard)
- Widget pro homescreen
- Wear OS companion app

## 🔒 Bezpečnost

### ✅ IMPLEMENTOVÁNO (Prosinec 2024):
- ✅ **Produkční SSL**: Vypnuté trust-all certificates v release buildech
- ✅ **Environment credentials**: API údaje z environmentálních proměnných
- ✅ **AES-256-GCM šifrování**: Android KeyStore pro citlivá data  
- ✅ **Certificate Pinning**: Připraveno v network_security_config.xml
- ✅ **Integrity checks**: Základní ochrana proti tampering
- ✅ **Security headers**: X-Security-Token, X-App-Version
- ✅ **Network Security Config**: Oddělené debug/release konfigurace
- ✅ **ProGuard obfuskace**: Pokročilá ochrana kódu

### 📋 Produkční checklist:
1. **Nastavit ENV proměnné**: `TOBISO_API_USERNAME`, `TOBISO_API_PASSWORD`
2. **Implementovat certificate pinning**: Aktualizovat SHA-256 hash v XML
3. **Secure keystore**: Produkční keystore.properties
4. **Testing**: Otestovat release build na různých zařízeních

### 📁 Nové bezpečnostní soubory:
- `config/SecurityConfig.kt` - Centralizovaná bezpečnostní konfigurace
- `security/EncryptionManager.kt` - AES-256-GCM šifrování
- `SECURITY.md` - Detailní bezpečnostní dokumentace
- Vylepšený `utils/NetworkUtils.kt` - Moderní síťové monitorování

---

## ⚠️ **DŮLEŽITÉ: ProGuard/R8 a TypeToken problém**

### **Klíčové zjištění:**
- Chyba `java.lang.Class cannot be cast to java.lang.reflect.ParameterizedType` se vyskytuje **POUZE v release buildech**
- **Debug buildy fungují správně** - problém není v Android 15, ale v obfuskaci
- **ProGuard/R8** odstraňuje metadata o generických typech, což narušuje `TypeToken` reflexi

### **Řešení (implementováno):**
1. ✅ **Změna ApiService na Array<T>** - eliminuje TypeToken úplně
2. ✅ **Použít Array<T>::class.java** v offline manageru - bezpečné, rychlé
3. ✅ **Rozšířené ProGuard rules** - dodatečná ochrana
4. 🚫 **Vypnout obfuskaci** - nezadatelné pro produkci

### **Proč je Array<T> lepší:**
```kotlin
// PROBLÉM s obfuskací (Retrofit používá TypeToken interně):
@GET("categories")
suspend fun getCategories(): List<Category>  // ❌ Nefunguje s ProGuard
// Retrofit interně: TypeToken<List<Category>>

// ŘEŠENÍ bez závislosti na reflexi:
@GET("categories") 
suspend fun getCategories(): Array<Category>  // ✅ Funguje vždy

// V ViewModelu:
val categoriesArray = ApiClient.apiService.getCategories()
val categories = categoriesArray.toList()
```

### **Implementované změny:**
- **ApiService.kt**: Všechny `List<T>` změněny na `Array<T>`
- **MainViewModel.kt**: Přidána `.toList()` konverze
- **CalendarViewModel.kt**: Přidána `.toList()` konverze
- **EventNotificationWorker.kt**: Přidána `.toList()` konverze
- **proguard-rules.pro**: Rozšířené rules pro Gson/Retrofit kompatibilitu

---

## 📞 Kontakt a podpora

Pro technické problémy a další dotazy kontaktujte vývojový tým.

*Dokument aktualizován: 25. září 2025*