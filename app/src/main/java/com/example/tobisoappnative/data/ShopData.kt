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
            
            // Citáty kategorie
            ShopItem(
                id = 10,
                name = "Robert Collier",
                description = "Americký spisovatel a propagátor pozitivního myšlení",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Úspěch je součet malých úsilí opakovaných den za dnem. - Robert Collier"
            ),
            ShopItem(
                id = 11,
                name = "Benjamin Franklin",
                description = "Americký politik, vědec a jeden ze zakladatelů USA",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Vzdělání je nejlepší investice do budoucnosti. - Benjamin Franklin"
            ),
            ShopItem(
                id = 12,
                name = "Hérakleitos",
                description = "Starověký řecký filozof",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Jediná konstanta ve vesmíru je změna. - Hérakleitos"
            ),
            ShopItem(
                id = 13,
                name = "Albert Einstein",
                description = "Teoretický fyzik a nositel Nobelovy ceny",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Fantazie je důležitější než vědomosti. - Albert Einstein"
            ),
            ShopItem(
                id = 14,
                name = "Nelson Mandela",
                description = "Jihoafrický prezident a bojovník proti apartheidu",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Vzdělání je nejmocnější zbraň, kterou můžete použít ke změně světa. - Nelson Mandela"
            ),
            ShopItem(
                id = 15,
                name = "Aristoteles",
                description = "Starověký řecký filozof a učitel Alexandra Velikého",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Kvalita není čin, ale návyk. - Aristoteles"
            ),
            ShopItem(
                id = 16,
                name = "Walt Disney",
                description = "Americký animátor a zakladatel Disney Company",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Způsob, jak začít, je přestat mluvit a začít dělat. - Walt Disney"
            ),
            ShopItem(
                id = 17,
                name = "Maya Angelou",
                description = "Americká spisovatelka a aktivistka za občanská práva",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Lidé zapomenou, co jste řekli, lidé zapomenou, co jste udělali, ale nikdy nezapomenou, jak jste jim připadali. - Maya Angelou"
            ),
            ShopItem(
                id = 18,
                name = "Steve Jobs",
                description = "Spoluzakladatel a CEO společnosti Apple",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Inovace rozlišuje vůdce od následovníka. - Steve Jobs"
            ),
            ShopItem(
                id = 19,
                name = "Mahatma Gandhi",
                description = "Indický duchovní vůdce a bojovník za nezávislost",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Buď změnou, kterou chceš vidět ve světě. - Mahatma Gandhi"
            ),
            ShopItem(
                id = 50,
                name = "Winston Churchill",
                description = "Britský premiér během druhé světové války",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Úspěch není konečný, neúspěch není fatální: důležitá je odvaha pokračovat. - Winston Churchill"
            ),
            ShopItem(
                id = 51,
                name = "Theodore Roosevelt",
                description = "26. prezident Spojených států amerických",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Věř, že můžeš, a jsi na půli cesty. - Theodore Roosevelt"
            ),
            ShopItem(
                id = 52,
                name = "Mark Twain",
                description = "Americký spisovatel a humorista",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Odvaha není absence strachu, ale zvládnutí strachu. - Mark Twain"
            ),
            ShopItem(
                id = 53,
                name = "Helen Keller",
                description = "Americká spisovatelka a aktivistka pro práva postižených",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Život je buď odvážné dobrodružství, nebo nic. - Helen Keller"
            ),
            ShopItem(
                id = 54,
                name = "Leonardo da Vinci",
                description = "Italský renesanční génius, malíř a vynálezce",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Učení nikdy nevyčerpá mysl. - Leonardo da Vinci"
            ),
            ShopItem(
                id = 55,
                name = "Konfucius",
                description = "Čínský filozof a učitel",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Skutečné vědění je znát rozsah své neznalosti. - Konfucius"
            ),
            ShopItem(
                id = 56,
                name = "Oscar Wilde",
                description = "Irský spisovatel a dramatik",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Buď sám sebou; všichni ostatní už jsou obsazeni. - Oscar Wilde"
            ),
            ShopItem(
                id = 57,
                name = "Maya Angelou",
                description = "Americká spisovatelka a aktivistka za občanská práva",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Pokud se ti něco nelíbí, změň to. Pokud to nemůžeš změnit, změň svůj postoj. - Maya Angelou"
            ),
            ShopItem(
                id = 58,
                name = "Ralph Waldo Emerson",
                description = "Americký esejista a filozof",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Co leží za námi a co leží před námi, jsou malé věci ve srovnání s tím, co leží v nás. - Ralph Waldo Emerson"
            ),
            ShopItem(
                id = 59,
                name = "Franklin D. Roosevelt",
                description = "32. prezident Spojených států amerických",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Jediné, čeho se máme bát, je strach sám. - Franklin D. Roosevelt"
            ),
            ShopItem(
                id = 60,
                name = "Vince Lombardi",
                description = "Legendární americký fotbalový trenér",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Dokonalost není dosažitelná, ale pokud se snažíme o dokonalost, můžeme dosáhnout excellence. - Vince Lombardi"
            ),
            ShopItem(
                id = 61,
                name = "John F. Kennedy",
                description = "35. prezident Spojených států amerických",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Neptej se, co může tvoje země udělat pro tebe, ale co můžeš udělat ty pro svou zemi. - John F. Kennedy"
            ),
            ShopItem(
                id = 62,
                name = "Socrates",
                description = "Starověký řecký filozof",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Vím, že nic nevím. - Socrates"
            ),
            ShopItem(
                id = 63,
                name = "Martin Luther King Jr.",
                description = "Americký pastor a aktivista za občanská práva",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Temnota nemůže vyhnat temnotu: pouze světlo to dokáže. - Martin Luther King Jr."
            ),
            ShopItem(
                id = 64,
                name = "Oprah Winfrey",
                description = "Americká mediální osobnost a filantropka",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Největší objevení všech časů je, že člověk může změnit svou budoucnost pouhým změněním svého postoje. - Oprah Winfrey"
            ),
            ShopItem(
                id = 65,
                name = "Muhammad Ali",
                description = "Legendární americký boxer",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Nemožné je jen názor. - Muhammad Ali"
            ),
            ShopItem(
                id = 66,
                name = "Buddha",
                description = "Zakladatel buddhismu",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Mysl je vše. Čím si myslíš, tím se stáváš. - Buddha"
            ),
            ShopItem(
                id = 67,
                name = "Marcus Aurelius",
                description = "Římský císař a stoický filozof",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Velmi málo je potřeba k šťastnému životu; je to vše v tobě, ve tvém způsobu myšlení. - Marcus Aurelius"
            ),
            ShopItem(
                id = 68,
                name = "Eleanor Roosevelt",
                description = "Americká první dáma a aktivistka za lidská práva",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Nikdo tě nemůže ponížit bez tvého souhlasu. - Eleanor Roosevelt"
            ),
            ShopItem(
                id = 69,
                name = "Viktor Frankl",
                description = "Rakouský neurolog, psychiatr a filozof",
                price = 0,
                category = ShopCategory.QUOTES,
                type = ShopItemType.PROFILE_QUOTE,
                quote = "Všechno ti může být vzato kromě jedné věci: tvé svobody vybrat si svůj postoj v jakékoli situaci. - Viktor Frankl"
            ),
            
            // Balíčky ikon kategorie
            ShopItem(
                id = 20,
                name = "Emoji balíček",
                description = "Veselé emoji ikony pro všechny předměty",
                price = 0, // DEBUG - pro testování zdarma
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.ICON_PACK,
                iconPackType = IconPackType.EMOJI,
                subjectIcons = listOf(
                    SubjectIcon("Mluvnice", "📝", IconPackType.EMOJI),
                    SubjectIcon("Literatura", "📚", IconPackType.EMOJI),
                    SubjectIcon("Sloh", "📄", IconPackType.EMOJI), // Změněno z ✍️ na 📄
                    SubjectIcon("Hudební výchova", "🎵", IconPackType.EMOJI),
                    SubjectIcon("Matematika", "🔢", IconPackType.EMOJI),
                    SubjectIcon("Chemie", "🧪", IconPackType.EMOJI),
                    SubjectIcon("Fyzika", "⚡", IconPackType.EMOJI),
                    SubjectIcon("Přírodopis", "🌿", IconPackType.EMOJI),
                    SubjectIcon("Zeměpis", "🌍", IconPackType.EMOJI)
                )
            ),
            ShopItem(
                id = 21,
                name = "Moderní balíček",
                description = "Stylové Material Design ikony",
                price = 0, // DEBUG - normálně 75 (7.5 lekcí)
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.ICON_PACK,
                iconPackType = IconPackType.MATERIAL_ICONS,
                subjectIcons = listOf(
                    SubjectIcon("Mluvnice", "edit", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Literatura", "library_books", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Sloh", "article", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Hudební výchova", "music_note", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Matematika", "functions", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Chemie", "biotech", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Fyzika", "bolt", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Přírodopis", "local_florist", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Zeměpis", "language", IconPackType.MATERIAL_ICONS)
                )
            ),
            ShopItem(
                id = 22,
                name = "Premium balíček",
                description = "Exkluzivní emoji ikony s gradientem",
                price = 0, // DEBUG - normálně 100 (10 lekcí)
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.ICON_PACK,
                iconPackType = IconPackType.EMOJI,
                subjectIcons = listOf(
                    SubjectIcon("Mluvnice", "✏️", IconPackType.EMOJI), // Změněno z 🖋️
                    SubjectIcon("Literatura", "📖", IconPackType.EMOJI),
                    SubjectIcon("Sloh", "📄", IconPackType.EMOJI),
                    SubjectIcon("Hudební výchova", "🎼", IconPackType.EMOJI),
                    SubjectIcon("Matematika", "📐", IconPackType.EMOJI),
                    SubjectIcon("Chemie", "⚗️", IconPackType.EMOJI),
                    SubjectIcon("Fyzika", "🔬", IconPackType.EMOJI),
                    SubjectIcon("Přírodopis", "🦋", IconPackType.EMOJI),
                    SubjectIcon("Zeměpis", "🌎", IconPackType.EMOJI) // Změněno z 🗺️
                )
            ),
            ShopItem(
                id = 23,
                name = "Klasické ikony",
                description = "Původní Material Design ikony z výchozího nastavení",
                price = 0, // DEBUG - normálně 50 (5 lekcí)
                category = ShopCategory.SUBJECTS,
                type = ShopItemType.ICON_PACK,
                iconPackType = IconPackType.MATERIAL_ICONS,
                subjectIcons = listOf(
                    SubjectIcon("Mluvnice", "spellcheck", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Literatura", "menu_book", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Sloh", "description", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Hudební výchova", "library_music", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Matematika", "calculate", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Chemie", "science", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Fyzika", "precision_manufacturing", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Přírodopis", "eco", IconPackType.MATERIAL_ICONS),
                    SubjectIcon("Zeměpis", "public", IconPackType.MATERIAL_ICONS)
                )
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
                cooldownMinutes = 300,
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
                cooldownMinutes = 300,
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
                cooldownMinutes = 300,
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
            ),
            ShopItem(
                id = 48,
                name = "Myška Roztomilá",
                description = "Malá, rychlá a chytrá myška, která ti pomůže najít nejkratší cesty k poznání",
                price = 0, // DEBUG - normálně 45 (4.5 lekce)
                category = ShopCategory.PETS,
                type = ShopItemType.PET,
                petIcon = "🐭"
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