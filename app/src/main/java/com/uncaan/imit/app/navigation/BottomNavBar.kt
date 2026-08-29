package com.uncaan.imit.app.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Catalog : BottomNavItem(
        route = NavRoutes.CATALOG,
        title = "Catalog",
        selectedIcon = Icons.Filled.VideoLibrary,
        unselectedIcon = Icons.Outlined.VideoLibrary
    )

    data object Downloads : BottomNavItem(
        route = NavRoutes.DOWNLOADS,
        title = "Downloads",
        selectedIcon = Icons.Filled.Download,
        unselectedIcon = Icons.Outlined.Download
    )

    companion object {
        val items = listOf(Catalog, Downloads)
    }
}

fun shouldShowBottomBar(currentRoute: String?): Boolean {
    return when (currentRoute) {
        NavRoutes.CATALOG,
        NavRoutes.DOWNLOADS -> true
        else -> false
    }
}

@Composable
fun BottomNavBar(
    navController: NavController,
    currentRoute: String?,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier
    ) {
        BottomNavItem.items.forEach { item ->
            val isSelected = currentRoute == item.route

            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    if (!isSelected) {
                        navController.navigate(item.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = {
                    Text(text = item.title)
                }
            )
        }
    }
}
