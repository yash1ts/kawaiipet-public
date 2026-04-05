package com.kawaiipet.app.overlay

sealed class OverlayState {
    data object Idle : OverlayState()
    /** Waiting for downloaded Sherpa STT to finish loading before opening the mic. */
    data object PreparingVoice : OverlayState()
    data object Listening : OverlayState()
    data class Processing(val userText: String) : OverlayState()
    data class Speaking(val responseText: String) : OverlayState()
    data object TextInput : OverlayState()
    data object Minimized : OverlayState()
}
