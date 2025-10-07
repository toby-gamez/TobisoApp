package com.example.tobisoappnative.data

import com.example.tobisoappnative.model.*

object ShopData {
    
    fun getShopItems(): List<ShopItem> {
        return listOf(
            // Streak kategorie
            ShopItem(
                id = 1,
                name = "Zmražení řady",
                description = "Ochráni svoji řadu před přerušením na jeden den",
                price = 0, // DEBUG - normálně 20 (2 lekce)
                category = ShopCategory.STREAK,
                type = ShopItemType.STREAK_FREEZE
            ),
            
            // Profil kategorie - citáty
            ShopItem(
                id = 10,
                name = "Motivační citát",
                description = "\"Úspěch je součet malých úsilí opakovaných den za dnem.\"",
                price = 0, // DEBUG - normálně 15 (1.5 lekce)
                category = ShopCategory.PROFILE,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Úspěch je součet malých úsilí opakovaných den za dnem."
            ),
            ShopItem(
                id = 11,
                name = "Inspirující citát",
                description = "\"Vzdělání je nejlepší investice do budoucnosti.\"",
                price = 0, // DEBUG - normálně 15 (1.5 lekce)
                category = ShopCategory.PROFILE,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Vzdělání je nejlepší investice do budoucnosti."
            ),
            ShopItem(
                id = 12,
                name = "Filozofický citát",
                description = "\"Jediná konstanta ve vesmíru je změna.\"",
                price = 0, // DEBUG - normálně 20 (2 lekce)
                category = ShopCategory.PROFILE,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Jediná konstanta ve vesmíru je změna."
            ),
            
            // Předměty kategorie - ikony
            ShopItem(
                id = 20,
                name = "Matematická ikona",
                description = "Speciální ikona pro matematické předměty",
                price = 0, // DEBUG - normálně 25 (2.5 lekce)
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.SUBJECT_ICON,
                iconRes = android.R.drawable.ic_dialog_info // Placeholder
            ),
            ShopItem(
                id = 21,
                name = "Vědecká ikona",
                description = "Speciální ikona pro vědecké předměty",
                price = 0, // DEBUG - normálně 25 (2.5 lekce)
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.SUBJECT_ICON,
                iconRes = android.R.drawable.ic_dialog_info // Placeholder
            ),
            ShopItem(
                id = 22,
                name = "Jazyková ikona",
                description = "Speciální ikona pro jazykové předměty",
                price = 0, // DEBUG - normálně 25 (2.5 lekce)
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.SUBJECT_ICON,
                iconRes = android.R.drawable.ic_dialog_info // Placeholder
            ),
            
            // Power-upy kategorie
            ShopItem(
                id = 30,
                name = "1.5x Body",
                description = "Získávej 1.5x více bodů po dobu 15 minut",
                price = 0, // DEBUG - normálně 30 (3 lekce)
                category = ShopCategory.POWER_UPS,
                type = ShopItemType.POINTS_MULTIPLIER,
                durationMinutes = 15,
                multiplier = 1.5f,
                cooldownMinutes = 30,
                powerUpIcon = "1.5x" // Aqua
            ),
            ShopItem(
                id = 31,
                name = "2x Body",
                description = "Získávej dvojnásobné body po dobu 15 minut",
                price = 0, // DEBUG - normálně 50 (5 lekcí)
                category = ShopCategory.POWER_UPS,
                type = ShopItemType.POINTS_MULTIPLIER,
                durationMinutes = 15,
                multiplier = 2.0f,
                cooldownMinutes = 60,
                powerUpIcon = "2x" // Zlaté
            ),
            ShopItem(
                id = 32,
                name = "3x Body",
                description = "Získávej trojnásobné body po dobu 15 minut",
                price = 0, // DEBUG - normálně 80 (8 lekcí)
                category = ShopCategory.POWER_UPS,
                type = ShopItemType.POINTS_MULTIPLIER,
                durationMinutes = 15,
                multiplier = 3.0f,
                cooldownMinutes = 120,
                powerUpIcon = "3x" // Fialové
            ),
            
            // Zvířátka kategorie - používáme Material ikony
            ShopItem(
                id = 40,
                name = "Kočka Mourek",
                description = "Roztomilá kočka, která ti bude dělat společnost při učení",
                price = 0, // DEBUG - normálně 60 (6 lekcí)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🐱"
            ),
            ShopItem(
                id = 41,
                name = "Pes Rexík",
                description = "Věrný společník, který tě bude motivovat k dalšímu učení",
                price = 0, // DEBUG - normálně 70 (7 lekcí)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🐶"
            ),
            ShopItem(
                id = 42,
                name = "Králík Hopísek",
                description = "Rychlý králík, který ti dodá energii do studia",
                price = 0, // DEBUG - normálně 50 (5 lekcí)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🐰"
            ),
            ShopItem(
                id = 43,
                name = "Sova Moudrá",
                description = "Moudrá sova, symbol vzdělání a vědomostí",
                price = 0, // DEBUG - normálně 100 (10 lekcí)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🦉"
            ),
            ShopItem(
                id = 44,
                name = "Liška Chytrá",
                description = "Mazaná liška, která ti pomůže s řešením těžkých úkolů",
                price = 0, // DEBUG - normálně 80 (8 lekcí)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🦊"
            ),
            ShopItem(
                id = 45,
                name = "Panda Klidná",
                description = "Klidná panda, která ti dodá zen při studiu",
                price = 0, // DEBUG - normálně 90 (9 lekcí)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🐼"
            ),
            ShopItem(
                id = 46,
                name = "Delfín Inteligentní",
                description = "Chytrý delfín, který ti pomůže s koncentrací",
                price = 0, // DEBUG - normálně 85 (8.5 lekcí)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🐬"
            )
        )
    }
    
    fun getItemsByCategory(category: ShopCategory): List<ShopItem> {
        return getShopItems().filter { it.category == category }
    }
    
    fun getItemById(itemId: Int): ShopItem? {
        return getShopItems().find { it.id == itemId }
    }
}