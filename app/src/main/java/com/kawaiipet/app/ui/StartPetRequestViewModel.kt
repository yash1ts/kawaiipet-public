package com.kawaiipet.app.ui

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@HiltViewModel
class StartPetRequestViewModel @Inject constructor() : ViewModel() {

    private val _startPetRequested = MutableStateFlow(false)
    val startPetRequested: StateFlow<Boolean> = _startPetRequested.asStateFlow()

    fun setStartPetRequested(value: Boolean) {
        _startPetRequested.value = value
    }

    fun consumeStartPetRequest() {
        _startPetRequested.value = false
    }
}
