package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.components.Card
import androidx.compose.ui.unit.sp
import android.content.Intent
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.navigation.NavController
import com.tobiso.tobisoappnative.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
    val context = LocalContext.current
    val packageInfo = remember {
        try {
            context.packageManager.getPackageInfo(context.packageName, 0)
        } catch (e: Exception) {
            null
        }
    }
    val currentVersion = packageInfo?.versionName ?: "?"
    val libraries = listOf(
        "androidx.core:core-ktx:1.12.0",
        "androidx.lifecycle:lifecycle-runtime-ktx:2.7.0",
        "androidx.activity:activity-compose:1.8.2",
        "androidx.compose.ui:ui",
        "androidx.compose.ui:ui-graphics",
        "androidx.compose.ui:ui-tooling-preview",
        "androidx.compose.material3:material3",
        "androidx.compose.material:material-icons-extended",
        "androidx.navigation:navigation-compose:2.8.0",
        "androidx.core:core-splashscreen:1.0.1",
        "com.squareup.retrofit2:retrofit:2.9.0",
        "org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.3",
        "com.jakewharton.retrofit:retrofit2-kotlin-coroutines-adapter:0.9.2",
        "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3",
        "io.coil-kt:coil-compose:2.4.0",
        "com.halilibo.compose-richtext:richtext-ui-material3:1.0.0-alpha03",
        "com.halilibo.compose-richtext:richtext-commonmark:1.0.0-alpha03",
        "com.google.accompanist:accompanist-navigation-animation:0.32.0",
        "com.google.android.gms:play-services-oss-licenses:17.0.1",
        "androidx.compose.ui:ui-text-google-fonts:1.8.1",
        "androidx.media3:media3-exoplayer:1.8.0",
        "androidx.media3:media3-ui:1.8.0",
        "com.google.accompanist:accompanist-swiperefresh:0.33.2-alpha",
        "androidx.datastore:datastore-preferences:1.0.0",
        "com.google.accompanist:accompanist-systemuicontroller:0.32.0",
        "androidx.work:work-runtime-ktx:2.9.0",
        "implementation libs.androidx.compose.foundation",

        "junit:junit:4.13.2",
        "androidx.test.ext:junit:1.1.5",
        "androidx.test.espresso:espresso-core:3.5.1",
        "androidx.compose.ui:ui-test-junit4",
        "androidx.compose.ui:ui-tooling",
        "androidx.compose:compose-bom:2023.10.01",
        "androidx.compose.ui:ui-test-manifest",
    )
    val isDark = isSystemInDarkTheme()
    val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = { Text("O aplikaci", style = com.tobiso.tobisoappnative.ui.theme.SecondaryTopBarTitle) },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Zpět")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Text(
                "Tobiso",
                fontFamily = com.tobiso.tobisoappnative.ui.theme.Baloo2,
                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                fontSize = 28.sp
            )
            Text("Autor: Taneq (Tobias) · v$currentVersion", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            Box(Modifier.fillMaxWidth()) { com.tobiso.tobisoappnative.components.SectionLabel("Popis aplikace", Modifier.padding(start = 4.dp)) }
            Card(Modifier.fillMaxWidth()) {
                Text(
                    "Tato aplikace zobrazuje obsah z Tobiso.com a její vývoj bude pokračovat. Aplikace nemá žádnou telemetrii ani něco jako cookies. Jak už je Tobiso.com, tato aplikace je také primárně zaměřena na jednu školu, a to momentálně do 9. ročníku základní školy. K dispozici jsou již speciální funkce jako ukládání článků a útržků, offline režim, bodový systém pro otázky, řada otevření aplikace a s tím odměny, profil, obchod a aktovka, procvičování a kalendář událostí, kdy není škola + vaše události. Další budou pokračovat!",
                    fontSize = 15.sp, lineHeight = 23.sp,
                    modifier = Modifier.padding(16.dp)
                )
            }

            Box(Modifier.fillMaxWidth()) { com.tobiso.tobisoappnative.components.SectionLabel("Licence", Modifier.padding(start = 4.dp)) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("All rights reserved", fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold, fontSize = 15.sp)
                    Text("Tento software nesmí být kopírován, upravován ani distribuován bez výslovného svolení autora. Kontribuce přes platformu Github jsou vítány.", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            Box(Modifier.fillMaxWidth()) { com.tobiso.tobisoappnative.components.SectionLabel("Použité knihovny", Modifier.padding(start = 4.dp)) }
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    libraries.forEach { lib ->
                        Text(lib, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}