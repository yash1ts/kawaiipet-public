package com.kawaiipet.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.kawaiipet.app.ui.navigation.AppNavigation
import com.kawaiipet.app.ui.theme.KawaiiPetTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KawaiiPetTheme {
                AppNavigation()
            }
        }
    }
}
