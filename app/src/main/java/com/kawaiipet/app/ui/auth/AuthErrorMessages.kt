package com.kawaiipet.app.ui.auth

import android.content.res.Resources
import com.kawaiipet.app.R

/**
 * Turns Supabase / network throwables into short, user-facing copy (no exception class dumps).
 */
fun Throwable.toAuthUserMessage(resources: Resources): String {
    val raw = message?.trim().orEmpty()
    if (raw.isEmpty()) return resources.getString(R.string.auth_error_generic)

    val lower = raw.lowercase()
    return when {
        lower.contains("invalid login") ||
            lower.contains("invalid email or password") ||
            lower.contains("invalid credentials") -> resources.getString(R.string.auth_error_invalid_credentials)

        lower.contains("already registered") ||
            lower.contains("user already registered") ||
            lower.contains("email address is already") -> resources.getString(R.string.auth_error_email_in_use)

        lower.contains("email not confirmed") ||
            lower.contains("confirm your email") -> resources.getString(R.string.auth_error_confirm_email)

        lower.contains("network") ||
            lower.contains("unable to resolve host") ||
            lower.contains("failed to connect") ||
            lower.contains("timeout") ||
            lower.contains("connection") && lower.contains("refused") ->
            resources.getString(R.string.auth_error_network)

        else -> raw.lineSequence().first().trim().take(MAX_LEN)
    }
}

private const val MAX_LEN = 280
