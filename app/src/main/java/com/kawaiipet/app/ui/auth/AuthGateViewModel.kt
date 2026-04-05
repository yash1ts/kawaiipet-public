package com.kawaiipet.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaiipet.app.auth.AuthSessionCoordinator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val coordinator: AuthSessionCoordinator,
) : ViewModel() {

    val gateState = coordinator.state

    init {
        viewModelScope.launch { coordinator.refresh() }
    }

    fun onAuthenticated() {
        coordinator.setAuthenticated()
    }

    fun onSignedOut() {
        coordinator.setUnauthenticated()
    }
}
