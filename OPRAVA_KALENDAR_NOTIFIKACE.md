# 🔧 OPRAVA PROBLÉMU S KALENDÁŘOVÝMI OZNÁMENÍMI

## 🚨 Problém
Když je aplikace **zavřená** nebo **běží na pozadí**, oznámení vždycky říkají "jdeš do školy", i když je víkend nebo prázdniny.

## 🔍 Příčina
`EventNotificationWorker` (background proces) **nemá spolehlivý přístup k internetu/API** → v `catch` bloku se vrátí `emptyList()` → aplikace myslí, že nejsou události → pošle "jdeš do školy".

### Problematický kód (PŘED opravou):
```kotlin
return try {
    val allEventsArray = ApiClient.apiService.getEventsInRange(startDate, endDate)
    // ...
} catch (e: Exception) {
    android.util.Log.e("EventNotificationWorker", "Error fetching events", e)
    emptyList() // ❌ PROBLÉM: Vždycky prázdný seznam = škola
}
```

## ✅ Řešení
Přidána **fallback logika** do `EventNotificationWorker.kt`:

### 1. Fallback hierarchie:
1. **API data** (pokud je dostupné)
2. **Lokální události** (uložené v zařízení)
3. **Základní víkendová logika** (sobota/neděle = volno)

### 2. Implementace:
```kotlin
// V getEventsForDate():
} catch (e: Exception) {
    android.util.Log.e("EventNotificationWorker", "Error fetching events", e)
    android.util.Log.w("EventNotificationWorker", "API failed, trying fallback logic...")
    
    // FALLBACK: Zkus použít lokální data nebo základní víkendovou logiku
    return getFallbackEventsForDate(date)
}

// Nová metoda getFallbackEventsForDate():
private suspend fun getFallbackEventsForDate(date: Date): List<Event> {
    // 1. Zkus lokální události
    val localEvents = LocalEventManager.expandRecurringEvents(context, date, date+1day)
    if (localEvents.isNotEmpty()) return localEvents
    
    // 2. Základní víkendová logika
    val dayOfWeek = Calendar.getInstance().apply { time = date }.get(Calendar.DAY_OF_WEEK)
    if (dayOfWeek == Calendar.SATURDAY || dayOfWeek == Calendar.SUNDAY) {
        // Vytvoř umělou událost "víkend"
        return listOf(artificialWeekendEvent)
    }
    
    return emptyList() // Všední den bez událostí = škola
}
```

## 🧪 Testování

### Přidány debug logy:
- `EventNotificationWorker` teď loguje celý proces
- Vidíš, kdy se používá API vs. fallback
- Můžeš sledovat v Android logcat

### Test v aplikaci:
1. Otevři kalendář
2. Podívej se na logy v Android Studio (logcat)
3. Testuj s vypnutým/zapnutým internetem

## 📱 Očekávané chování

### Když má aplikace internet:
- **API má události** → "Máš volno!" 
- **API nemá události** → "Jdeš do školy"

### Když NEMÁ internet (background):
- **Jsou lokální události** → "Máš volno!"
- **Je víkend** → "Máš volno!" (umělá víkendová událost)
- **Všední den, žádné události** → "Jdeš do školy"

## 🔧 Upravené soubory:
- `EventNotificationWorker.kt` - přidána fallback logika
- `NotificationTestHelper.kt` - přidány debug logy

## 🎯 Výsledek:
**Víkendové oznámení bude fungovat i když aplikace běží na pozadí!**

---
*Oprava implementována: 5. října 2025*