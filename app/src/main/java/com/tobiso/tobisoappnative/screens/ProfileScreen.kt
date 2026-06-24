package com.tobiso.tobisoappnative.screens

import com.tobiso.tobisoappnative.navigation.AiChatHistoryRoute
import com.tobiso.tobisoappnative.navigation.BackpackRoute
import com.tobiso.tobisoappnative.navigation.ShopRoute
import com.tobiso.tobisoappnative.navigation.StreakRoute
import com.tobiso.tobisoappnative.navigation.OfflineManagerRoute
import com.tobiso.tobisoappnative.navigation.FavoritesRoute
import com.tobiso.tobisoappnative.navigation.UpdaterRoute
import com.tobiso.tobisoappnative.navigation.FeedbackRoute
import com.tobiso.tobisoappnative.navigation.AboutRoute
import com.tobiso.tobisoappnative.navigation.ChangelogRoute
import com.tobiso.tobisoappnative.navigation.PostDetailRoute
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
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import com.tobiso.tobisoappnative.utils.generatePointsAchievements
import com.tobiso.tobisoappnative.viewmodel.profile.ProfileViewModel
import com.tobiso.tobisoappnative.model.Post
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
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
import java.util.TimeZone
import java.io.File
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.text.style.TextAlign
import androidx.navigation.NavController
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.delay
import com.tobiso.tobisoappnative.PointsManager
import com.tobiso.tobisoappnative.BackpackManager
import com.tobiso.tobisoappnative.PetManager
import com.tobiso.tobisoappnative.manager.PetHealth
import com.tobiso.tobisoappnative.manager.GrowthStage
import com.tobiso.tobisoappnative.components.MultiplierIndicator
import com.tobiso.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.tobiso.tobisoappnative.components.PrestigeAvatarBorder
import com.tobiso.tobisoappnative.components.PrestigeHeroOverlay
import com.tobiso.tobisoappnative.components.getPrestigeTier
import com.tobiso.tobisoappnative.components.formatPointsBalance
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import com.tobiso.tobisoappnative.components.ImageCropperDialog
import com.tobiso.tobisoappnative.model.ProfileThemeData
import androidx.compose.ui.graphics.Brush

import com.tobiso.tobisoappnative.utils.getProfileName
import com.tobiso.tobisoappnative.utils.saveProfileName
import com.tobiso.tobisoappnative.utils.getProfileImageUri
import com.tobiso.tobisoappnative.utils.saveProfileImageUri
import com.tobiso.tobisoappnative.utils.loadGradeId
import com.tobiso.tobisoappnative.utils.saveGradeId
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import com.tobiso.tobisoappnative.BuildConfig

// Image copying moved to ProfileViewModel to avoid IO on UI thread.

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(navController: NavController) {
    val vm: ProfileViewModel = hiltViewModel()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val postsState = vm.posts.collectAsState()
    val posts: List<Post> = postsState.value
    val postLoading by vm.postLoading.collectAsState()
    val otherCategoryId = 42
    val filteredPosts = posts.filter { it.categoryId == otherCategoryId }

    val grades by vm.grades.collectAsState()
    LaunchedEffect(Unit) {
        vm.loadPosts(otherCategoryId)
        vm.loadGrades()
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
                    // Zobrazení aktivního multiplikátoru
                    MultiplierIndicator()
                    
                    // Zobrazení bodů s novým designem
                    val totalPointsFloat by PointsManager.instance.totalPointsFloat.collectAsState()
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(20.dp)
                            )
                            .clickable { navController.navigate(ShopRoute) }
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
                            text = formatPointsBalance(totalPointsFloat),
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    
                    // Streak button s počtem dní (s freeze podporou)
                    val context = LocalContext.current
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    // Sledování změn v freeze
                    val availableFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.availableFreezes.collectAsState()
                    val usedFreezes by com.tobiso.tobisoappnative.StreakFreezeManager.instance.usedFreezes.collectAsState()
                    
                    LaunchedEffect(availableFreezes, usedFreezes) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = com.tobiso.tobisoappnative.utils.StreakUtils.getCurrentStreak(context)
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { navController.navigate(StreakRoute) }
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
                                progress = { offlineProgress.coerceIn(0f, 1f) },
                                strokeWidth = 2.dp,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                },
                scrollBehavior = scrollBehavior
            )

            if (postLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Načítání profilu...")
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item { ProfileSection(navController = navController, grades = grades) }
                    item { PetCareSection() }
                    item { EquippedQuoteSection(navController = navController) }
                    item { AchievementsSection() }

                    item { ProfileSectionHeader("Aktivita") }
                    item {
                        ProfileNavGroup {
                            ProfileNavRow(
                                icon = Icons.Default.Whatshot,
                                title = "Streak",
                                subtitle = "Sleduj svou sérii a zmrazování",
                                onClick = { navController.navigate(StreakRoute) }
                            )
                        }
                    }

                    item { ProfileSectionHeader("Knihovna") }
                    item {
                        ProfileNavGroup {
                            ProfileNavRow(
                                icon = Icons.Default.Favorite,
                                title = "Oblíbené",
                                subtitle = "Tvé uložené útržky a články",
                                onClick = { navController.navigate(FavoritesRoute) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                            ProfileNavRow(
                                icon = Icons.Default.Forum,
                                title = "Nedávné AI chaty",
                                subtitle = "Prohlédni nebo pokračuj v konverzacích",
                                onClick = { navController.navigate(AiChatHistoryRoute) }
                            )
                        }
                    }

                    item { ProfileSectionHeader("Nastavení") }
                    item {
                        ProfileNavGroup {
                            ProfileNavRow(
                                icon = Icons.Default.CloudDownload,
                                title = "Správce offline dat",
                                subtitle = "Správa a stažení offline dat",
                                onClick = { navController.navigate(OfflineManagerRoute) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                            ProfileNavRow(
                                icon = Icons.Default.Update,
                                title = "Aktualizátor",
                                subtitle = "Aktualizuj si aplikaci",
                                onClick = { navController.navigate(UpdaterRoute) }
                            )
                        }
                    }

                    item { ProfileSectionHeader("Informace") }
                    item {
                        ProfileNavGroup {
                            ProfileNavRow(
                                icon = Icons.Default.RateReview,
                                title = "Zpětná vazba",
                                subtitle = "Napište nám, co byste chtěli změnit",
                                onClick = { navController.navigate(FeedbackRoute) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                            ProfileNavRow(
                                icon = Icons.Default.Info,
                                title = "O aplikaci",
                                subtitle = "Vše o aplikaci Tobiso",
                                onClick = { navController.navigate(AboutRoute) }
                            )
                            HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                            ProfileNavRow(
                                icon = Icons.Default.History,
                                title = "Deník změn",
                                subtitle = "Novinky a opravy",
                                onClick = { navController.navigate(ChangelogRoute) }
                            )
                        }
                    }

                    if (BuildConfig.DEBUG) {
                        item { DebugAddPointsSection() }
                    }

                    if (filteredPosts.isNotEmpty()) {
                        item { ProfileSectionHeader("Ostatní") }
                        item {
                            ProfileNavGroup {
                                filteredPosts.forEachIndexed { index, post ->
                                    if (index > 0) HorizontalDivider(modifier = Modifier.padding(start = 52.dp))
                                    ProfileNavRow(
                                        icon = Icons.Default.Description,
                                        title = post.title,
                                        subtitle = when (post.title) {
                                            "O mně" -> "Něco drobného o autoru aplikace"
                                            "Zásady ochrany osobních údajů" -> "Jak nakládáme s tvými daty a soukromím?"
                                            "Podmínky použití" -> "Pravidla a podmínky pro používání aplikace"
                                            else -> "Proč tento projekt vlastně existuje?"
                                        },
                                        onClick = { navController.navigate(PostDetailRoute(postId = post.id)) }
                                    )
                                }
                            }
                        }
                    }
                }
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
fun ProfileSection(navController: NavController, grades: List<com.tobiso.tobisoappnative.model.Grade> = emptyList()) {
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
    val vm: ProfileViewModel = hiltViewModel()
    val copiedImagePath by vm.copiedImagePath.collectAsState()
    val totalPointsFloat by PointsManager.instance.totalPointsFloat.collectAsState()

    LaunchedEffect(copiedImagePath) {
        copiedImagePath?.let { path ->
            tempImageForCropping = path
            showImageCropper = true
            vm.clearCopiedImagePath()
        }
    }

    LaunchedEffect(Unit) {
        profileName = getProfileName(context)
        tempName = profileName
        profileImageUri = getProfileImageUri(context)
        val equippedPet = BackpackManager.instance.equippedPet.value
        if (equippedPet != null && shouldShowBubble(context)) {
            currentBubbleText = getRandomBubbleForPet(equippedPet.petIcon)
            showPetBubble = true
        }
    }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { vm.copyImageToInternalStorage(it) }
    }

    val totalEarnedPoints by PointsManager.instance.totalEarnedPoints.collectAsState()
    val prestigeTier = remember(totalEarnedPoints) { getPrestigeTier(totalEarnedPoints) }

    val equippedTheme by BackpackManager.instance.equippedTheme.collectAsState()
    val equippedPet by BackpackManager.instance.equippedPet.collectAsState()

    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val heroGradientColors = equippedTheme?.let { ProfileThemeData.getGradientColors(it.id) }
        ?: listOf(primaryColor, primaryContainerColor)
    val heroBrush = Brush.horizontalGradient(heroGradientColors)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(2.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                // Gradient hero header (top 110 dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .background(heroBrush)
                ) {
                    // Prestige shimmer + particles overlay
                    PrestigeHeroOverlay(tier = prestigeTier)

                    // Theme badge – top right
                    equippedTheme?.let { theme ->
                        Row(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(10.dp)
                                .background(Color.White.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(theme.powerUpIcon ?: "🎨", fontSize = 12.sp)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                text = theme.name,
                                color = Color.White,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Pet emoji – bottom left, tap to show speech bubble
                    equippedPet?.petIcon?.let { petIcon ->
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .padding(start = 16.dp, bottom = 10.dp)
                                .size(38.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.25f))
                                .clickable {
                                    currentBubbleText = getRandomBubbleForPet(petIcon)
                                    showPetBubble = true
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(petIcon, fontSize = 20.sp)
                        }
                    }
                }

                // Content column – avatar straddles the gradient / surface boundary
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Spacer: 110 dp gradient − 40 dp (half of 80 dp avatar) = 70 dp
                    Spacer(Modifier.height(70.dp))

                    // Avatar with prestige border
                    val surfaceColor = MaterialTheme.colorScheme.surface
                    PrestigeAvatarBorder(tier = prestigeTier, surfaceColor = surfaceColor) {
                        Box(
                            modifier = Modifier
                                .size(80.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer)
                                .clickable { showFullscreenImage = true },
                            contentAlignment = Alignment.Center
                        ) {
                            if (profileImageUri != null) {
                                AsyncImage(
                                    model = File(profileImageUri!!),
                                    contentDescription = "Profilový obrázek",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Profilový obrázek",
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Camera button
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .size(26.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                                .clickable { imagePickerLauncher.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PhotoCamera,
                                contentDescription = "Změnit obrázek",
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Name / inline edit
                    if (isEditingName) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Jméno") },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp),
                            trailingIcon = {
                                Row {
                                    IconButton(onClick = {
                                        profileName = tempName
                                        saveProfileName(context, tempName)
                                        isEditingName = false
                                    }) {
                                        Icon(Icons.Default.Check, contentDescription = "Uložit")
                                    }
                                    IconButton(onClick = {
                                        tempName = profileName
                                        isEditingName = false
                                    }) {
                                        Icon(Icons.Default.Close, contentDescription = "Zrušit")
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
                                text = profileName.ifEmpty { "Nastav jméno" },
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (profileName.isEmpty())
                                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                else
                                    MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Upravit jméno",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // Points pill + backpack button
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { navController.navigate(ShopRoute) }
                                .padding(horizontal = 18.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Stars,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(Modifier.width(6.dp))
                                Text(
                                    text = "${formatPointsBalance(totalPointsFloat)} bodů",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        Spacer(Modifier.width(12.dp))

                        Box(
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable { navController.navigate(BackpackRoute) }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Backpack,
                                contentDescription = "Aktovka",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (grades.isNotEmpty()) {
                            var selectedGradeId by remember { mutableStateOf(loadGradeId(context)) }
                            LaunchedEffect(grades) {
                                if (selectedGradeId == null && grades.isNotEmpty()) {
                                    val default = grades.find { it.level == 9 } ?: grades.last()
                                    selectedGradeId = default.id
                                    saveGradeId(context, default.id)
                                }
                            }
                            val selectedGrade = grades.find { it.id == selectedGradeId }
                            var expanded by remember { mutableStateOf(false) }

                            Spacer(Modifier.width(12.dp))

                            Box {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .background(
                                            MaterialTheme.colorScheme.primaryContainer,
                                            RoundedCornerShape(14.dp)
                                        )
                                        .clickable { expanded = true }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = selectedGrade?.name ?: grades.firstOrNull()?.name ?: "",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Vybrat ročník",
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    grades.forEach { grade ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    text = grade.name,
                                                    fontWeight = if (selectedGradeId == grade.id) FontWeight.Bold else FontWeight.Normal
                                                )
                                            },
                                            onClick = {
                                                selectedGradeId = grade.id
                                                saveGradeId(context, grade.id)
                                                expanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Pet speech bubble (overlaid above the card, aligned top-start)
        AnimatedVisibility(
            visible = showPetBubble,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 4.dp),
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ) + fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            ) + fadeOut(animationSpec = tween(300)) + scaleOut(
                targetScale = 0.6f,
                animationSpec = tween(300)
            )
        ) {
            val scale by animateFloatAsState(
                targetValue = if (isAnimatingOut) 0.9f else 1f,
                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                label = "bubble_scale"
            )
            Card(
                modifier = Modifier
                    .widthIn(max = 240.dp)
                    .graphicsLayer { scaleX = scale; scaleY = scale }
                    .clickable { isAnimatingOut = true },
                shape = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                elevation = CardDefaults.cardElevation(8.dp)
            ) {
                Text(
                    text = currentBubbleText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(14.dp)
                )
            }
        }
    }

    // Auto-dismiss bubble after 7 s
    LaunchedEffect(showPetBubble) {
        if (showPetBubble) {
            delay(7000)
            isAnimatingOut = true
        }
    }

    LaunchedEffect(isAnimatingOut) {
        if (isAnimatingOut) {
            delay(300)
            dismissBubbleForHour(context)
            showPetBubble = false
            isAnimatingOut = false
        }
    }

    // Fullscreen image dialog
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

                IconButton(
                    onClick = { showFullscreenImage = false },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Zavřít", tint = Color.White)
                }
            }
        }
    }

    // Image cropper
    if (showImageCropper) {
        tempImageForCropping?.let { imageToProcess ->
            ImageCropperDialog(
                imageUri = imageToProcess,
                onCropComplete = { croppedImagePath ->
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
fun PetCareSection() {
    val equippedPet by BackpackManager.instance.equippedPet.collectAsState()
    val context = LocalContext.current
    val foodCount by PetManager.foodCount.collectAsState()
    val waterCount by PetManager.waterCount.collectAsState()

    if (equippedPet == null) return

    val petId = equippedPet!!.id
    var refreshTrigger by remember { mutableStateOf(0) }
    var petHealth by remember { mutableStateOf(PetHealth.ALIVE) }
    var stage by remember { mutableStateOf(GrowthStage.BABY) }
    var growthLevel by remember { mutableStateOf(0f) }

    LaunchedEffect(petId, refreshTrigger) {
        PetManager.checkPetStatus(petId)
        if (PetManager.isPetInitialized(petId)) {
            petHealth = PetManager.getPetHealth(petId)
            stage = PetManager.getGrowthStage(petId)
            growthLevel = PetManager.getGrowthLevel(petId)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            if (petHealth == PetHealth.ALIVE) {
                // Growth stage + progress bar
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "${equippedPet!!.petIcon ?: "🐾"} ",
                        fontSize = 24.sp
                    )
                    Text(
                        text = stage.emoji,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stage.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "${(growthLevel * 100).toInt()}%",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { growthLevel },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = when (stage) {
                        GrowthStage.BABY -> Color(0xFFFF9800)
                        GrowthStage.ADOLESCENT -> Color(0xFF4CAF50)
                        GrowthStage.ADULT -> Color(0xFF2196F3)
                        GrowthStage.FULLY_GROWN -> Color(0xFF9C27B0)
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                // Hunger/thirst warning reminder
                val now = System.currentTimeMillis()
                val hourMs = 60 * 60 * 1000L
                val lastFed = PetManager.getLastFedTime(petId)
                val lastWatered = PetManager.getLastWateredTime(petId)
                val hoursSinceFed = if (lastFed > 0) (now - lastFed) / hourMs else 0L
                val hoursSinceWatered = if (lastWatered > 0) (now - lastWatered) / hourMs else 0L

                val isHungry = hoursSinceFed >= PetManager.HUNGER_WARNING_HOURS
                val isThirsty = hoursSinceWatered >= PetManager.THIRST_WARNING_HOURS

                if (isThirsty || isHungry) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                color = Color(0xFFFFF3E0).copy(alpha = 0.7f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        if (isThirsty && isHungry) {
                            Text(
                                text = "Zvířátko má hlad a žízeň!",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else if (isThirsty) {
                            Text(
                                text = "Zvířátko má žízeň! Napoj ho.",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        } else {
                            Text(
                                text = "Zvířátko má hlad! Nakrm ho.",
                                color = Color(0xFFE65100),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Feed/Water buttons with cooldown check
                val canFeed = PetManager.canFeedPet(petId)
                val canWater = PetManager.canWaterPet(petId)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Feed button
                    Button(
                        onClick = {
                            if (PetManager.feedPet(petId)) {
                                refreshTrigger++
                            } else if (!canFeed) {
                                val remaining = PetManager.getTimeUntilNextFeed(petId)
                                val totalMin = remaining / 60_000
                                val timeText = when {
                                    totalMin >= 24 * 60 -> "${totalMin / (24 * 60)} d"
                                    totalMin >= 60 -> "${totalMin / 60} h"
                                    else -> "${totalMin} min"
                                }
                                android.widget.Toast.makeText(
                                    context, "Zvířátko má plné bříško, počkej $timeText",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = foodCount > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFFF9800),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            if (canFeed) "🍖 Nakrmit" else "🍖 Plné bříško",
                            fontSize = 12.sp
                        )
                    }

                    // Water button
                    Button(
                        onClick = {
                            if (PetManager.waterPet(petId)) {
                                refreshTrigger++
                            } else if (!canWater) {
                                val remaining = PetManager.getTimeUntilNextWater(petId)
                                val totalMin = remaining / 60_000
                                val timeText = when {
                                    totalMin >= 24 * 60 -> "${totalMin / (24 * 60)} d"
                                    totalMin >= 60 -> "${totalMin / 60} h"
                                    else -> "${totalMin} min"
                                }
                                android.widget.Toast.makeText(
                                    context, "Zvířátko není žíznivé, počkej $timeText",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        },
                        enabled = waterCount > 0,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3),
                            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Text(
                            if (canWater) "💧 Napít" else "💧 Není žíznivé",
                            fontSize = 12.sp
                        )
                    }
                }

                // Cooldown remaining text
                if (!canFeed || !canWater) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (!canFeed) {
                            val remaining = PetManager.getTimeUntilNextFeed(petId)
                            val totalMin = remaining / 60_000
                            val timeText = when {
                                totalMin >= 24 * 60 -> "${totalMin / (24 * 60)} d"
                                totalMin >= 60 -> "${totalMin / 60} h"
                                else -> "${totalMin} min"
                            }
                            Text(
                                text = "🍖 za $timeText • ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        if (!canWater) {
                            val remaining = PetManager.getTimeUntilNextWater(petId)
                            val totalMin = remaining / 60_000
                            val timeText = when {
                                totalMin >= 24 * 60 -> "${totalMin / (24 * 60)} d"
                                totalMin >= 60 -> "${totalMin / 60} h"
                                else -> "${totalMin} min"
                            }
                            Text(
                                text = "💧 za $timeText",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Count info
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🍖 ${foodCount}x    💧 ${waterCount}x",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

            } else {
                // Pet is dead
                val causeText = if (petHealth == PetHealth.DEAD_THIRST) "žízní" else "hlady"
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "💀",
                        fontSize = 28.sp
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Tvé zvířátko zemřelo $causeText",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Oživ ho a bude tě znovu potřebovat!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Revive button
                Button(
                    onClick = {
                        if (PetManager.revivePet(petId)) {
                            refreshTrigger++
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Oživit (${PetManager.REVIVE_COST} bodů)")
                }
            }
        }
    }
}

@Composable
fun EquippedQuoteSection(navController: NavController) {
    val equippedQuote by BackpackManager.instance.equippedQuote.collectAsState()
    val equippedTheme by BackpackManager.instance.equippedTheme.collectAsState()

    equippedQuote?.quote?.let { fullQuote ->
        val parts = fullQuote.split(" - ")
        val quoteText = if (parts.size > 1) parts.dropLast(1).joinToString(" - ") else fullQuote
        val author = if (parts.size > 1) parts.last() else null

        val accentColor = equippedTheme?.let { ProfileThemeData.getThemeColors(it.id)?.primary }
            ?: MaterialTheme.colorScheme.tertiary

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(0.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(androidx.compose.foundation.layout.IntrinsicSize.Min)
            ) {
                // Themed left accent bar
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .fillMaxHeight()
                        .background(
                            accentColor,
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "“",
                        fontSize = 36.sp,
                        color = accentColor,
                        lineHeight = 24.sp,
                        modifier = Modifier.offset(y = (-4).dp)
                    )
                    Text(
                        text = quoteText,
                        style = MaterialTheme.typography.bodyMedium,
                        fontStyle = androidx.compose.ui.text.font.FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (author != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "— $author",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.End
                        )
                    }
                }
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
    val maxStreak = com.tobiso.tobisoappnative.utils.StreakUtils.getMaxStreak(context)
    
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
        7500 -> AchievementBadge("🌠", "Meteorický", "Letí vesmírem", Color(0xFF607D8B))
        10000 -> AchievementBadge("🎇", "Deset tisíc", "Magická hranice", Color(0xFFE91E63))
        15000 -> AchievementBadge("🏛️", "Architekt", "Stavitel znalostí", Color(0xFF795548))
        20000 -> AchievementBadge("🧬", "Vědec", "Hloubka výzkumu", Color(0xFF00BCD4))
        25000 -> AchievementBadge("🔑", "Klíčník", "Otevírá tajemství", Color(0xFFFFEB3B))
        30000 -> AchievementBadge("⚜️", "Šlechtic", "Vznešený duch", Color(0xFF9C27B0))
        40000 -> AchievementBadge("🌋", "Vulkán", "Výbuchová energie", Color(0xFFFF5722))
        50000 -> AchievementBadge("🦁", "Lev", "Půl sta tisíc", Color(0xFFFF9800))
        60000 -> AchievementBadge("🌊", "Oceán", "Hlubiny poznání", Color(0xFF2196F3))
        75000 -> AchievementBadge("🦋", "Metamorfóza", "Úplná proměna", Color(0xFF4CAF50))
        100000 -> AchievementBadge("👑", "Vševědoucí", "100 000 bodů!", Color(0xFFFFD700))
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
    val totalEarnedPoints by PointsManager.instance.totalEarnedPoints.collectAsState()
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
    
    var expanded by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
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
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Sbalit" else "Rozbalit",
                    tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.size(24.dp)
                )
            }
            
            AnimatedVisibility(visible = expanded) {
                Column {
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

@Composable
fun GradeSelectorSection(grades: List<com.tobiso.tobisoappnative.model.Grade>) {
    val context = LocalContext.current
    var selectedGradeId by remember { mutableStateOf(loadGradeId(context)) }

    LaunchedEffect(grades) {
        if (selectedGradeId == null && grades.isNotEmpty()) {
            val default = grades.find { it.level == 9 } ?: grades.last()
            selectedGradeId = default.id
            saveGradeId(context, default.id)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Text(
                text = "Ročník",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Zobrazí obsah přizpůsobený vybranému ročníku",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (grades.isEmpty()) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grades.forEach { grade ->
                        FilterChip(
                            selected = selectedGradeId == grade.id,
                            onClick = {
                                selectedGradeId = grade.id
                                saveGradeId(context, grade.id)
                            },
                            label = { Text(grade.name) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileSectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 6.dp)
    )
}

@Composable
private fun ProfileNavGroup(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column { content() }
    }
}

@Composable
private fun DebugAddPointsSection() {
    var input by remember { mutableStateOf("") }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "DEBUG",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it.filter { c -> c.isDigit() } },
                    label = { Text("Body") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = {
                    val n = input.toIntOrNull() ?: return@Button
                    PointsManager.instance.addPoints(n)
                    input = ""
                }) {
                    Text("Přidat")
                }
            }
        }
    }
}

@Composable
private fun ProfileNavRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
            modifier = Modifier.size(20.dp)
        )
    }
}