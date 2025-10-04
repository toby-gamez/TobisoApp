package com.example.tobisoappnative.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.tobisoappnative.ui.theme.poppins

@Composable
fun BottomBar(navController: NavHostController, searchRequestFocus: MutableState<Boolean>) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination?.route

    NavigationBar (
        modifier = Modifier.padding(top = 0.dp)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Zobrazení všech předmětů") },
            label = { Text("Předměty", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "home",
            onClick = { navController.navigate("home") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.DirectionsWalk, contentDescription = "Procvičování všech otázek") },
            label = { Text("Procvičování", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "allQuestions",
            onClick = { navController.navigate("allQuestions") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Search, contentDescription = "Vyhledávání kategorií, článků a obsahu") },
            label = { Text("Vyhledávání", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "search",
            onClick = {
                if (currentDestination == "search") {
                    searchRequestFocus.value = true
                } else {
                    navController.navigate("search")
                }
            }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Kalendář s událostmi") },
            label = { Text("Kalendář", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "calendar",
            onClick = { navController.navigate("calendar") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Menu, contentDescription = "Více") },
            label = { Text("Více", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "more",
            onClick = { navController.navigate("more") }
        )
    }
}
