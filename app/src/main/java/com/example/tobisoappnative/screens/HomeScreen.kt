package com.example.tobisoappnative.screens

import com.example.tobisoappnative.R
import com.example.tobisoappnative.components.ToastHandler
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
import androidx.compose.foundation.isSystemInDarkTheme
import com.example.tobisoappnative.viewmodel.MainViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavController
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.example.tobisoappnative.PointsManager
import kotlinx.coroutines.delay
import com.example.tobisoappnative.components.FullScreenTotalPointsOverlay
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import java.time.LocalDate
import java.time.format.DateTimeFormatter

// Helper funkce pro získání aktuální řady
@RequiresApi(Build.VERSION_CODES.O)
fun getCurrentStreak(context: Context): Int {
    val sharedPreferences = context.getSharedPreferences("StreakData", Context.MODE_PRIVATE)
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
fun HomeScreen(navController: NavController) {
    val isDark = isSystemInDarkTheme()
    val logoRes = if (isDark) R.drawable.logo_dark else R.drawable.logo_light
    val subjects = listOf(
        Subject("Mluvnice", Icons.Default.Spellcheck, SubjectColorType.PRIMARY, "Gramatika a pravopis českého jazyka"),
        Subject("Literatura", Icons.Default.MenuBook, SubjectColorType.SECONDARY, "Česká a světová literatura"),
        Subject("Sloh", Icons.Default.Description, SubjectColorType.TERTIARY, "Tvorba textů a slohové útvary"),
        Subject("Hudební výchova", Icons.Default.LibraryMusic, SubjectColorType.PRIMARY_CONTAINER, "Hudební teorie, autoři, žánry, písně, díla a historie"),
        Subject("Matematika", Icons.Default.Calculate, SubjectColorType.SECONDARY_CONTAINER, "Algebra a geometrie"),
        Subject("Chemie", Icons.Default.Science, SubjectColorType.ERROR, "Tělesa, látky, zákony, prvky a sloučeniny"),
        Subject("Fyzika", Icons.Default.PrecisionManufacturing, SubjectColorType.TERTIARY_CONTAINER, "Zákony fyziky, elektřina, radioaktivita, veličiny, stroje a světlo"),
        Subject("Přírodopis", Icons.Default.Eco, SubjectColorType.OUTLINE, "Lidské tělo"),
        Subject("Zeměpis", Icons.Default.Public, SubjectColorType.SURFACE_VARIANT, "Vše o povrchu, obyvatelstvu, hospodářství a ochraně přírody ČR"),
    )
    val context = LocalContext.current
    val columnCount = getColumnCount()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val gridState = rememberLazyGridState()
    val viewModel: MainViewModel = viewModel()
    val categories by viewModel.categories.collectAsState()
    val categoryLoading by viewModel.categoryLoading.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    LaunchedEffect(Unit) { viewModel.loadCategories() }
    val totalPoints by PointsManager.totalPoints.collectAsState()
    var showTotalOverlay by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            LargeTopAppBar(
                title = {
                    Image(
                        painter = painterResource(id = logoRes),
                        contentDescription = "Logo",
                        modifier = Modifier.size(150.dp)
                    )
                },
                actions = {
                    // Kulaté tlačítko s body vlevo od ohýnku
                    val tertiaryColor = MaterialTheme.colorScheme.tertiary
                    val points = remember { mutableStateOf(PointsManager.getPoints()) }
                    // Aktualizace bodů při změně
                    LaunchedEffect(Unit) {
                        PointsManager.totalPoints.collect { total ->
                            points.value = total
                        }
                    }
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(40.dp)
                            .background(
                                color = tertiaryColor.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { showTotalOverlay = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = points.value.toString(),
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Streak button s počtem dní
                    val context = LocalContext.current
                    val currentStreak = remember { mutableStateOf(0) }
                    
                    LaunchedEffect(Unit) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                            currentStreak.value = getCurrentStreak(context)
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
            if (showTotalOverlay) {
                FullScreenTotalPointsOverlay(totalPoints = totalPoints)
                LaunchedEffect(showTotalOverlay) {
                    kotlinx.coroutines.delay(2200)
                    showTotalOverlay = false
                }
            }
        }
        if (categoryLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator()
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Načítání obsahu...")
                }
            }
        } else {
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(columnCount),
                contentPadding = PaddingValues(
                    start = 8.dp,
                    end = 8.dp,
                    top = 8.dp,
                    bottom = 8.dp
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(subjects) { subject ->
                    SubjectCard(
                        subject = subject,
                        modifier = Modifier,
                        onClick = {
                            val category = categories.find { it.name == subject.name }
                            category?.let {
                                navController.navigate("categoryList/${it.name}")
                            }
                        }
                    )
                }
            }
        }
    }
    
    // Toast handler pro zobrazení zpráv
    ToastHandler(
        toastMessage = toastMessage,
        onClearToast = { viewModel.clearToast() }
    )
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
            Icon(
                imageVector = subject.icon,
                contentDescription = subject.name,
                tint = getSubjectColor(subject.colorType),
                modifier = Modifier.size(32.dp)
            )

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