package com.uncaan.imit.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.uncaan.imit.core.player.VideoPlayerManager
import com.uncaan.imit.core.player.VideoPlayerScreen
import com.uncaan.imit.feature.catalog.CatalogScreen
import com.uncaan.imit.feature.details.DetailScreen
import com.uncaan.imit.feature.downloads.DownloadsScreen
import org.koin.compose.koinInject
import java.net.URLDecoder

@Composable
fun MitOcwNavGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = NavRoutes.CATALOG,
        modifier = modifier
    ) {
        composable(NavRoutes.CATALOG) {
            CatalogScreen(
                onVideoClick = { identifier ->
                    navController.navigate(NavRoutes.detailsRoute(identifier))
                }
            )
        }

        composable(
            route = NavRoutes.DETAILS,
            arguments = listOf(navArgument("identifier") { type = NavType.StringType })
        ) { backStackEntry ->
            val identifier = backStackEntry.arguments?.getString("identifier").orEmpty()
            DetailScreen(
                identifier = identifier,
                onBackClick = { navController.popBackStack() },
                onPlayVideo = { videoUrl ->
                    navController.navigate(NavRoutes.playerRoute(videoUrl))
                }
            )
        }

        composable(NavRoutes.DOWNLOADS) {
            DownloadsScreen(
                onPlayVideo = { videoUrl ->
                    navController.navigate(NavRoutes.playerRoute(videoUrl))
                }
            )
        }

        composable(
            route = NavRoutes.PLAYER,
            arguments = listOf(
                navArgument("videoUrl") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { backStackEntry ->
            val encodedUrl = backStackEntry.arguments?.getString("videoUrl").orEmpty()
            val videoUrl = if (encodedUrl.isNotEmpty()) {
                URLDecoder.decode(encodedUrl, "UTF-8")
            } else {
                ""
            }
            val playerManager: VideoPlayerManager = koinInject()

            VideoPlayerScreen(
                videoUrl = videoUrl,
                playerManager = playerManager,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
