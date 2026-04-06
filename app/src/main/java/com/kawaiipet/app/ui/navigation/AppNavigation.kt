package com.kawaiipet.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kawaiipet.app.auth.AuthGateState
import com.kawaiipet.app.ui.auth.AuthGateViewModel
import com.kawaiipet.app.ui.screens.CustomizeScreen
import com.kawaiipet.app.ui.screens.HomeScreen
import com.kawaiipet.app.ui.screens.MemoryScreen
import com.kawaiipet.app.ui.screens.SettingsScreen

object Routes {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val CUSTOMIZE = "customize"
    const val MEMORY = "memory"
}

@Composable
fun AppNavigation(
    gateViewModel: AuthGateViewModel = hiltViewModel(),
) {
    val gateState by gateViewModel.gateState.collectAsStateWithLifecycle()

    when (gateState) {
        AuthGateState.Checking -> {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }
        }
        AuthGateState.Unauthenticated -> {
            AuthFlowNavHost(
                onAuthenticated = { gateViewModel.onAuthenticated() },
            )
        }
        AuthGateState.Authenticated -> {
            val navController = rememberNavController()
            NavHost(navController = navController, startDestination = Routes.HOME) {
                composable(Routes.HOME) {
                    HomeScreen(navController = navController)
                }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        navController = navController,
                        onSignedOut = { gateViewModel.onSignedOut() },
                    )
                }
                composable(Routes.CUSTOMIZE) {
                    CustomizeScreen(navController = navController)
                }
                composable(Routes.MEMORY) {
                    MemoryScreen(navController = navController)
                }
            }
        }
    }
}
