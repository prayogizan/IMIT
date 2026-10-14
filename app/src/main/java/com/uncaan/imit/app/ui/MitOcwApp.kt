package com.uncaan.imit.app.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.uncaan.imit.app.navigation.BottomNavBar
import com.uncaan.imit.app.navigation.MitOcwNavGraph
import com.uncaan.imit.app.navigation.shouldShowBottomBar

/**
 * Top-level application composable hosting the persistent [Scaffold], bottom navigation bar,
 * and the [MitOcwNavGraph].
 *
 * @param modifier Optional [Modifier] applied to the root [Scaffold].
 * @param navController Navigation controller managing app destinations. Defaults to [rememberNavController].
 */
@Composable
fun MitOcwApp(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            if (shouldShowBottomBar(currentRoute)) {
                BottomNavBar(
                    navController = navController,
                    currentRoute = currentRoute
                )
            }
        }
    ) { innerPadding ->
        MitOcwNavGraph(
            navController = navController,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
