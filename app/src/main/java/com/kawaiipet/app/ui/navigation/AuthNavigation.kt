package com.kawaiipet.app.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kawaiipet.app.ui.screens.auth.AuthWelcomeScreen
import com.kawaiipet.app.ui.screens.auth.LoginScreen
import com.kawaiipet.app.ui.screens.auth.SignUpScreen

object AuthRoutes {
    const val WELCOME = "auth_welcome"
    const val LOGIN = "auth_login"
    const val SIGN_UP = "auth_sign_up"
}

@Composable
fun AuthFlowNavHost(
    onAuthenticated: () -> Unit,
) {
    val navController = rememberNavController()
    NavHost(
        navController = navController,
        startDestination = AuthRoutes.WELCOME,
    ) {
        composable(AuthRoutes.WELCOME) {
            AuthWelcomeScreen(
                onLoginClick = {
                    navController.navigate(AuthRoutes.LOGIN) {
                        launchSingleTop = true
                    }
                },
                onSignUpClick = {
                    navController.navigate(AuthRoutes.SIGN_UP) {
                        launchSingleTop = true
                    }
                },
            )
        }
        composable(AuthRoutes.LOGIN) {
            LoginScreen(
                navController = navController,
                onAuthenticated = onAuthenticated,
            )
        }
        composable(AuthRoutes.SIGN_UP) {
            SignUpScreen(
                navController = navController,
                onAuthenticated = onAuthenticated,
            )
        }
    }
}
