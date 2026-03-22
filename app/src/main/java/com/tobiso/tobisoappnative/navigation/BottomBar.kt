package com.tobiso.tobisoappnative.navigation

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
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomBar(navController: NavHostController, searchRequestFocus: MutableState<Boolean>) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination

    NavigationBar (
        modifier = Modifier.padding(top = 0.dp)
    ) {
        NavigationBarItem(
            icon = { Icon(Icons.Default.Home, contentDescription = "Zobrazení všech předmětů") },
            label = { Text("Učivo", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination?.hasRoute(HomeRoute::class) == true,
            onClick = { navController.navigate(HomeRoute) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.AutoMirrored.Filled.DirectionsWalk, contentDescription = "Procvičování všech otázek") },
            label = { Text("Procvičování", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination?.hasRoute(AllQuestionsRoute::class) == true,
            onClick = { navController.navigate(AllQuestionsRoute) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.CalendarToday, contentDescription = "Kalendář s událostmi") },
            label = { Text("Kalendář", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination?.hasRoute(CalendarRoute::class) == true,
            onClick = { navController.navigate(CalendarRoute) }
        )
        NavigationBarItem(
            icon = { Icon(Icons.Default.Person, contentDescription = "Profil") },
            label = { Text("Profil", style = MaterialTheme.typography.labelSmall.copy(fontSize = 12.sp)) },
            selected = currentDestination?.hasRoute(ProfileRoute::class) == true,
            onClick = { navController.navigate(ProfileRoute) }
        )
    }
}
