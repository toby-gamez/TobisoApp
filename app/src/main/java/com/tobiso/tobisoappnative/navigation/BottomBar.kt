package com.tobiso.tobisoappnative.navigation

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth as CalendarMonthFilled
import androidx.compose.material.icons.rounded.Person as PersonFilled
import androidx.compose.material.icons.rounded.School as SchoolFilled
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.rounded.Quiz as QuizFilled
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.School
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun BottomBar(navController: NavHostController, searchRequestFocus: MutableState<Boolean>) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val scheme = MaterialTheme.colorScheme

    val colors = NavigationBarItemDefaults.colors(
        indicatorColor = scheme.primaryContainer,
        selectedIconColor = scheme.onPrimaryContainer,
        selectedTextColor = scheme.onSurface,
        unselectedIconColor = scheme.onSurfaceVariant,
        unselectedTextColor = scheme.onSurfaceVariant
    )

    @Composable
    fun RowScope.Item(
        selected: Boolean,
        icon: ImageVector,
        selectedIcon: ImageVector,
        label: String,
        description: String,
        onClick: () -> Unit
    ) {
        NavigationBarItem(
            icon = { Icon(if (selected) selectedIcon else icon, contentDescription = description) },
            label = {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontSize = 12.sp,
                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                    )
                )
            },
            selected = selected,
            onClick = onClick,
            colors = colors
        )
    }

    NavigationBar(containerColor = scheme.surfaceContainer) {
        Item(
            selected = currentDestination?.hasRoute(HomeRoute::class) == true,
            icon = Icons.Rounded.School,
            selectedIcon = Icons.Rounded.SchoolFilled,
            label = "Učivo",
            description = "Zobrazení všech předmětů",
            onClick = { navController.navigate(HomeRoute) }
        )
        Item(
            selected = currentDestination?.hasRoute(AllQuestionsRoute::class) == true,
            icon = Icons.Outlined.Quiz,
            selectedIcon = Icons.Rounded.QuizFilled,
            label = "Procvičování",
            description = "Procvičování všech otázek",
            onClick = { navController.navigate(AllQuestionsRoute) }
        )
        Item(
            selected = currentDestination?.hasRoute(CalendarRoute::class) == true,
            icon = Icons.Rounded.CalendarMonth,
            selectedIcon = Icons.Rounded.CalendarMonthFilled,
            label = "Kalendář",
            description = "Kalendář s událostmi",
            onClick = { navController.navigate(CalendarRoute) }
        )
        Item(
            selected = currentDestination?.hasRoute(ProfileRoute::class) == true,
            icon = Icons.Rounded.Person,
            selectedIcon = Icons.Rounded.PersonFilled,
            label = "Profil",
            description = "Profil",
            onClick = { navController.navigate(ProfileRoute) }
        )
    }
}
