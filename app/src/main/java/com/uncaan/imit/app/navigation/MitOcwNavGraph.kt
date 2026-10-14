package com.uncaan.imit.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import androidx.navigation.navDeepLink
import com.uncaan.imit.core.player.VideoPlayerManager
import com.uncaan.imit.core.player.VideoPlayerScreen
import com.uncaan.imit.feature.catalog.CatalogScreen
import com.uncaan.imit.feature.details.DetailScreen
import com.uncaan.imit.feature.downloads.DownloadsScreen
import org.koin.compose.koinInject
import java.net.URLDecoder

/**
 * Root navigation graph composable for the MIT OCW application.
 *
 * Configures the [NavHost] destinations, argument parsing, deep link URL handling,
 * and navigation transitions between Catalog, Details, Downloads, and Player screens.
 *
 * Registered deep links:
 * - Details: `imit://app/details/{identifier}`
 * - Downloads: `imit://app/downloads`
 *
 * @param navController Controller managing app navigation state and back stack.
 * @param modifier Optional [Modifier] applied to the host layout.
 */
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
            arguments = listOf(navArgument("identifier") { type = NavType.StringType }),
            deepLinks = listOf(
                navDeepLink { uriPattern = NavRoutes.DETAILS_DEEP_LINK_PATTERN }
            )
        ) { backStackEntry ->
            val identifier = backStackEntry.arguments?.getString("identifier").orEmpty()
            DetailScreen(
                identifier = identifier,
                onBackClick = {
                    if (!navController.popBackStack()) {
                        navController.navigate(NavRoutes.CATALOG) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                },
                onPlayVideo = { videoUrl ->
                    navController.navigate(NavRoutes.playerRoute(videoUrl))
                }
            )
        }

        composable(
            route = NavRoutes.DOWNLOADS,
            deepLinks = listOf(
                navDeepLink { uriPattern = NavRoutes.DOWNLOADS_DEEP_LINK_PATTERN }
            )
        ) {
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
