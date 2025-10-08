package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.tobisoappnative.viewmodel.MainViewModel
import com.example.tobisoappnative.model.Post
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Stars
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.components.MultiplierIndicator
import com.example.tobisoappnative.BackpackManager
import androidx.compose.material.icons.filled.FormatQuote
import kotlinx.coroutines.delay
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.filled.Backpack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Image
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.net.Uri
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.graphics.Color
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalDensity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer

// Helper funkce pro správu profilu
fun getProfileName(context: android.content.Context): String {
    val prefs = context.getSharedPreferences("ProfilePrefs", android.content.Context.MODE_PRIVATE)
    return prefs.getString("profile_name", "Chytrá věc") ?: "Chytrá věc"
}

fun saveProfileName(context: android.content.Context, name: String) {
    val prefs = context.getSharedPreferences("ProfilePrefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putString("profile_name", name).apply()
}

fun getProfileImageUri(context: android.content.Context): String? {
    val prefs = context.getSharedPreferences("ProfilePrefs", android.content.Context.MODE_PRIVATE)
    return prefs.getString("profile_image_uri", null)
}

fun saveProfileImageUri(context: android.content.Context, uri: String?) {
    val prefs = context.getSharedPreferences("ProfilePrefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putString("profile_image_uri", uri).apply()
}

// Funkce pro kopírování obrázku do interního úložiště
fun copyImageToInternalStorage(context: android.content.Context, uri: android.net.Uri): String? {
    return try {
        val inputStream: InputStream? = context.contentResolver.openInputStream(uri)
        val fileName = "profile_image.jpg"
        val file = File(context.filesDir, fileName)
        val outputStream = FileOutputStream(file)
        
        inputStream?.use { input ->
            outputStream.use { output ->
                input.copyTo(output)
            }
        }
        
        file.absolutePath
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

// Helper funkce pro získání aktuální řady
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreakProfile(context: android.content.Context): Int {
    val sharedPreferences = context.getSharedPreferences("StreakData", android.content.Context.MODE_PRIVATE)
    val streakDays = sharedPreferences.getStringSet("streak_days", emptySet()) ?: emptySet()
    
    if (streakDays.isEmpty()) return 0
    
    val sortedDates = streakDays.map { LocalDate.parse(it) }.sorted()
    if (sortedDates.size == 1) return 1
    
    var currentStreak = 0
    val today = LocalDate.now()
    val lastRecordedDay = sortedDates.last()
    
    if (lastRecordedDay == today || lastRecordedDay == today.minusDays(1)) {
        var expectedDate = lastRecordedDay
        for (i in sortedDates.indices.reversed()) {
            if (sortedDates[i] == expectedDate) {
                currentStreak++
                expectedDate = expectedDate.minusDays(1)
            } else {
                break
            }
        }
    }
    
    return currentStreak
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController, viewModel: MainViewModel = viewModel()) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val postsState = viewModel.posts.collectAsState()
    val posts: List<Post> = postsState.value
    val postLoading by viewModel.postLoading.collectAsState()
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }
    val otherCategoryId = 42
    val filteredPosts = posts.filter { it.categoryId == otherCategoryId }

    LaunchedEffect(Unit) {
        viewModel.loadPosts(otherCategoryId)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LargeTopAppBar(
                title = { Text("Profil", style = MaterialTheme.typography.titleLarge) },
                actions = {
                    // Zobrazení bodů s novým designem
                    val totalPoints by PointsManager.totalPoints.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { showTotalOverlay = true }
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stars,
                            contentDescription = "Body",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = totalPoints.toString(),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    // Zobrazení aktivního multiplikátoru
                    MultiplierIndicator()
                    
                    // Streak button s počtem dní
                    val context = LocalContext.current
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = getCurrentStreakProfile(context)
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navController.navigate("streak") }
                    ) {
                        if (currentStreak.value > 0) {
                            Text(
                                text = currentStreak.value.toString(),
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                        Icon(
                            imageVector = Icons.Default.Whatshot,
                            contentDescription = "Streak",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                },
                scrollBehavior = scrollBehavior
            )

            val configuration = LocalConfiguration.current
            val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
            val gridColumns = if (isLandscape) 3 else 1
            val cardModifier = Modifier
                .padding(8.dp)
            val cardShape = RoundedCornerShape(16.dp)

            if (postLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Načítání dalšího obsahu...")
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(gridColumns),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp)
                ) {
                // Profilová sekce
                item(span = { GridItemSpan(gridColumns) }) {
                    ProfileSection(navController = navController)
                }
                
                // Vybavený citát
                item(span = { GridItemSpan(gridColumns) }) {
                    EquippedQuoteSection(navController = navController)
                }
                
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        onClick = { navController.navigate("shop") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = "Obchod",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    "Obchod", 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Utrať své body za streak freeze, citáty, ikony a zvířátka!", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                        onClick = { navController.navigate("backpack") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Backpack,
                                    contentDescription = "Aktovka",
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Text(
                                    "Aktovka", 
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                "Prohlédni si své koupené věci - citáty, ikony a zvířátka!", 
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { navController.navigate("feedback") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Zpětná vazba", style = MaterialTheme.typography.titleMedium)
                            Text("Napište nám, co byste chtěli změnit nebo vylepšit.", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { navController.navigate("about") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("O aplikaci", style = MaterialTheme.typography.titleMedium)
                            Text("Všechno o aplikaci", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { navController.navigate("changelog") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Deník změn", style = MaterialTheme.typography.titleMedium)
                            Text("Všechno důležité, co bylo změněno", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { navController.navigate("favorites") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Oblíbené", style = MaterialTheme.typography.titleMedium)
                            Text("Tvé uložené útržky a články, které nevyuživáš. :(", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                item(span = { GridItemSpan(1) }) {
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { navController.navigate("updater") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text("Aktualizátor", style = MaterialTheme.typography.titleMedium)
                            Text("Aktualizuj si aplikaci, ať ti nic neunikne!", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                items(filteredPosts) { post ->
                    Card(
                        modifier = cardModifier,
                        elevation = CardDefaults.cardElevation(4.dp),
                        shape = cardShape,
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        onClick = { navController.navigate("postDetail/${post.id}") }
                    ) {
                        Column(Modifier.padding(16.dp)) {
                            Text(text = post.title, style = MaterialTheme.typography.titleMedium)
                            val updated = post.updatedAt
                            val formatted = updated?.let { dateString ->
                                try {
                                    val inputFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", java.util.Locale.forLanguageTag("cs-CZ"))
                                    val outputFormatter = java.text.SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", java.util.Locale.forLanguageTag("cs-CZ"))
                                    val date = inputFormatter.parse(dateString)
                                    date?.let { outputFormatter.format(it) } ?: dateString
                                } catch (_: Exception) {
                                    dateString
                                }
                            } ?: ""
                            if (formatted.isNotBlank()) {
                                Text(text = "Upraveno: $formatted", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }
        }
    }

        // Overlay na nejvyšší úrovni
        if (showTotalOverlay) {
            FullScreenTotalPointsOverlay(totalPoints = totalPoints)
            LaunchedEffect(showTotalOverlay) {
                delay(2200)
                showTotalOverlay = false
            }
        }
    }
}

// Bubliny pro jednotlivá zvířátka
val petBubbles = mapOf(
    "🐱" to listOf(
        "Mňau! Máš dnes skvělou náladu! �",
        "Mrr... Vidím, že pilně studuješ! 🐱",
        "Kočičí rada: Občas si zdřímni, pak budeš chytřejší! 😴",
        "Mňau! Nezapomeň si pohladit svého virtuálního mazlíčka! 💕",
        "Psst... V obchodě mají nové věci, mrr! 🛒",
        "Dnes jsi můj nejoblíbenější člověk! Mňau! ❤️",
        "Kočky jsou chytré, ale ty jsi ještě chytřejší! 🧠",
        "Mrrrr... Čas na pauzu s kočičím video! 📱",
        "Mňau! Tvoje streak je jako moje licousy - perfektní! 🔥",
        "Kočičí moudrost: Každý den se něco nauč! 📚"
    ),
    "🐶" to listOf(
        "Haf! Dobrý člověk, jak se máš? 🐕",
        "Woof! Jsem tak hrdý na tvoje pokroky! 🎾",
        "Haf haf! Nezapomeň si dnes udělat procházku! �",
        "Psí rada: Věrnost k cílům vede k úspěchu! 💪",
        "Woof! Máš nejlepší streak v celé smečce! 🔥",
        "Haf! Čas na odměnu - zasloužíš si něco dobrého! 🦴",
        "Psí moudrost: Nikdy se nevzdávej, haf! 🌟",
        "Woof woof! Dnes budeš úžasný, cítím to! ✨",
        "Haf! Společně zvládneme všechno! 🤝",
        "Psí tip: Po učení si zahrát pomáhá mozku! 🎈"
    ),
    "🐰" to listOf(
        "Hop hop! Rychle k novým znalostem! 🥕",
        "Králičí rada: Malé kroky vedou k velkým cílům! �",
        "Hop! Tvoje tempo učení je perfektní! ⚡",
        "Králík říká: Zdravé jídlo = zdravý mozek! 🥬",
        "Hop hop! Nezapomeň si odpočinout mezi lekcemi! 😌",
        "Králičí moudrost: Buď rychlý, ale důkladný! 🏃",
        "Hop! V obchodě jsou nové věci, skoč se podívat! 🛒",
        "Králík ví: Trpělivost přináší ovoce! 🍎",
        "Hop hop! Tvoje snaha mě inspiruje! 💫",
        "Králičí tip: Cvič mozek každý den! 🧠"
    ),
    "🦉" to listOf(
        "Hú hú! Moudrost přichází s praxí! 🦉",
        "Sova praví: Noc je dobrá doba na učení! 🌙",
        "Hú! Tvoje vědomosti rostou každým dnem! 📚",
        "Sova doporučuje: Přečti si něco nového! 📖",
        "Hú hú! Moudrost je největší poklad! 💎",
        "Sova ví: Otázky jsou klíčem k poznání! ❓",
        "Hú! Buď moudrý jako sova, pilný jako včela! 🐝",
        "Sova říká: Každá chyba je lekce! 🎓",
        "Hú hú! Tvoje vzdělání je investice do budoucnosti! �",
        "Sova radí: Nikdy nepřestaň být zvědavý! 🔍"
    ),
    "🦊" to listOf(
        "Chytrá liška ti radí: Mysli kreativně! 🦊",
        "Liška ví: Lstivost a znalosti = úspěch! 🧠",
        "Mazaná rada: Různé přístupy = různé výsledky! 🔄",
        "Liška praví: Buď flexibilní ve svém učení! 🤸",
        "Chytře zvolenou cestou dojdeš nejdál! 🛤️",
        "Liška doporučuje: Hledej skryté souvislosti! 🔗",
        "Mazaná poznámka: Někdy méně je více! ✨",
        "Liška říká: Adaptabilita je klíčová! 🗝️",
        "Chytrý tip: Využij své silné stránky! �",
        "Liška ví: Intuice + fakta = dokonalost! 🎯"
    ),
    "🐼" to listOf(
        "Panda říká: Klid a vyrovnanost vedou k úspěchu! 🧘",
        "Zen rada: Dýchej hluboce a soustřeď se! 🌸",
        "Panda ví: Pomalu ale jistě! 🐌",
        "Klidná mysl = jasné myšlení! 💭",
        "Panda doporučuje: Meditace před učením! 🕯️",
        "Zen moudrost: Přítomný okamžik je vše! ⏰",
        "Panda praví: Harmonie v učení i odpočinku! ⚖️",
        "Klidný tip: Stres je nepřítel učení! �",
        "Panda říká: Najdi si svůj rytmus! 🎵",
        "Zen rada: Buď trpělivý sám se sebou! 🌱"
    ),
    "🐬" to listOf(
        "Delfín plave: Inteligence je jako voda - tekutá! 🌊",
        "Chytrá rada: Spolupráce přináší výsledky! 🤝",
        "Delfín ví: Radost z učení je důležitá! 😊",
        "Vodní moudrost: Plynule mezi tématy! 🏊",
        "Delfín říká: Komunikace je klíčová! 💬",
        "Inteligentní tip: Sdílej své znalosti! 📤",
        "Delfín praví: Hravost podporuje kreativitu! 🎨",
        "Vodní rada: Nech myšlenky volně plynout! 💫",
        "Delfín doporučuje: Učte se ve skupině! 👥",
        "Chytrý delfín ví: Každý má co nabídnout! 🎁"
    )
)

// Fallback bubliny pro neznámá zvířátka
val defaultPetBubbles = listOf(
    "Ahoj! Jak ti jde učení? 😊",
    "Nezapomeň na přestávku! ☕",
    "Dnes budeš úžasný! ✨",
    "Tvoje snaha se vyplácí! 💪",
    "Zůstaň motivovaný! 🌟",
    "Každý den se zlepšuješ! 📈",
    "Věř si, zvládneš to! 💫",
    "Učení je dobrodružství! 🗺️",
    "Jsi na správné cestě! 🛤️",
    "Pokračuj, jdeš skvěle! �"
)

// Helper funkce pro správu bubliny
fun shouldShowBubble(context: android.content.Context): Boolean {
    val prefs = context.getSharedPreferences("PetBubblePrefs", android.content.Context.MODE_PRIVATE)
    val lastDismissed = prefs.getLong("last_dismissed", 0)
    val currentTime = System.currentTimeMillis()
    val oneHourInMillis = 60 * 60 * 1000L
    return currentTime - lastDismissed > oneHourInMillis
}

fun dismissBubbleForHour(context: android.content.Context) {
    val prefs = context.getSharedPreferences("PetBubblePrefs", android.content.Context.MODE_PRIVATE)
    prefs.edit().putLong("last_dismissed", System.currentTimeMillis()).apply()
}

// Funkce pro získání náhodné bubliny podle zvířátka
fun getRandomBubbleForPet(petIcon: String?): String {
    return petBubbles[petIcon]?.random() ?: defaultPetBubbles.random()
}

@Composable
fun ProfileSection(navController: NavController) {
    val context = LocalContext.current
    var profileName by remember { mutableStateOf("") }
    var isEditingName by remember { mutableStateOf(false) }
    var tempName by remember { mutableStateOf(profileName) }
    var profileImageUri by remember { mutableStateOf<String?>(null) }
    var showFullscreenImage by remember { mutableStateOf(false) }
    var showPetBubble by remember { mutableStateOf(false) }
    var currentBubbleText by remember { mutableStateOf("") }
    var isAnimatingOut by remember { mutableStateOf(false) }
    
    // Načtení dat při startu
    LaunchedEffect(Unit) {
        profileName = getProfileName(context)
        tempName = profileName
        profileImageUri = getProfileImageUri(context)
        
        // Zkontroluj, jestli má zobrazit bublinu
        if (shouldShowBubble(context)) {
            val equippedPet = BackpackManager.equippedPet.value
            currentBubbleText = getRandomBubbleForPet(equippedPet?.petIcon)
            showPetBubble = true
        }
    }
    
    // Launcher pro výběr obrázku z galerie
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { originalUri ->
            // Zkopírujeme obrázek do interního úložiště
            val copiedImagePath = copyImageToInternalStorage(context, originalUri)
            copiedImagePath?.let { path ->
                profileImageUri = path
                saveProfileImageUri(context, path)
            }
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(6.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profilový obrázek
            Box(
                modifier = Modifier.size(120.dp),
                contentAlignment = Alignment.Center
            ) {
                // Kruh s obrázkem nebo ikonou
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                        .clickable {
                            showFullscreenImage = true
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (profileImageUri != null) {
                        AsyncImage(
                            model = File(profileImageUri!!),
                            contentDescription = "Profilový obrázek",
                            modifier = Modifier
                                .size(120.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Profilový obrázek",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                // Ikona kamery pro editaci
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary)
                        .clickable {
                            imagePickerLauncher.launch("image/*")
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PhotoCamera,
                        contentDescription = "Změnit obrázek",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                // Vybavené zvířátko vlevo dolů
                val equippedPet by BackpackManager.equippedPet.collectAsState()
                equippedPet?.petIcon?.let { petIcon ->
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.9f))
                            .clickable { 
                                currentBubbleText = getRandomBubbleForPet(petIcon)
                                showPetBubble = true
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = petIcon,
                            fontSize = 20.sp,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Jméno profilu
            if (isEditingName) {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    label = { Text("Jméno") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        Row {
                            IconButton(
                                onClick = {
                                    profileName = tempName
                                    saveProfileName(context, tempName)
                                    isEditingName = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = "Uložit"
                                )
                            }
                            IconButton(
                                onClick = {
                                    tempName = profileName
                                    isEditingName = false
                                }
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Zrušit"
                                )
                            }
                        }
                    }
                )
            } else {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { 
                        isEditingName = true
                        tempName = profileName
                    }
                ) {
                    Text(
                        text = profileName,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Upravit jméno",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
    
    // Fullscreen dialog pro zobrazení profilového obrázku
    if (showFullscreenImage) {
        Dialog(
            onDismissRequest = { showFullscreenImage = false },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = true,
                usePlatformDefaultWidth = false
            )
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { showFullscreenImage = false },
                contentAlignment = Alignment.Center
            ) {
                if (profileImageUri != null) {
                    AsyncImage(
                        model = File(profileImageUri!!),
                        contentDescription = "Profilový obrázek",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(32.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "Profilový obrázek",
                        modifier = Modifier.size(200.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                
                // Tlačítko pro zavření
                IconButton(
                    onClick = { showFullscreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(
                            Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Zavřít",
                        tint = Color.White
                    )
                }
            }
        }
    }
    
    // Bublina nad profilem s animacemi
    AnimatedVisibility(
        visible = showPetBubble,
        enter = slideInVertically(
            initialOffsetY = { -it },
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ) + fadeIn(
            animationSpec = tween(300)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            )
        ),
        exit = slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        ) + fadeOut(
            animationSpec = tween(300)
        ) + scaleOut(
            targetScale = 0.6f,
            animationSpec = tween(300)
        )
    ) {
        // Animovaný scale efekt při kliknutí
        val scale by animateFloatAsState(
            targetValue = if (isAnimatingOut) 0.9f else 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium
            ),
            label = "bubble_click_scale"
        )
        // Hlavní bublina
        Card(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 60.dp, top = 20.dp, end = 20.dp)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                }
                .clickable { 
                    isAnimatingOut = true
                },
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            ),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            Text(
                text = currentBubbleText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
                modifier = Modifier.padding(16.dp),
                textAlign = TextAlign.Start
            )
        }
        
        // Špička bubliny (trojúhelník) směřující k zvířátku
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 40.dp, y = (-30).dp)
                .size(16.dp)
                .background(
                    MaterialTheme.colorScheme.secondaryContainer,
                    shape = RoundedCornerShape(bottomStart = 16.dp)
                )
        )
    }
    
    // Automatické zavření po 7 sekundách
    LaunchedEffect(showPetBubble) {
        if (showPetBubble) {
            delay(7000)
            isAnimatingOut = true
        }
    }
    
    // Animace při kliknutí na bublinu
    LaunchedEffect(isAnimatingOut) {
        if (isAnimatingOut) {
            delay(300) // Čeká na dokončení animace
            dismissBubbleForHour(context)
            showPetBubble = false
            isAnimatingOut = false
        }
    }
}
}

@Composable
fun EquippedQuoteSection(navController: NavController) {
    val equippedQuote by BackpackManager.equippedQuote.collectAsState()
    
    equippedQuote?.quote?.let { quote ->
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp)
                .clickable { navController.navigate("backpack") },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.FormatQuote,
                    contentDescription = "Citát",
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "\"$quote\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}