#!/bin/bash

# 🔒 SSL Certificate Hash Generator pro TobisoApp
# Tento skript získá SHA-256 hash SSL certifikátu pro certificate pinning

echo "🔒 Získávání SSL certificate hash pro tobiso.com..."
echo "=================================================="

# Funkce pro získání certificate hash
get_cert_hash() {
    local domain=$1
    echo "📋 Zpracovávám doménu: $domain"
    
    # Získání certifikátu
    echo "📡 Stahování certifikátu..."
    cert_info=$(echo | openssl s_client -connect $domain:443 -servername $domain 2>/dev/null | openssl x509 -noout -pubkey 2>/dev/null)
    
    if [ $? -eq 0 ]; then
        # Výpočet SHA-256 hash
        hash=$(echo "$cert_info" | openssl pkey -pubin -outform DER 2>/dev/null | openssl dgst -sha256 -binary | base64)
        
        if [ ! -z "$hash" ]; then
            echo "✅ SHA-256 Hash pro $domain: $hash"
            echo ""
            echo "📋 Pro network_security_config.xml použij:"
            echo "<pin digest=\"SHA-256\">$hash</pin>"
            echo ""
        else
            echo "❌ Nepodařilo se vypočítat hash pro $domain"
        fi
    else
        echo "❌ Nepodařilo se stáhnout certifikát pro $domain"
    fi
}

# Získej hash pro hlavní domény
get_cert_hash "tobiso.com"
get_cert_hash "www.tobiso.com"

echo "=================================================="
echo "🎯 INSTRUKCE PRO IMPLEMENTACI:"
echo ""
echo "1. Zkopíruj SHA-256 hash hodnoty výše"
echo "2. Otevři: app/src/main/res/xml/network_security_config.xml"
echo "3. Nahraď placeholder hodnoty v <pin-set> sekci"
echo "4. Odkomentuj <pin-set> blok"
echo "5. Nastav expiration datum na příští rok"
echo ""
echo "⚠️  DŮLEŽITÉ:"
echo "   - První hash je pro hlavní certifikát"
echo "   - Doporučujeme mít backup pin pro případ obnovení certifikátu"
echo "   - Otestuj aplikaci před nasazením do produkce"
echo ""
echo "📖 Více informací v SECURITY.md"