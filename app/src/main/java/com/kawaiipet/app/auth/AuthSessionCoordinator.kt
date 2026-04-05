package com.kawaiipet.app.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed interface AuthGateState {
    data object Checking : AuthGateState
    data object Unauthenticated : AuthGateState
    data object Authenticated : AuthGateState
}

@Singleton
class AuthSessionCoordinator @Inject constructor(
    private val supabase: SupabaseClient,
) {
    private val _state = MutableStateFlow<AuthGateState>(AuthGateState.Checking)
    val state: StateFlow<AuthGateState> = _state.asStateFlow()

    suspend fun refresh() {
        supabase.auth.awaitReady()
        _state.value =
            if (supabase.auth.currentSessionOrNull() != null) AuthGateState.Authenticated
            else AuthGateState.Unauthenticated
    }

    fun setAuthenticated() {
        _state.value = AuthGateState.Authenticated
    }

    fun setUnauthenticated() {
        _state.value = AuthGateState.Unauthenticated
    }
}
