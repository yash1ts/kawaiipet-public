package com.kawaiipet.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kawaiipet.app.ui.screens.HomeScreen
import com.kawaiipet.app.ui.screens.MemoryScreen
import com.kawaiipet.app.ui.screens.ModelDownloadScreen
import com.kawaiipet.app.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val MEMORY = "memory"
    const val MODEL_DOWNLOAD_ROUTE = "model_download?startKind={startKind}"

    /** @param startKind use [com.kawaiipet.app.audio.FeaturedVoiceModels] `NAV_*` or blank. */
    fun modelDownload(startKind: String = "") = "model_download?startKind=$startKind"
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = Routes.HOME) {
        composable(Routes.HOME) {
            HomeScreen(navController = navController)
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(navController = navController)
        }
        composable(Routes.MEMORY) {
            MemoryScreen(navController = navController)
        }
        composable(
            route = Routes.MODEL_DOWNLOAD_ROUTE,
            arguments = listOf(
                navArgument("startKind") {
                    type = NavType.StringType
                    defaultValue = ""
                }
            )
        ) { entry ->
            val startKind = entry.arguments?.getString("startKind").orEmpty()
            ModelDownloadScreen(
                navController = navController,
                autoStartKind = startKind
            )
        }
    }
}
