package com.mitanshm.fitfindr

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.mitanshm.fitfindr.ui.CaptureScreen
import com.mitanshm.fitfindr.ui.HistoryScreen
import com.mitanshm.fitfindr.ui.ResultScreen
import com.mitanshm.fitfindr.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint

/**
 * Single-activity host for the Navigation Compose graph:
 * Capture -> Result -> History / Settings.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    FitFindrNavHost()
                }
            }
        }
    }
}

object FitFindrDestinations {
    const val CAPTURE = "capture"
    const val RESULT = "result"
    const val HISTORY = "history"
    const val SETTINGS = "settings"
}

@androidx.compose.runtime.Composable
private fun FitFindrNavHost() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = FitFindrDestinations.CAPTURE) {
        composable(FitFindrDestinations.CAPTURE) {
            CaptureScreen(
                onOutfitCaptured = { navController.navigate(FitFindrDestinations.RESULT) },
                onOpenHistory = { navController.navigate(FitFindrDestinations.HISTORY) },
                onOpenSettings = { navController.navigate(FitFindrDestinations.SETTINGS) },
            )
        }
        composable(FitFindrDestinations.RESULT) {
            ResultScreen(onDone = { navController.popBackStack() })
        }
        composable(FitFindrDestinations.HISTORY) {
            HistoryScreen(onBack = { navController.popBackStack() })
        }
        composable(FitFindrDestinations.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
    }
}
