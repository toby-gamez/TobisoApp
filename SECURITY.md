# 🔒 BEZPEČNOSTNÍ DOKUMENTACE - TobisoApp Native

## 📋 PŘEHLED IMPLEMENTOVANÝCH BEZPEČNOSTNÍCH OPATŘENÍ

### ✅ **IMPLEMENTOVÁNO (Prosinec 2024)**

#### 1. **SSL/TLS Bezpečnost**
- **Produkční SSL**: Vypnuté trust-all certificates v produkci
- **Certificate Pinning**: Připraveno pro implementaci (network_security_config.xml)
- **Pouze HTTPS**: Zakázán cleartext traffic v produkci
- **Network Security Config**: Oddělená konfigurace pro debug/release

#### 2. **API Autentifikace**
- **Environmentální proměnné**: Credentials načítané z ENV variables
- **Bezpečnostní hlavičky**: X-Security-Token, X-App-Version
- **Fallback ochrana**: Bezpečný fallback při chybě SSL
- **Request interceptor**: Centralizovaná správa autentifikace

#### 3. **Šifrování dat**
- **Android KeyStore**: AES-256-GCM šifrování citlivých dat
- **EncryptionManager**: Centralizovaná správa šifrovacích klíčů
- **Secure SharedPreferences**: Šifrované ukládání v offline režimu
- **Key rotation**: Možnost obnovení klíčů při kompromitaci

#### 4. **Ochrana aplikace**
- **Integrity checks**: Základní kontroly proti tampering
- **Anti-emulator**: Detekce běhu na emulátoru
- **ProGuard obfuscation**: Pokročilá obfuskace kódu
- **Debug protection**: Odlišné buildy pro debug/release

#### 5. **Síťová bezpečnost**
- **Moderní ConnectivityManager**: Callback-based monitoring
- **Network type detection**: Rozpoznání WiFi/Cellular/Ethernet
- **Secure connection check**: Kontrola zabezpečení sítě
- **Flow-based monitoring**: Reactive síťové sledování

## 🚀 NASAZENÍ V PRODUKCI

### **KROK 1: Environmentální proměnné**
Před nasazením nastavte bezpečné přihlašovací údaje:

```bash
export TOBISO_API_USERNAME="your_production_username"
export TOBISO_API_PASSWORD="your_secure_production_password"
```

### **KROK 2: Certificate Pinning**
1. Získejte SHA-256 hash certifikátu pro tobiso.com:
```bash
openssl s_client -connect tobiso.com:443 < /dev/null | openssl x509 -fingerprint -noout -sha256
```

2. Aktualizujte `network_security_config.xml`:
```xml
<pin digest="SHA-256">SKUTEČNÝ_HASH_CERTIFIKÁTU</pin>
```

### **KROK 3: Keystore podpis**
Ujistěte se, že keystore.properties obsahuje produkční údaje:
```properties
storePassword=SECURE_PRODUCTION_PASSWORD
keyPassword=SECURE_PRODUCTION_PASSWORD  
keyAlias=production_key_alias
storeFile=/path/to/production/keystore.jks
```

### **KROK 4: Release build**
```bash
./gradlew assembleRelease
```

## 🔧 KONFIGURACE PRO RŮZNÁ PROSTŘEDÍ

### **Debug Build**
- Trust-all SSL certificates (pro dev servery)
- Podrobné logování
- Debug network config
- ApplicationId suffix `.debug`

### **Release Build**
- Pouze validní SSL certifikáty
- Minimální logování
- Produkční network security
- Plná ProGuard obfuskace
- Podepsané APK

## 🛡️ BEZPEČNOSTNÍ KONTROLY

### **Při spuštění aplikace:**
1. ✅ Kontrola integrity aplikace
2. ✅ Ověření package name
3. ✅ Detekce emulátoru
4. ✅ Síťové zabezpečení

### **Při API komunikaci:**
1. ✅ SSL certificate validation
2. ✅ Security headers
3. ✅ Request authentication
4. ✅ Error handling

### **Pro offline data:**
1. ✅ AES-256-GCM šifrování
2. ✅ Android KeyStore
3. ✅ Secure file storage
4. ✅ Key management

## 📊 BEZPEČNOSTNÍ TŘÍDY

### **SecurityConfig**
- Centralizovaná bezpečnostní konfigurace
- Environment-based credentials
- Integrity checks
- Anti-tampering protection

### **EncryptionManager** 
- Android KeyStore integration
- AES-256-GCM encryption
- Secure data storage
- Key lifecycle management

### **NetworkUtils**
- Modern connectivity monitoring  
- Network security validation
- Flow-based state management
- Connection type detection

## ⚠️ DŮLEŽITÉ POZNÁMKY

### **PRO PRODUKCI:**
1. **NIKDY** nepoužívejte hardcoded credentials
2. **VŽDY** implementujte certificate pinning
3. **AKTIVUJTE** všechny ProGuard optimalizace
4. **TESTUJTE** na různých Android verzích

### **PRO VÝVOJ:**
1. Debug buildy mají relaxovanou SSL konfiguraci
2. Používejte `.debug` suffix pro testing
3. Environmentální proměnné pro API credentials
4. Network security config pro dev servery

### **MONITOROVÁNÍ:**
1. Logování bezpečnostních událostí
2. Sledování neúspěšných API volání
3. Detekce podezřelých aktivit
4. Crash reporting s bezpečnostním kontextem

## 🔄 AKTUALIZACE BEZPEČNOSTI

### **Pravidelné úkoly:**
- [ ] Obnovení SSL certifikátů (každý rok)
- [ ] Rotace API credentials (každé 3 měsíce)
- [ ] Update dependency dependencies (měsíčně)
- [ ] Security audit (čtvrtletně)

### **Při detekci hrozby:**
- [ ] Okamžité obnovení API credentials
- [ ] Vydání security update
- [ ] Notifikace uživatelů
- [ ] Audit bezpečnostních logů

---

*Dokument aktualizován: Prosinec 2024*
*Verze bezpečnostní implementace: 2.0*