package com.uncaan.imit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.uncaan.imit.app.ui.MitOcwApp
import com.uncaan.imit.core.designsystem.theme.MitOcwTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MitOcwTheme {
                MitOcwApp()
            }
        }
    }
}
