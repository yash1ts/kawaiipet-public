package com.kawaiipet.app.auth

import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.status.SessionStatus

/**
 * Ensures auth has left [SessionStatus.Initializing] so [Auth.currentSessionOrNull] is meaningful.
 *
 * On Android, [Auth] can be left in [SessionStatus.Initializing] after the process moves to the
 * background: lifecycle hooks stop auto-refresh and reset loading state, but the next storage load
 * only runs on process [onStart]. A foreground overlay with no resumed activity never gets that,
 * so [Auth.awaitInitialization] would hang indefinitely.
 */
suspend fun Auth.awaitReady() {
    when (sessionStatus.value) {
        is SessionStatus.Initializing -> {
            if (!loadFromStorage()) {
                awaitInitialization()
            }
        }
        else -> awaitInitialization()
    }
}
