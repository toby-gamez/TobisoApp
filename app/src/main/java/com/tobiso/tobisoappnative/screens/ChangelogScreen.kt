package com.tobiso.tobisoappnative.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
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

data class VersionInfo(
    val version: String,
    val changes: List<String>
)

@Composable
fun VersionSection(versionInfo: VersionInfo) {
    Text(
        text = "Verze ${versionInfo.version}",
        style = typography.headlineSmall,
        modifier = Modifier.padding(bottom = 8.dp, top = 16.dp)
    )
    
    versionInfo.changes.forEach { item ->
        BulletPoint(text = item)
    }
}

@Composable
fun BulletPoint(text: String) {
    Row(
        verticalAlignment = Alignment.Top,
        modifier = Modifier.padding(start = 16.dp, bottom = 4.dp)
    ) {
        Text(
            "•",
            style = typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(end = 8.dp, top = 2.dp)
        )
        Text(
            text,
            style = typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(navController: NavController) {
    val versions = listOf(
        VersionInfo("3.0", listOf(
            "opraveno načítání prověrek a cvičení",
            "kompletní implementace kontroly cvičení",
            "opraveny bugy a zrychlena aplikace",
            "modernější práce s daty aplikace",
            "integrace cvičení do systému bodů",
            "opraveno zobrazování souvisejících článků",
            "přidána umělá inteligence",
            "opraveny obrázky s pozadím",
            "vylepšeno UI",
            "znovupřidáno čtení článků",
            "velké optimalizace",
            "vylepšeno vyhledávání",
            "přidána podpora pro karty se zdroji a autory obrázků",
        )),
        VersionInfo("2.7", listOf(
            "přidána podpora pro obrázky z nového serveru",
            "přidáno renderování pro šipky a zlomky v textu",
            "přidána podpora pro interaktivní cvičení ve článcích",
            "přidáno pamatování stavu na hlavní obrazovce",
        )),
        VersionInfo("2.6.1", listOf(
            "zpětná vazba je nově nativní a nepoužívá Formspree",
        )),
        VersionInfo("2.6", listOf(
            "přidáno řazení podle nejnovějšího učiva",
            "přidán systém oprav/úprav článků",
            "přidáno schovávání vyhledávání při swipu dolů",
        )),
        VersionInfo("2.5.1", listOf(
            "opraveny pády aplikace při otevírání některých článků",
            "opraveny dodatky a jejich implementace v aplikace",
        )),
        VersionInfo("2.5", listOf(
            "informace o verzi aplikace v Aktualizátoru",
            "tisk článků v online režimu",
            "opraveny pády aplikace při otevírání některých článků",
            "přidány dodatky",
            "vylepšen systém souvisejících článků",
            "přidány související články do offline režimu"
        )),
        VersionInfo("2.4", listOf(
            "floating vyhledávání místo celé obrazovky",
            "vylepšen offline mode",
            "přidán Správce offline dat",
            "přeskládán profil a vylepšeny popisy"
        )),
        VersionInfo("2.3", listOf(
            "opraven přechod u Vybrat text",
            "přidáno čtení článků",
            "přidány funkce do offline režimu",
            "přidány algoritmy na zrychlování načítání kategorií, otázek a předmětů",
            "opraveno upravování obrázku při nahrávání",
            "lepší popis přírodopisu",
            "opraveny časy článků v celé aplikaci"
        )),
        VersionInfo("2.2.2", listOf(
            "přidána podpora pro : na klávesnici",
            "změněna logika pro zadávání a vyhledávání (není již závislé na diakritice)",
            "oprava vybírání textu a ukládání útržků",
        )),
        VersionInfo("2.2.1", listOf(
            "přidána úprava obrázku",
            "opraveny bubliny při neexistujících zvířátkách",
            "místo klávesnice mobilu je použita klávesnice vlastní"
        )),
        VersionInfo("2.2", listOf(
            "přidán obchod (zmražení řady, balíčky ikon, zvířátka atd.)",
            "přidán profil, odměny a odznaky",
            "opravena typografie a layout topbarů",
            "opravy gest zpět a klávesnic v otázkách"
        )),
        VersionInfo("2.1", listOf(
            "přidány související články",
            "přidáno procvičování, seznam otázek a předělán design prověrek",
            "přidány otázky do offline režimu",
            "opraven restart všech odpovědí při změně rotace telefonu v otázkách",
            "přidány odměny za milníky v řadě",
            "upraven Deník změn (design i funkčnost)",
            "oprava notifikací u školy",
            "při načítání dat přidány všude kolečka"
        )),
        VersionInfo("2.0.3", listOf(
            "oprava načítání videí a obrázků",
            "přidána podpora pro landscape a fullscreen dívání videa",
            "aktualizována stránka O aplikaci"
        )),
        VersionInfo("2.0.2", listOf(
            "oprava ochrany, aby neodpojovala api připojení",
            "lepší ochrana u citlivých údajů v aplikaci"
        )),
        VersionInfo("2.0.1", listOf(
            "oprava celého připojení k api, nastavavení kalendáře a událostí"
        )),
        VersionInfo("2.0", listOf(
            "přesunuto tlačítko na mazání z oblíbených",
            "přídán kalendář pro události",
            "přidána ochrana proti krádeži a padělání",
            "přidáno náhodné uspořádání otázek",
            "přidána podpora pro vytvoření svého vlastního kalendáře",
            "přidán plný offline režim"
        )),
        VersionInfo("1.9.2", listOf(
            "přidána podpora pro textové odpovídání otázek",
            "lepší přepojení na apk soubor v Aktualizátoru"
        )),
        VersionInfo("1.9.1", listOf(
            "přidána podpora pro vysvětlení u otázek",
            "oprava notifikací (čas a výjimky)",
            "oprava řady, aby se počítala správně",
            "zjednodušení aktualizátoru a opraveny chyby 'null'"
        )),
        VersionInfo("1.9", listOf(
            "přidána podpora pro otázky",
            "přidáno využití pro body"
        )),
        VersionInfo("1.8", listOf(
            "opravena ikona tak, aby podporovala dynamic i Nothing ikony",
            "přidána podpora pro posílání notifikací",
            "oprava animací obrazovek a navigace",
            "přidán aktualizátor"
        )),
        VersionInfo("1.7", listOf(
            "odstraněn problém se status barem",
            "přidána řada",
            "přidány notifikace pro učení",
            "přidány body pro otázky (ještě nejsou hotové)",
            "opraveny chyby v landscape módu"
        )),
        VersionInfo("1.6", listOf(
            "přidán počet slov a délka čtení pro článek",
            "přidáno gesto pro znovunačtení článku (swipe down)",
            "přidány oblíbené posty a útržky",
            "přidány možnosti v přispěvku pro uložení a sdílení"
        )),
        VersionInfo("1.5", listOf(
            "text se již dá vybírat a kopírovat (včetně intra a článků)",
            "přidána podpora pro otevírání souborů",
            "přidána podpora pro pouštění videí v přehrávači"
        )),
        VersionInfo("1.4", listOf(
            "opraveny tabulky",
            "opraveny intra a linky, přidána podpora pro weblinky",
            "opraveno číslo verze v android 'O aplikaci'",
            "opraven tmavý režim v některých elementech a obrazovkách",
            "upraveny popisy předmětů a vyhledávání"
        )),
        VersionInfo("1.3", listOf(
            "přidán deník změn",
            "přidáno vyhledávání",
            "přidána obrazovka o 'Bez internetu' a načítání",
            "zfunkčněny odkazy ve článcích",
            "zfunkčněny intra článků"
        )),
        VersionInfo("1.2", listOf(
            "přidán základ aplikace, nastavení všeho (list předmětů, navigace atd.)",
            "přidáno načítání kategorií a postů i jejich obsahu, načítání obrázků",
            "zabezpečené připojení s api",
            "přidán readme",
            "přidán lepší design",
            "oprava layoutů"
        ))
    )

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("Deník změn", style = MaterialTheme.typography.headlineLarge) },
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
            
            versions.forEach { versionInfo ->
                VersionSection(versionInfo = versionInfo)
            }
        }
    }
}