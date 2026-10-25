package com.tobiso.tobisoappnative.navigation

import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.rounded.CalendarMonth as CalendarMonthFilled
import androidx.compose.material.icons.rounded.Person as PersonFilled
import androidx.compose.material.icons.rounded.School as SchoolFilled
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
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.tobiso.tobisoappnative.R

@Composable
fun BottomBar(navController: NavHostController, searchRequestFocus: MutableState<Boolean>) {
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStackEntry?.destination
    val scheme = MaterialTheme.colorScheme
    val practiceIcon = ImageVector.vectorResource(R.drawable.ic_dumbbell)
    val practiceIconFilled = ImageVector.vectorResource(R.drawable.ic_dumbbell_filled)

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
            icon = {
                val bounce by androidx.compose.animation.core.animateFloatAsState(
                    if (selected) 1.12f else 1f,
                    androidx.compose.animation.core.spring(dampingRatio = 0.35f, stiffness = androidx.compose.animation.core.Spring.StiffnessMedium),
                    label = "navIconBounce"
                )
                androidx.compose.animation.Crossfade(selected, animationSpec = androidx.compose.animation.core.tween(200), label = "navIcon") { sel ->
                    Icon(
                        if (sel) selectedIcon else icon,
                        contentDescription = description,
                        modifier = Modifier.graphicsLayer { scaleX = bounce; scaleY = bounce }
                    )
                }
            },
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
            icon = Icons.Outlined.School,
            selectedIcon = Icons.Rounded.SchoolFilled,
            label = "Učivo",
            description = "Zobrazení všech předmětů",
            onClick = { navController.navigate(HomeRoute) }
        )
        Item(
            selected = currentDestination?.hasRoute(AllQuestionsRoute::class) == true,
            icon = practiceIcon,
            selectedIcon = practiceIconFilled,
            label = "Procvičování",
            description = "Procvičování všech otázek",
            onClick = { navController.navigate(AllQuestionsRoute) }
        )
        Item(
            selected = currentDestination?.hasRoute(CalendarRoute::class) == true,
            icon = Icons.Outlined.CalendarMonth,
            selectedIcon = Icons.Rounded.CalendarMonthFilled,
            label = "Kalendář",
            description = "Kalendář s událostmi",
            onClick = { navController.navigate(CalendarRoute) }
        )
        Item(
            selected = currentDestination?.hasRoute(ProfileRoute::class) == true,
            icon = Icons.Outlined.Person,
            selectedIcon = Icons.Rounded.PersonFilled,
            label = "Profil",
            description = "Profil",
            onClick = { navController.navigate(ProfileRoute) }
        )
    }
}
