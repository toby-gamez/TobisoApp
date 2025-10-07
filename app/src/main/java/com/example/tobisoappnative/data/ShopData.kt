package com.example.tobisoappnative.data

import com.example.tobisoappnative.model.*

object ShopData {
    
    fun getShopItems(): List<ShopItem> {
        return listOf(
            // Streak kategorie
            ShopItem(
                id = 1,
                name = "Streak Freeze",
                description = "Ochráni svoji řadu před přerušením na jeden den",
                price = 50,
                category = ShopCategory.STREAK,
                type = ShopItemType.STREAK_FREEZE
            ),
            ShopItem(
                id = 2,
                name = "Týdenní Streak Freeze",
                description = "Ochráni svoji řadu před přerušením na celý týden",
                price = 300,
                category = ShopCategory.STREAK,
                type = ShopItemType.STREAK_FREEZE
            ),
            
            // Profil kategorie - citáty
            ShopItem(
                id = 10,
                name = "Motivační citát",
                description = "\"Úspěch je součet malých úsilí opakovaných den za dnem.\"",
                price = 25,
                category = ShopCategory.PROFILE,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Úspěch je součet malých úsilí opakovaných den za dnem."
            ),
            ShopItem(
                id = 11,
                name = "Inspirující citát",
                description = "\"Vzdělání je nejlepší investice do budoucnosti.\"",
                price = 25,
                category = ShopCategory.PROFILE,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Vzdělání je nejlepší investice do budoucnosti."
            ),
            ShopItem(
                id = 12,
                name = "Filozofický citát",
                description = "\"Jediná konstanta ve vesmíru je změna.\"",
                price = 30,
                category = ShopCategory.PROFILE,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Jediná konstanta ve vesmíru je změna."
            ),
            
            // Předměty kategorie - ikony
            ShopItem(
                id = 20,
                name = "Matematická ikona",
                description = "Speciální ikona pro matematické předměty",
                price = 40,
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.SUBJECT_ICON,
                iconRes = android.R.drawable.ic_dialog_info // Placeholder
            ),
            ShopItem(
                id = 21,
                name = "Vědecká ikona",
                description = "Speciální ikona pro vědecké předměty",
                price = 40,
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.SUBJECT_ICON,
                iconRes = android.R.drawable.ic_dialog_info // Placeholder
            ),
            ShopItem(
                id = 22,
                name = "Jazyková ikona",
                description = "Speciální ikona pro jazykové předměty",
                price = 40,
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.SUBJECT_ICON,
                iconRes = android.R.drawable.ic_dialog_info // Placeholder
            ),
            
            // Power-upy kategorie
            ShopItem(
                id = 30,
                name = "1.5x Body",
                description = "Získávej 1.5x více bodů po dobu 15 minut",
                price = 75,
                category = ShopCategory.POWER_UPS,
                type = ShopItemType.POINTS_MULTIPLIER,
                durationMinutes = 15,
                multiplier = 1.5f,
                cooldownMinutes = 30
            ),
            ShopItem(
                id = 31,
                name = "2x Body",
                description = "Získávej dvojnásobné body po dobu 15 minut",
                price = 150,
                category = ShopCategory.POWER_UPS,
                type = ShopItemType.POINTS_MULTIPLIER,
                durationMinutes = 15,
                multiplier = 2.0f,
                cooldownMinutes = 60
            ),
            ShopItem(
                id = 32,
                name = "3x Body",
                description = "Získávej trojnásobné body po dobu 15 minut",
                price = 300,
                category = ShopCategory.POWER_UPS,
                type = ShopItemType.POINTS_MULTIPLIER,
                durationMinutes = 15,
                multiplier = 3.0f,
                cooldownMinutes = 120
            ),
            
            // Zvířátka kategorie
            ShopItem(
                id = 40,
                name = "Kočka Mourek",
                description = "Roztomilá kočka, která ti bude dělat společnost při učení",
                price = 150,
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                imageUrl = "https://picsum.photos/200/200?random=1"
            ),
            ShopItem(
                id = 41,
                name = "Pes Rexík",
                description = "Věrný společník, který tě bude motivovat k dalšímu učení",
                price = 180,
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                imageUrl = "https://picsum.photos/200/200?random=2"
            ),
            ShopItem(
                id = 42,
                name = "Králík Hopísek",
                description = "Rychlý králík, který ti dodá energii do studia",
                price = 120,
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                imageUrl = "https://picsum.photos/200/200?random=3"
            ),
            ShopItem(
                id = 43,
                name = "Sova Moudrá",
                description = "Moudrá sova, symbol vzdělání a vědomostí",
                price = 250,
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                imageUrl = "https://picsum.photos/200/200?random=4"
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