package com.example.tobisoappnative.screens

import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.tobisoappnative.viewmodel.MainViewModel
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.*

// Nová datová třída pro přehledné vracení obou hodnot
data class StreakInfo(val currentStreak: Int, val maxStreak: Int)

@RequiresApi(Build.VERSION_CODES.O)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StreakScreen(
    navController: NavController,
    viewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val today = remember { Calendar.getInstance() }
    var calendarMonth by remember { mutableIntStateOf(today.get(Calendar.MONTH)) }
    var calendarYear by remember { mutableIntStateOf(today.get(Calendar.YEAR)) }

    // --- OPRAVA 3: Znovu jsem zapnul přidávání dnešního dne ---
    LaunchedEffect(Unit) {
        addTodayToStreak(context)
    }

    val streakDays by remember(calendarMonth, calendarYear) {
        mutableStateOf(getStreakDays(context))
    }
    val currentDateString = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.time)

    // --- NOVÝ A EFEKTIVNÍ VÝPOČET ---
    val (currentStreak, maxStreak) = remember(streakDays) {
        calculateStreaks(streakDays)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        LargeTopAppBar(
            title = { Text("Řada") },
            navigationIcon = {
                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowUpward, contentDescription = "Zpět")
                }
            }
        )

        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = "Řada",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Aktuální řada",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$currentStreak ${denDnyDni(currentStreak)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Whatshot,
                            contentDescription = "Max Streak",
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            text = "Nejdelší řada",
                            style = MaterialTheme.typography.labelMedium
                        )
                        Text(
                            text = "$maxStreak ${denDnyDni(maxStreak)}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (calendarMonth == 0) {
                        calendarMonth = 11
                        calendarYear -= 1
                    } else {
                        calendarMonth -= 1
                    }
                }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Předchozí měsíc")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = monthYearString(calendarMonth, calendarYear),
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(onClick = {
                    if (calendarMonth == 11) {
                        calendarMonth = 0
                        calendarYear += 1
                    } else {
                        calendarMonth += 1
                    }
                }) {
                    Icon(Icons.Default.ArrowForward, contentDescription = "Další měsíc")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            CalendarStreak(
                streakDays = streakDays,
                month = calendarMonth,
                year = calendarYear,
                todayString = currentDateString
            )
        }
    }
}

/**
 * Nová, optimalizovaná funkce pro výpočet aktuální a maximální série.
 * Používá java.time.LocalDate pro výrazně vyšší výkon.
 */
@RequiresApi(Build.VERSION_CODES.O)
fun calculateStreaks(streakDays: Set<String>): StreakInfo {
    if (streakDays.isEmpty()) {
        return StreakInfo(0, 0)
    }

    val sortedDates = streakDays.map { LocalDate.parse(it) }.sorted()

    // Pokud je v seznamu jen jeden den, série je 1.
    if (sortedDates.size == 1) {
        return StreakInfo(1, 1)
    }

    var maxStreak = 1
    var runningStreak = 1

    for (i in 1 until sortedDates.size) {
        if (sortedDates[i].minusDays(1) == sortedDates[i - 1]) {
            runningStreak++
        } else {
            runningStreak = 1
        }
        if (runningStreak > maxStreak) {
            maxStreak = runningStreak
        }
    }

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

    return StreakInfo(currentStreak = currentStreak, maxStreak = maxStreak)
}

// --- ZMĚNA: Napojení na reálné úložiště telefonu ---
@RequiresApi(Build.VERSION_CODES.O)
fun addTodayToStreak(context: Context) {
    // 1. Otevřeme si soubor v paměti telefonu (pokud neexistuje, vytvoří se).
    val sharedPreferences = context.getSharedPreferences("StreakData", Context.MODE_PRIVATE)

    // 2. Načteme si stávající dny (pokud žádné nejsou, vezmeme prázdný seznam).
    val existingDays = sharedPreferences.getStringSet("streak_days", emptySet()) ?: emptySet()

    // 3. Vytvoříme si kopii, do které můžeme přidávat (původní seznam je jen pro čtení).
    val newDays = existingDays.toMutableSet()

    // 4. Přidáme dnešní datum.
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val today = LocalDate.now().format(formatter)
    newDays.add(today)

    // 5. Uložíme nový, rozšířený seznam zpět do paměti.
    sharedPreferences.edit().putStringSet("streak_days", newDays).apply()
    println("Today ($today) was added to streak. Total days: ${newDays.size}")
}


// --- ZMĚNA: Načítání z reálného úložiště telefonu ---
@RequiresApi(Build.VERSION_CODES.O)
fun getStreakDays(context: Context): Set<String> {
    val sharedPreferences = context.getSharedPreferences("StreakData", Context.MODE_PRIVATE)
    // Jednoduše načteme a vrátíme uložená data. Pokud žádná nejsou, vrátí se prázdný seznam.
    return sharedPreferences.getStringSet("streak_days", emptySet()) ?: emptySet()
}

fun denDnyDni(count: Int): String {
    return when {
        count == 1 -> "den"
        count in 2..4 -> "dny"
        else -> "dní"
    }
}


@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CalendarStreak(
    streakDays: Set<String>,
    month: Int,
    year: Int,
    todayString: String
) {
    val days = getMonthDaysGrid(month, year)
    val weekDays = listOf("Po", "Út", "St", "Čt", "Pá", "So", "Ne")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Start
    ) {
        weekDays.forEach { weekDay ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = weekDay,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1
                )
            }
        }
    }

    Spacer(modifier = Modifier.height(4.dp))

    Column {
        for (week in days.chunked(7)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                for (day in week) {
                    val isActive = day.fullDate != null && streakDays.contains(day.fullDate)
                    val isToday = day.fullDate == todayString

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .padding(2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // --- OPRAVA 1: Kompletně předělaná logika zobrazení dne ---
                        if (isToday) {
                            val bgColor = if (isActive) MaterialTheme.colorScheme.primary else Color.Transparent
                            val textColor = if (isActive) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary
                            Surface(
                                shape = CircleShape,
                                color = bgColor,
                                border = if (!isActive) BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else null,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                                    Text(
                                        text = day.dayNumber.toString(),
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = textColor
                                        )
                                    )
                                }
                            }
                        } else if (isActive) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(MaterialTheme.colorScheme.tertiaryContainer, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.dayNumber.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                )
                            }
                        } else if (day.isCurrentMonth) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = day.dayNumber.toString(),
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

data class CalendarDay(val dayNumber: Int, val isCurrentMonth: Boolean, val fullDate: String?)

@RequiresApi(Build.VERSION_CODES.O)
fun getMonthDaysGrid(month: Int, year: Int): List<CalendarDay> {
    val firstDayOfMonth = LocalDate.of(year, month + 1, 1)
    val firstDayOfWeek = firstDayOfMonth.dayOfWeek.value // 1 = pondělí, 7 = neděle

    val days = mutableListOf<CalendarDay>()

    // Dny z předchozího měsíce
    val daysFromPrevMonth = firstDayOfWeek - 1
    val prevMonth = firstDayOfMonth.minusMonths(1)
    val daysInPrevMonth = prevMonth.lengthOfMonth()
    for (i in 0 until daysFromPrevMonth) {
        val dayNum = daysInPrevMonth - daysFromPrevMonth + i + 1
        days.add(CalendarDay(dayNum, false, null))
    }

    // Aktuální měsíc
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val daysInMonth = firstDayOfMonth.lengthOfMonth()
    for (i in 1..daysInMonth) {
        val date = LocalDate.of(year, month + 1, i).format(formatter)
        days.add(CalendarDay(i, true, date))
    }

    // Dny z dalšího měsíce, abychom zaplnili mřížku na 42 dní (6 řádků)
    var nextDay = 1
    while (days.size < 42) {
        days.add(CalendarDay(nextDay, false, null))
        nextDay++
    }
    return days
}


fun monthYearString(month: Int, year: Int): String {
    val months = listOf(
        "Leden", "Únor", "Březen", "Duben", "Květen", "Červen",
        "Červenec", "Srpen", "Září", "Říjen", "Listopad", "Prosinec"
    )
    return "${months[month]} $year"
}


