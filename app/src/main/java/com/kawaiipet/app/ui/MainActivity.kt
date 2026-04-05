package com.kawaiipet.app.ui

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.kawaiipet.app.auth.AuthSessionCoordinator
import com.kawaiipet.app.ui.navigation.AppNavigation
import com.kawaiipet.app.ui.theme.KawaiiPetTheme
import com.kawaiipet.app.util.PetLauncher
import dagger.hilt.android.AndroidEntryPoint
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.handleDeeplinks
import javax.inject.Inject
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var supabase: SupabaseClient

    @Inject
    lateinit var authSessionCoordinator: AuthSessionCoordinator

    private val startPetRequestViewModel: StartPetRequestViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthRedirect(intent)
        handleStartPetIntent(intent)
        enableEdgeToEdge()
        setContent {
            KawaiiPetTheme {
                AppNavigation()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleAuthRedirect(intent)
        handleStartPetIntent(intent)
    }

    private fun handleStartPetIntent(intent: Intent?) {
        if (intent?.action == PetLauncher.ACTION_START_PET) {
            startPetRequestViewModel.setStartPetRequested(true)
        }
    }

    private fun handleAuthRedirect(intent: Intent?) {
        intent ?: return
        supabase.handleDeeplinks(intent) {
            // Session is stored by the SDK; re-read so the auth gate matches (cold start, returning from browser, PKCE completes async).
            lifecycleScope.launch {
                authSessionCoordinator.refresh()
            }
        }
    }
}
