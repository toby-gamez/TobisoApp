# Room DB Migrations Checklist

Krátký checklist pro přidávání a testování migrací Room databáze.

1. Zvětšit `version` v `@Database` pouze pokud měníte schéma.
2. Přidejte konkrétní `Migration(oldVersion, newVersion)` implementaci do `DatabaseModule`.
   - V migraci používejte SQL přes `db.execSQL(...)` nebo `SupportSQLiteDatabase` API.
3. Nepoužívejte `fallbackToDestructiveMigration()` v production builds.
4. Přidejte jednotkové testy migrací:
   - Vytvořte testovací DB s původní verzí a datey.
   - Aplikujte migraci a ověřte očekávané změny/schéma.
5. Zvažte no-op migraci pro meziverze, pokud je potřeba udržet sekvenci verzí.
6. Před vydáním sestavte migrační plán pro existující uživatele (zálohy, rollback strategie).
7. Dokumentujte každou migraci v changelogu (co se mění a proč).

Tipy pro implementaci:
- Použijte `PRAGMA user_version` pro rychlé ověření verze v testech.
- Pro velké operace (např. přejmenování tabulek) používejte transakce a dočasné tabulky.

Použijte tento checklist jako základ pro PR, který přidává konkrétní migrace.
