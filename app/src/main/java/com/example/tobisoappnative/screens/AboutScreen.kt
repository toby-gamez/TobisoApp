package com.example.tobisoappnative.screens

import android.content.Intent
import com.google.android.gms.oss.licenses.OssLicensesMenuActivity
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import androidx.navigation.NavController
import com.example.tobisoappnative.R
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(navController: NavController) {
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
        "com.squareup.retrofit2:converter-gson:2.9.0",
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

        "junit:junit:4.13.2",
        "androidx.test.ext:junit:1.1.5",
        "androidx.test.espresso:espresso-core:3.5.1",
        "androidx.compose.ui:ui-test-junit4",
        "androidx.compose.ui:ui-tooling",
        "androidx.compose.ui:ui-test-manifest",
    )
    val isDark = isSystemInDarkTheme()
    val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light

    // ✅ Odstraněn Scaffold - padding se aplikuje z MainActivity
    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        LargeTopAppBar(
            title = { Text("O aplikaci") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zpět")
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(id = logoRes),
                contentDescription = "Logo",
                modifier = Modifier.size(120.dp)
            )
            Text("Autor: Taneq (Tobias)", style = MaterialTheme.typography.bodyMedium)
            Text("v1.7", style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(24.dp))
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "Stručné informace",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text("Tato aplikace zobrazuje obsah z Tobiso.com a její vývoj bude pokračovat. Aplikace nemá žádnou telemetrii ani něco jako cookies. Jak už je Tobiso.com, tato aplikace je také primárně zaměřena na jednu školu, a to momentálně do 8. ročníku základní školy. K dispozici jsou již speciální funkce jako ukládání článků a útržků. Další budou pokračovat!", style = MaterialTheme.typography.bodyMedium)
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "Licence",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text("All rights reserved", style = MaterialTheme.typography.bodyMedium)
            Text("Tento software nesmí být kopírován, upravován ani distribuován bez výslovného svolení autora. Kontribuce přes platformu Github jsou vítány.", style = MaterialTheme.typography.bodyMedium)
            Divider(
                color = Color.Gray,
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 8.dp)
            )
            Text(
                text = "Použité knihovny",
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
            Column {
                libraries.forEach { lib ->
                    Text(lib, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}