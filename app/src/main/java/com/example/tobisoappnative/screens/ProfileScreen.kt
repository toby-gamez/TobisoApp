package com.example.tobisoappnative.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.outlined.ShoppingBag
import com.example.tobisoappnative.viewmodel.profile.ProfileViewModel
import com.example.tobisoappnative.model.Post
import androidx.compose.material.icons.outlined.Backpack
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
import java.util.TimeZone
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavHostController
import com.example.tobisoappnative.components.ImageCropperDialog
import com.example.tobisoappnative.components.FloatingSearchBar

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

// Helper funkce pro získání aktuální řady (nyní s freeze podporou)
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreakProfile(context: android.content.Context): Int {
    return com.example.tobisoappnative.utils.StreakUtils.getCurrentStreak(context)
}

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val application = LocalContext.current.applicationContext as Application
    val vm: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(application))
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val postsState = vm.posts.collectAsState()
    val posts: List<Post> = postsState.value
    val postLoading by vm.postLoading.collectAsState()
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }
    val otherCategoryId = 42
    val filteredPosts = posts.filter { it.categoryId == otherCategoryId }

    LaunchedEffect(Unit) {
        vm.loadPosts(otherCategoryId)
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
                    // Ikona aktovky
                    IconButton(
                        onClick = { navController.navigate("backpack") }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Backpack,
                            contentDescription = "Aktovka",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Ikona obchodu
                    IconButton(
                        onClick = { navController.navigate("shop") }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ShoppingBag,
                            contentDescription = "Obchod",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    
                    // Zobrazení aktivního multiplikátoru
                    MultiplierIndicator()
                    
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
                    
                    // Streak button s počtem dní (s freeze podporou)
                    val context = LocalContext.current
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    // Sledování změn v freeze
                    val availableFreezes by com.example.tobisoappnative.StreakFreezeManager.availableFreezes.collectAsState()
                    val usedFreezes by com.example.tobisoappnative.StreakFreezeManager.usedFreezes.collectAsState()
                    
                    LaunchedEffect(availableFreezes, usedFreezes) {
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
                    // Offline download progress indicator (small circle)
                    val offlineDownloading by vm.offlineDownloading.collectAsState()
                    val offlineProgress by vm.offlineDownloadProgress.collectAsState()
                    if (offlineDownloading) {
                        Box(modifier = Modifier.padding(end = 8.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(
                                progress = offlineProgress.coerceIn(0f, 1f),
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
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
                        Text("Načítání profilu...")
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
                
                // Úspěchy
                item(span = { GridItemSpan(gridColumns) }) {
                    AchievementsSection()
                }

                    item(span = { GridItemSpan(1) }) {
                        Card(
                            modifier = cardModifier,
                            elevation = CardDefaults.cardElevation(4.dp),
                            shape = cardShape,
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                            onClick = { navController.navigate("offlineManager") }
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Správce offline dat", style = MaterialTheme.typography.titleMedium)
                                Text("Správa a stažení offline dat", style = MaterialTheme.typography.bodySmall)
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
                            val locale = java.util.Locale.forLanguageTag("cs-CZ")
                            val inputFormatter = java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSSSSS", locale).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            val outputFormatter = java.text.SimpleDateFormat("dd. MM. yyyy 'v' HH:mm", locale).apply {
                                timeZone = TimeZone.getDefault()
                            }

                            val candidates = listOfNotNull(post.lastEdit, post.lastFix, post.createdAt)
                            val latestDate = candidates.mapNotNull { ds ->
                                try {
                                    inputFormatter.parse(ds)
                                } catch (_: Exception) {
                                    null
                                }
                            }.maxOrNull()

                            val formatted = latestDate?.let { outputFormatter.format(it) } ?: candidates.firstOrNull() ?: ""
                            if (formatted.isNotBlank()) {
                                Text(text = if (post.title == "O mně") "Něco drobného o autoru aplikace" else "Proč vlastně toto existuje?", style = MaterialTheme.typography.bodySmall)
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
    ),
    "🐭" to listOf(
        "Myška šeptá: Malé kroky vedou k velkým cílům! 🏃‍♀️",
        "Rychlá rada: Procvič si paměť každý den! 🧠",
        "Myši vědí: Pozornost k detailům je klíčová! 🔍",
        "Myška praví: Buď rychlý, ale přesný! ⚡",
        "Malá myška, velká moudrost: Vytrvej! 💪",
        "Myška doporučuje: Najdi si tiché místo na učení! 🤫",
        "Rychlý tip: Opakování je matka moudrosti! 🔄",
        "Myška ví: I malé úspěchy se počítají! ⭐",
        "Chytrá myška říká: Buď zvědavý jako já! 🤔",
        "Myška radí: Někdy je nejlepší začít znovu! 🔄"
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
    var showImageCropper by remember { mutableStateOf(false) }
    var tempImageForCropping by remember { mutableStateOf<String?>(null) }
    
    // Načtení dat při startu
    LaunchedEffect(Unit) {
        profileName = getProfileName(context)
        tempName = profileName
        profileImageUri = getProfileImageUri(context)
        
        // Zkontroluj, jestli má zobrazit bublinu - pouze pokud má vybavené zvířátko
        val equippedPet = BackpackManager.equippedPet.value
        if (equippedPet != null && shouldShowBubble(context)) {
            currentBubbleText = getRandomBubbleForPet(equippedPet.petIcon)
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
                // Místo přímého nastavení, otevřeme cropper
                tempImageForCropping = path
                showImageCropper = true
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
                                // Kliknutí zobrazí bublinu pouze pokud je zvířátko vybavené
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
    
    // Image Cropper Dialog
    if (showImageCropper && tempImageForCropping != null) {
        ImageCropperDialog(
            imageUri = tempImageForCropping!!,
            onCropComplete = { croppedImagePath ->
                // Uložit oříznutý obrázek
                profileImageUri = croppedImagePath
                saveProfileImageUri(context, croppedImagePath)
                showImageCropper = false
                tempImageForCropping = null
            },
            onDismiss = {
                showImageCropper = false
                tempImageForCropping = null
            }
        )
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
                .padding(horizontal = 8.dp, vertical = 4.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(4.dp),

        ) {
            Column(
                modifier = Modifier
                    .padding(16.dp)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

// Funkce pro generování achievementů bodů
fun generatePointsAchievements(): Map<Int, Int> {
    return mapOf(
        10 to 5,      // 10 bodů -> 5 bodů odměna
        20 to 5,
        50 to 10,
        75 to 10,
        100 to 15,
        150 to 15,
        200 to 15,
        300 to 20,
        400 to 20,
        500 to 25,
        600 to 25,
        700 to 25,
        800 to 30,
        900 to 30,
        1000 to 50,   // speciální odměna za 1000
        1500 to 75,
        2000 to 100,
        3000 to 150,
        4000 to 200,
        5000 to 250
        // můžeme přidat další podle potřeby
    )
}

// Funkce pro kontrolu achievementů
fun checkPointsAchievements(context: android.content.Context) {
    val totalEarnedPoints = PointsManager.getTotalEarnedPoints()
    val achievements = generatePointsAchievements()
    
    // SharedPreferences pro sledování dosažených achievementů
    val achievementsPrefs = context.getSharedPreferences("points_achievements", android.content.Context.MODE_PRIVATE)
    
    var newAchievementsFound = false
    achievements.forEach { (requiredPoints, rewardPoints) ->
        if (totalEarnedPoints >= requiredPoints) {
            val achievementKey = "achievement_$requiredPoints"
            val isAlreadyAchieved = achievementsPrefs.getBoolean(achievementKey, false)
            
            if (!isAlreadyAchieved) {
                newAchievementsFound = true
                println("🏆 NEW ACHIEVEMENT UNLOCKED: $requiredPoints points - awarding $rewardPoints points")
                
                // Achievement dosažen poprvé - přidat body s informací o achievementu
                PointsManager.addPointsForAchievement(context, rewardPoints, requiredPoints)
                
                // Označit achievement jako dosažený
                achievementsPrefs.edit().putBoolean(achievementKey, true).apply()
            }
        }
    }
}

// Funkce pro generování streak odznaků podle dní
fun getStreakBadge(days: Int): AchievementBadge {
    return when (days) {
        7 -> AchievementBadge("🗓️", "Týdenní řada", "7 dní v řadě", Color(0xFF4CAF50))
        14 -> AchievementBadge("📅", "Čtrnáctidenní řada", "14 dní v řadě", Color(0xFF2196F3))
        25 -> AchievementBadge("🔥", "Čtvrt sta řada", "25 dní v řadě", Color(0xFFFF5722))
        30 -> AchievementBadge("🌙", "Měsíční řada", "30 dní v řadě", Color(0xFF9C27B0))
        50 -> AchievementBadge("💫", "Padesátidenní řada", "50 dní v řadě", Color(0xFFFFEB3B))
        60 -> AchievementBadge("⏰", "Dvouměsíční řada", "60 dní v řadě", Color(0xFF00BCD4))
        75 -> AchievementBadge("⚡", "Bleskurychlá řada", "75 dní v řadě", Color(0xFF673AB7))
        100 -> AchievementBadge("💯", "Stoletní řada", "100 dní v řadě", Color(0xFFF44336))
        125 -> AchievementBadge("🎯", "Vytrvalá řada", "125 dní v řadě", Color(0xFF795548))
        150 -> AchievementBadge("🏅", "Šampionská řada", "150 dní v řadě", Color(0xFFFFEB3B))
        175 -> AchievementBadge("🌟", "Hvězdná řada", "175 dní v řadě", Color(0xFF3F51B5))
        183 -> AchievementBadge("📆", "Půlroční řada", "183 dní v řadě", Color(0xFF607D8B))
        200 -> AchievementBadge("🚀", "Raketová řada", "200 dní v řadě", Color(0xFF9C27B0))
        250 -> AchievementBadge("👑", "Královská řada", "250 dní v řadě", Color(0xFFE91E63))
        300 -> AchievementBadge("💎", "Diamantová řada", "300 dní v řadě", Color(0xFF424242))
        365 -> AchievementBadge("🎂", "Roční řada", "365 dní v řadě", Color(0xFFFF9800))
        400 -> AchievementBadge("🦅", "Orlí řada", "400 dní v řadě", Color(0xFF3F51B5))
        500 -> AchievementBadge("🏆", "Legendární řada", "500 dní v řadě", Color(0xFF9C27B0))
        548 -> AchievementBadge("🌍", "Půldruharoční řada", "548 dní v řadě", Color(0xFFFF5722))
        600 -> AchievementBadge("🔮", "Mystická řada", "600 dní v řadě", Color(0xFF4CAF50))
        730 -> AchievementBadge("🌈", "Dvouletá řada", "730 dní v řadě", Color(0xFF2196F3))
        800 -> AchievementBadge("🏔️", "Vrcholová řada", "800 dní v řadě", Color(0xFF9C27B0))
        913 -> AchievementBadge("🎆", "Dva a půl roky řada", "913 dní v řadě", Color(0xFFFFEB3B))
        1000 -> AchievementBadge("♾️", "Tisícidenní řada", "1000 dní v řadě", Color(0xFFFF5722))
        1095 -> AchievementBadge("🌌", "Tříletá řada", "1095 dní v řadě", Color(0xFF00BCD4))
        1460 -> AchievementBadge("🎊", "Čtyřletá řada", "1460 dní v řadě", Color(0xFF673AB7))
        1826 -> AchievementBadge("🏰", "Pětiletá řada", "1826 dní v řadě", Color(0xFFF44336))
        else -> {
            // Každých 25 dní generický odznak
            if (days % 25 == 0) {
                AchievementBadge("🏅", "${days}denní řada", "$days dní v řadě", Color(0xFF795548))
            } else {
                AchievementBadge("📊", "Streak řada", "$days dní v řadě", Color(0xFF607D8B))
            }
        }
    }
}

// Funkce pro získání všech dosažených streak milníků (nyní s freeze podporou)
@RequiresApi(Build.VERSION_CODES.O)
fun getCompletedStreakMilestones(context: android.content.Context): List<Int> {
    val milestonesPrefs = context.getSharedPreferences("streak_milestones", android.content.Context.MODE_PRIVATE)
    
    // Používáme StreakUtils pro správný výpočet včetně freezes
    val maxStreak = com.example.tobisoappnative.utils.StreakUtils.getMaxStreak(context)
    
    if (maxStreak == 0) return emptyList()
    
    // Speciální milníky podle MainActivity
    val specialMilestones = listOf(7, 14, 30, 60, 100, 183, 365, 548, 730, 913, 1095, 1460, 1826)
    val regularMilestones = (25..maxStreak + 100 step 25).filter { it !in specialMilestones }
    val allMilestones = (specialMilestones + regularMilestones).filter { it <= maxStreak }
    
    // Vrátit jen ty, které jsou označené jako dosažené
    return allMilestones.filter { days ->
        milestonesPrefs.getBoolean("milestone_$days", false)
    }.sorted()
}

// Funkce pro generování odznaků podle bodů
fun getAchievementBadge(points: Int): AchievementBadge {
    return when (points) {
        10 -> AchievementBadge("🌱", "Začátečník", "První kroky", Color(0xFF4CAF50))
        20 -> AchievementBadge("📚", "Student", "Učí se", Color(0xFF2196F3))
        50 -> AchievementBadge("🎯", "Cílevědomý", "Má jasný cíl", Color(0xFF9C27B0))
        75 -> AchievementBadge("⭐", "Hvězda", "Září mezi ostatními", Color(0xFFFFEB3B))
        100 -> AchievementBadge("🏆", "Šampion", "První století", Color(0xFFFF9800))
        150 -> AchievementBadge("🔥", "V ohni", "Neustále aktivní", Color(0xFFFF5722))
        200 -> AchievementBadge("💎", "Diamant", "Vzácný talent", Color(0xFF00BCD4))
        300 -> AchievementBadge("🚀", "Raketa", "Rychlý pokrok", Color(0xFF673AB7))
        400 -> AchievementBadge("👑", "Král", "Vládne oboru", Color(0xFFF44336))
        500 -> AchievementBadge("🦅", "Orel", "Letí vysoko", Color(0xFF795548))
        600 -> AchievementBadge("⚡", "Blesk", "Rychlý jako světlo", Color(0xFFFFEB3B))
        700 -> AchievementBadge("🌟", "Supernova", "Zářivý úspěch", Color(0xFF3F51B5))
        800 -> AchievementBadge("🏔️", "Vrchol", "Na vrcholu hory", Color(0xFF607D8B))
        900 -> AchievementBadge("🔮", "Mystik", "Tajemná síla", Color(0xFF9C27B0))
        1000 -> AchievementBadge("🌈", "Legenda", "Tisíc bodů!", Color(0xFFE91E63))
        1500 -> AchievementBadge("🌙", "Lunární", "Dosáhl měsíce", Color(0xFF424242))
        2000 -> AchievementBadge("☀️", "Solární", "Síla slunce", Color(0xFFFF9800))
        3000 -> AchievementBadge("🌌", "Galaktický", "Mezi hvězdami", Color(0xFF3F51B5))
        4000 -> AchievementBadge("🎆", "Kosmický", "Za hranicí", Color(0xFF9C27B0))
        5000 -> AchievementBadge("♾️", "Nekonečný", "Bez hranic", Color(0xFFFF5722))
        else -> AchievementBadge("🏅", "Medaile", "Úspěch", Color(0xFF4CAF50))
    }
}

// Data třída pro odznaky
data class AchievementBadge(
    val emoji: String,
    val title: String,
    val description: String,
    val color: Color
)

// Data třída pro badge s dodatečnými informacemi
data class BadgeData(
    val id: String,
    val badge: AchievementBadge,
    val type: String // "points" nebo "streak"
)

// Komponenta pro zobrazení achievementů
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun AchievementsSection() {
    val context = LocalContext.current
    val totalEarnedPoints by PointsManager.totalEarnedPoints.collectAsState()
    val achievements = remember { generatePointsAchievements().toSortedMap() }
    
    // Získání dosažených achievementů bodů
    val achievementsPrefs = context.getSharedPreferences("points_achievements", android.content.Context.MODE_PRIVATE)
    val completedPointsAchievements = achievements.keys.filter { points ->
        totalEarnedPoints >= points && achievementsPrefs.getBoolean("achievement_$points", false)
    }
    
    // Získání dosažených streak milníků
    val completedStreakMilestones = getCompletedStreakMilestones(context)
    
    // Spojit všechny dosažené odznaky
    val allCompletedBadges = completedPointsAchievements.map { 
        BadgeData(it.toString(), getAchievementBadge(it), "points") 
    } + completedStreakMilestones.map { 
        BadgeData(it.toString(), getStreakBadge(it), "streak") 
    }
    
    // Najdi nejbližší nedosažený achievement bodů
    val nextAchievement = achievements.entries.firstOrNull { it.key > totalEarnedPoints }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            // Hlavička s ikonou a názvem
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Stars,
                    contentDescription = "Úspěchy",
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Odznaky",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "${allCompletedBadges.size}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Galerie dosažených odznaků
            if (allCompletedBadges.isNotEmpty()) {
                Text(
                    text = "Získané odznaky:",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                // Grid odznaků (3 v řádku) - seřazené podle typu a hodnoty
                val sortedBadges = allCompletedBadges.sortedWith(
                    compareBy<BadgeData> { if (it.type == "streak") 0 else 1 }
                        .thenBy { it.id.toIntOrNull() ?: 0 }
                )
                val chunkedBadges = sortedBadges.chunked(3)
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chunkedBadges.forEach { rowBadges ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            rowBadges.forEach { badgeData ->
                                BadgeCard(
                                    badge = badgeData.badge,
                                    value = badgeData.id.toIntOrNull() ?: 0,
                                    type = badgeData.type,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // Vyplnění zbývajících míst v řádku
                            repeat(3 - rowBadges.size) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
            }
            
            // Progress k nejbližšímu achievementu
            if (nextAchievement != null) {
                val previousAchievement = achievements.entries.lastOrNull { it.key <= totalEarnedPoints }
                val startValue = previousAchievement?.key ?: 0
                val targetValue = nextAchievement.key
                val currentProgress = (totalEarnedPoints - startValue).coerceAtLeast(0)
                val maxProgress = targetValue - startValue
                val progress = if (maxProgress > 0) currentProgress.toFloat() / maxProgress.toFloat() else 0f
                
                val nextBadge = getAchievementBadge(nextAchievement.key)
                
                // Náhled dalšího odznaku
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = nextBadge.emoji,
                        fontSize = 20.sp,
                        modifier = Modifier
                            .background(
                                nextBadge.color.copy(alpha = 0.2f),
                                CircleShape
                            )
                            .padding(8.dp)
                    )
                    
                    Spacer(modifier = Modifier.width(12.dp))
                    
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Další: ${nextBadge.title}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "${nextAchievement.key} bodů (+${nextAchievement.value} odměna)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = nextBadge.color,
                    trackColor = nextBadge.color.copy(alpha = 0.3f)
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "$totalEarnedPoints / ${nextAchievement.key}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                )
            } else {
                // Všechny achievementy dokončeny
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "🎉",
                        fontSize = 24.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Všechny odznaky získány!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// Komponenta pro jednotlivý odznak
@Composable
fun BadgeCard(
    badge: AchievementBadge,
    value: Int,
    type: String, // "points" nebo "streak"
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f),
        colors = CardDefaults.cardColors(
            containerColor = badge.color.copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(
            width = 1.dp,
            color = badge.color.copy(alpha = 0.4f)
        )
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        ) {
            Text(
                text = badge.emoji,
                fontSize = 24.sp
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = badge.title,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = badge.color,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            
            Text(
                text = if (type == "streak") "${value}d" else "${value}b",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}