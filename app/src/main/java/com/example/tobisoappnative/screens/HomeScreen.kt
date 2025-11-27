package com.example.tobisoappnative.screens

import com.example.tobisoappnative.R
import com.example.tobisoappnative.components.FloatingSearchBar
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.tobisoappnative.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.tobisoappnative.PointsManager
import com.example.tobisoappnative.BackpackManager
import com.example.tobisoappnative.IconPackManager
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import com.example.tobisoappnative.components.MultiplierIndicator
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.tobisoappnative.utils.StreakUtils
import kotlinx.coroutines.delay

// Helper funkce pro získání aktuální řady (nyní s freeze podporou)
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreak(context: Context): Int {
    return StreakUtils.getCurrentStreak(context)
}

data class Subject(
    val name: String,
    val icon: ImageVector,
    val colorType: SubjectColorType,
    val text: String,
)

enum class SubjectColorType {
    PRIMARY, SECONDARY, TERTIARY, ERROR, OUTLINE,
    PRIMARY_CONTAINER, SECONDARY_CONTAINER, TERTIARY_CONTAINER,
    SURFACE_VARIANT
}

@Composable
fun getColumnCount(): Int {
    val configuration = LocalConfiguration.current
    return when {
        configuration.screenWidthDp >= 840 -> 3  // Tablet landscape
        configuration.screenWidthDp >= 600 -> 2  // Tablet portrait / velký mobil
        else -> 1  // Mobil
    }
}

// Funkce pro získání barvy podle názvu předmětu
@Composable 
fun getSubjectColorByName(subjectName: String): Color {
    val colorType = when (subjectName) {
        "Mluvnice" -> SubjectColorType.PRIMARY
        "Literatura" -> SubjectColorType.SECONDARY  
        "Sloh" -> SubjectColorType.TERTIARY
        "Hudební výchova" -> SubjectColorType.PRIMARY_CONTAINER
        "Matematika" -> SubjectColorType.SECONDARY_CONTAINER
        "Chemie" -> SubjectColorType.ERROR
        "Fyzika" -> SubjectColorType.TERTIARY_CONTAINER
        "Přírodopis" -> SubjectColorType.OUTLINE
        "Zeměpis" -> SubjectColorType.SURFACE_VARIANT
        else -> SubjectColorType.PRIMARY // Výchozí barva
    }
    return getSubjectColor(colorType)
}

@Composable
fun getSubjectColor(colorType: SubjectColorType): Color {
    val isDarkTheme = isSystemInDarkTheme()

    return if (isDarkTheme) {
        // Šedější barvy pro tmavý režim
        when (colorType) {
            SubjectColorType.PRIMARY -> Color(0xFF9E9E9E)
            SubjectColorType.SECONDARY -> Color(0xFF757575)
            SubjectColorType.TERTIARY -> Color(0xFF616161)
            SubjectColorType.ERROR -> Color(0xFF8C8C8C)
            SubjectColorType.OUTLINE -> Color(0xFF666666)
            SubjectColorType.PRIMARY_CONTAINER -> Color(0xFF7C7C7C)
            SubjectColorType.SECONDARY_CONTAINER -> Color(0xFF696969)
            SubjectColorType.TERTIARY_CONTAINER -> Color(0xFF545454)
            SubjectColorType.SURFACE_VARIANT -> Color(0xFF424242)
        }
    } else {
        // Barvy podle názvu předmětů pro světlý režim
        when (colorType) {
            SubjectColorType.PRIMARY -> Color(0xFF2196F3)        // Mluvnice - Modrá
            SubjectColorType.SECONDARY -> Color(0xFF8B4513)      // Literatura - Hnědá
            SubjectColorType.TERTIARY -> Color(0xFFFF9800)       // Sloh - Oranžová
            SubjectColorType.PRIMARY_CONTAINER -> Color(0xFF9C27B0)     // Hudební výchova - Fialová
            SubjectColorType.SECONDARY_CONTAINER -> Color(0xFF1976D2)   // Matematika - Tmavě modrá
            SubjectColorType.ERROR -> Color(0xFFF44336)          // Chemie - Červená
            SubjectColorType.TERTIARY_CONTAINER -> Color(0xFF607D8B)    // Fyzika - Modro-šedá
            SubjectColorType.OUTLINE -> Color(0xFF4CAF50)        // Přírodopis - Zelená
            SubjectColorType.SURFACE_VARIANT -> Color(0xFF795548)       // Zeměpis - Hnědá
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavHostController) {
    val isDark = isSystemInDarkTheme()
    val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light
    val subjects = listOf(
        Subject("Mluvnice", Icons.Default.Spellcheck, SubjectColorType.PRIMARY, "Gramatika a pravopis českého jazyka"),
        Subject("Literatura", Icons.Default.MenuBook, SubjectColorType.SECONDARY, "Česká a světová literatura"),
        Subject("Sloh", Icons.Default.Description, SubjectColorType.TERTIARY, "Tvorba textů a slohové útvary"),
        Subject("Hudební výchova", Icons.Default.LibraryMusic, SubjectColorType.PRIMARY_CONTAINER, "Hudební teorie, autoři, žánry, písně, díla a dějiny"),
        Subject("Matematika", Icons.Default.Calculate, SubjectColorType.SECONDARY_CONTAINER, "Algebra a geometrie"),
        Subject("Chemie", Icons.Default.Science, SubjectColorType.ERROR, "Tělesa, látky, zákony, prvky a sloučeniny"),
        Subject("Fyzika", Icons.Default.PrecisionManufacturing, SubjectColorType.TERTIARY_CONTAINER, "Zákony fyziky, elektřina, radioaktivita, veličiny, stroje a světlo"),
        Subject("Přírodopis", Icons.Default.Eco, SubjectColorType.OUTLINE, "Lidské tělo, minerály a horniny"),
        Subject("Zeměpis", Icons.Default.Public, SubjectColorType.SURFACE_VARIANT, "Vše o povrchu, obyvatelstvu, hospodářství a ochraně přírody ČR"),
    )
    val context = LocalContext.current
    val columnCount = getColumnCount()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyGridState()
    val viewModel: MainViewModel = viewModel()
    
    LaunchedEffect(Unit) { 
        viewModel.loadCategories()
        viewModel.loadPosts()
        BackpackManager.init(context)
        IconPackManager.init(context)
    }
    
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }
    val offlineDownloading by viewModel.offlineDownloading.collectAsState()
    val offlineProgress by viewModel.offlineDownloadProgress.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .nestedScroll(scrollBehavior.nestedScrollConnection)
        ) {
            LargeTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "Logo",
                        modifier = Modifier.size(150.dp)
                    )
                },
                actions = {
                    // Zobrazení aktivního multiplikátoru
                    MultiplierIndicator()
                    
                    // Zobrazení bodů s novým designem
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
                    // Offline download indicator / tlačítko
                    if (offlineDownloading) {
                        Box(modifier = Modifier.padding(end = 8.dp)) {
                            CircularProgressIndicator(
                                progress = offlineProgress.coerceIn(0f, 1f),
                                modifier = Modifier.size(28.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    // Streak button s počtem dní (s freeze podporou)
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    // Sledování změn v freeze
                    val availableFreezes by com.example.tobisoappnative.StreakFreezeManager.availableFreezes.collectAsState()
                    val usedFreezes by com.example.tobisoappnative.StreakFreezeManager.usedFreezes.collectAsState()
                    
                    LaunchedEffect(availableFreezes, usedFreezes) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = getCurrentStreak(context)
                        }
                    }
                    
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .clickable { navController.navigate("streak") }
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
            
            // Grid se subjects
            LazyVerticalGrid(
                columns = GridCells.Fixed(columnCount),
                state = gridState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(subjects) { subject ->
                    SubjectCard(
                        subject = subject,
                        onClick = { navController.navigate("categoryList/${subject.name}") },
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
        
        // Overlay pro celkové body
        if (showTotalOverlay) {
            FullScreenTotalPointsOverlay(totalPoints = totalPoints)
            LaunchedEffect(showTotalOverlay) {
                delay(2200)
                showTotalOverlay = false
            }
        }

        // Snackbar pro toastMessage z ViewModel
        LaunchedEffect(toastMessage
        ) {
            toastMessage?.let { msg ->
                snackbarHostState.showSnackbar(msg)
                viewModel.clearToast()
            }
        }

        // Snackbar host (moved higher)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 80.dp),
            contentAlignment = Alignment.TopCenter
        ) {
            SnackbarHost(hostState = snackbarHostState)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubjectCard(
    subject: Subject,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        onClick = onClick,
        modifier = modifier.height(100.dp),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val subjectIcon = IconPackManager.getSubjectIcon(subject.name)
            val isEmoji = IconPackManager.isEmojiIcon(subject.name)
            
            // Ikona nebo emoji
            if (isEmoji && subjectIcon is String) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = subjectIcon,
                        fontSize = 28.sp,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Clip
                    )
                }
            } else {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (subjectIcon is ImageVector) subjectIcon else subject.icon,
                        contentDescription = subject.name,
                        tint = getSubjectColor(subject.colorType),
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = subject.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = subject.text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
