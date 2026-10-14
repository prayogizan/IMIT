package com.uncaan.imit.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.uncaan.imit.app.ui.MitOcwApp
import com.uncaan.imit.core.designsystem.theme.MitOcwTheme

/**
 * Main application activity hosting the single-Activity Jetpack Compose user interface.
 *
 * Configured with `launchMode="singleTop"` to receive incoming deep link intents via [onNewIntent]
 * without re-creating the Activity instance or losing runtime UI state.
 */
class MainActivity : ComponentActivity() {

    private var navController: NavHostController? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val navHostController = rememberNavController()
            navController = navHostController
            MitOcwTheme {
                MitOcwApp(navController = navHostController)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        navController?.handleDeepLink(intent)
    }
}
