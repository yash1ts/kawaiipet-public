package com.kawaiipet.app.ui.auth

import android.util.Patterns

object AuthInputValidation {
    fun isValidEmail(email: String): Boolean {
        val t = email.trim()
        return t.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(t).matches()
    }
}
