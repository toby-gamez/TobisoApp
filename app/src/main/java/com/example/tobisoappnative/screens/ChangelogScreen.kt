package com.example.tobisoappnative.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme.typography
import androidx.compose.foundation.text.ClickableText
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(navController: NavController) {
    val version2_1 = listOf(
        "přidány související články",
        "přidáno procvičování a seznam otázek a předělán design prověrek",
        "přidány otázky do offline režimu",
        "opraven restart všech odpovědí při změně rotace telefonu v otázkách"
    )
    val version2_0_3 = listOf(
        "oprava načítání videí a obrázků",
        "přidána podpora pro landscape a fullscreen dívání videa",
        "aktualizována stránka O aplikaci"
    )
    val version2_0_2 = listOf(
        "oprava ochrany, aby neodpojovala api připojení",
        "lepší ochrana u citlivých údajů v aplikaci"
    )
    val version2_0_1 = listOf (
        "oprava celého připojení k api, nastavavení kalendáře a událostí"
    )
    val version2_0 = listOf(
        "přesunuto tlačítko na mazání z oblíbených",
        "přídán kalendář pro události",
        "přidána ochrana proti krádeži a padělání",
        "přidáno náhodné uspořádání otázek",
        "přidána podpora pro vytvoření svého vlastního kalendáře",
        "přidán plný offline režim",
    )
    val version1_9_2 = listOf(
        "přidána podpora pro textové odpovídání otázek",
        "lepší přepojení na apk soubor v Aktualizátoru"
    )
    val version1_9_1 = listOf(
        "přidána podpora pro vysvětlení u otázek",
        "oprava notifikací (čas a výjimky)",
        "oprava řady, aby se počítala správně",
        "zjednodušení aktualizátoru a opraveny chyby 'null'"
    )
    val version1_9 = listOf(
        "přidána podpora pro otázky",
        "přidáno využití pro body"
    )
    val version1_8 = listOf(
        "opravena ikona tak, aby podporovala dynamic i Nothing ikony",
        "přidána podpora pro posílání notifikací",
        "oprava animací obrazovek a navigace",
        "přidán aktualizátor"
    )
    val version1_7 = listOf(
        "odstraněn problém se status barem",
        "přidána řada",
        "přidány notifikace pro učení",
        "přidány body pro otázky (ještě nejsou hotové)",
        "opraveny chyby v landscape módu",
    )
    val version1_6 = listOf(
        "přidán počet slov a délka čtení pro článek",
        "přidáno gesto pro znovunačtení článku (swipe down)",
        "přidány oblíbené posty a útržky",
        "přidány možnosti v přispěvku pro uložení a sdílení",
    )
    val version1_5 = listOf(
        "text se již dá vybírat a kopírovat (včetně intra a článků)",
        "přidána podpora pro otevírání souborů",
        "přidána podpora pro pouštění videí v přehrávači"
    )
    val version1_4 = listOf(
        "opraveny tabulky",
        "opraveny intra a linky, přidána podpora pro weblinky",
        "opraveno číslo verze v android 'O aplikaci'",
        "opraven tmavý režim v některých elementech a obrazovkách",
        "upraveny popisy předmětů a vyhledávání",
    )
    val version1_3 = listOf(
        "přidán deník změn",
        "přidáno vyhledávání",
        "přidána obrazovka o 'Bez internetu' a načítání",
        "zfunkčněny odkazy ve článcích",
        "zfunkčněny intra článků",
    )
    val version1_2 = listOf(
        "přidán základ aplikace, nastavení všeho (list předmětů, navigace atd.)",
        "přidáno načítání kategorií a postů i jejich obsahu, načítání obrázků",
        "zabezpečené připojení s api",
        "přidán readme",
        "přidán lepší design",
        "oprava layoutů",
    )

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LargeTopAppBar(
            title = { Text("Deník změn") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
                .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.Start
        ) {
            val context = LocalContext.current

            val annotatedText = buildAnnotatedString {
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.onBackground)) {
                    append("Github ")
                }
                pushStringAnnotation(tag = "URL", annotation = "https://github.com/toby-gamez/TobisoAppNative")
                withStyle(style = SpanStyle(color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)) {
                    append("zde")
                }
                pop()
            }

            ClickableText(
                text = annotatedText,
                onClick = { offset ->
                    annotatedText
                        .getStringAnnotations(tag = "URL", start = offset, end = offset)
                        .firstOrNull()
                        ?.let { annotation ->
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(annotation.item))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) // důležité pokud spouštíš z Contextu
                            context.startActivity(intent)
                        }
                }
            )
            Text("Verze 2.1", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version2_1.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 2.0.3", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version2_0_3.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 2.0.2", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version2_0_2.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 2.0.1", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version2_0_1.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 2.0", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version2_0.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.9.2", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_9_2.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.9.1", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_9_1.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.9", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_9.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.8", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_8.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.7", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_7.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.6", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_6.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.5", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_5.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.4", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_4.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.3", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_3.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
            Text("Verze 1.2", style = typography.headlineSmall, modifier = Modifier.padding(bottom = 4.dp))
            version1_2.forEach { item ->
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 16.dp)) {
                    Text("• $item", style = typography.bodyLarge)
                }
            }
        }
    }
}