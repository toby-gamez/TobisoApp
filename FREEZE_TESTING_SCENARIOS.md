## 🧪 TEST SCÉNÁŘ - Jak fungují Freeze dny

### **Jak se data ukládají:**
1. **Normální streak dny** → `SharedPreferences "StreakData"` → klíč `"streak_days"`
2. **Freeze dny** → `SharedPreferences "streak_freeze_prefs"` → klíč `"used_freezes"`

### **Scénář 1: Zapomenutí bez Freeze**
```
Den 1: ✅ Aktivita (uloženo do streak_days)
Den 2: ❌ Zapomněl, NEMÁ freeze
Den 3: ✅ Aktivita (uloženo do streak_days)
```

**Výsledek:**
- `streak_days` = ["2025-10-09", "2025-10-11"] 
- `used_freezes` = []
- **Streak = 1** (pouze dnešní den)
- **Původní řada se NEZACHOVÁ** ❌

### **Scénář 2: Zapomenutí s Freeze**
```
Den 1: ✅ Aktivita (uloženo do streak_days)
Den 2: ❌ Zapomněl, ALE má freeze → AUTO-POUŽIJE
Den 3: ✅ Aktivita (uloženo do streak_days)
```

**Výsledek:**
- `streak_days` = ["2025-10-09", "2025-10-11"]
- `used_freezes` = ["2025-10-10"] 
- **Streak = 3** (den 1 + freeze + dnešní den)
- **Původní řada se ZACHOVÁ** ✅

### **Scénář 3: Druhé zapomenutí bez dalšího Freeze**
```
Den 1: ✅ Aktivita
Den 2: ❄️ Freeze použit (zachráněno)
Den 3: ✅ Aktivita  
Den 4: ❌ Zapomněl, NEMÁ další freeze
Den 5: ✅ Aktivita
```

**Výsledek:**
- `streak_days` = ["2025-10-09", "2025-10-11", "2025-10-13"]
- `used_freezes` = ["2025-10-10"]
- **Streak = 1** (pouze dnešní den)
- **Freeze dny zůstávají uložené, ale aktuální streak se přeruší** ❌

### **DŮLEŽITÉ:**
- Freeze dny se **NEMAZOU** ani **NEPŘESOUVAJÍ**
- Zůstávají trvale uložené v `used_freezes`
- Počítají se do **maximální řady** navždy
- Ale **aktuální řada se přeruší**, pokud znova zapomenete a nemáte freeze