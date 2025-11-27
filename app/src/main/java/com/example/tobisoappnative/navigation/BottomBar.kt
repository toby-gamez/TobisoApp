package com.example.tobisoappnative.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsWalk
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
            icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Procvičování všech otázek") },
            label = { Text("Procvičování", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "allQuestions",
            onClick = { navController.navigate("allQuestions") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Kalendář s událostmi") },
            label = { Text("Kalendář", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "calendar",
            onClick = { navController.navigate("calendar") }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination == "profile",
            onClick = { navController.navigate("profile") }
        )
    }
}
