package com.kawaiipet.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kawaiipet.app.BuildConfig
import com.kawaiipet.app.supabase.ProfileRepository
import com.kawaiipet.app.util.PreferenceManager
import com.kawaiipet.app.util.Analytics
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AuthEmailViewModel @Inject constructor(
    private val supabase: SupabaseClient,
    private val profileRepository: ProfileRepository,
    private val prefs: PreferenceManager,
) : ViewModel() {

    /** Must exactly match an entry under Supabase → Authentication → URL Configuration → Redirect URLs. */
    private fun authEmailRedirectUrl(): String =
        "${BuildConfig.SUPABASE_AUTH_REDIRECT_SCHEME}://${BuildConfig.SUPABASE_AUTH_REDIRECT_HOST}"

    private suspend fun syncProfileFromServer() {
        profileRepository.fetchProfile().onSuccess { (name, personality) ->
            name?.takeIf { it.isNotBlank() }?.let { prefs.setPetName(it) }
            personality?.takeIf { it.isNotBlank() }?.let { prefs.setPersonalityPrompt(it) }
        }
    }

    fun signIn(email: String, password: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                supabase.auth.signInWith(Email) {
                    this.email = email.trim()
                    this.password = password
                }
                syncProfileFromServer()
                val userId = supabase.auth.currentUserOrNull()?.id
                if (userId != null) {
                    Analytics.identify(
                        distinctId = userId,
                        userProperties = mapOf("email" to email.trim()),
                    )
                }
                Analytics.capture(event = "user signed in", properties = mapOf("method" to "email"))
            }
            withContext(Dispatchers.Main.immediate) { onResult(result) }
        }
    }

    /**
     * Returns [Result] success with `true` if session is active (entered app), `false` if email verification needed.
     */
    fun signUp(email: String, password: String, onResult: (Result<Boolean>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                supabase.auth.signUpWith(Email, redirectUrl = authEmailRedirectUrl()) {
                    this.email = email.trim()
                    this.password = password
                }
                val session = supabase.auth.currentSessionOrNull() != null
                if (session) {
                    syncProfileFromServer()
                    val userId = supabase.auth.currentUserOrNull()?.id
                    if (userId != null) {
                        Analytics.identify(
                            distinctId = userId,
                            userProperties = mapOf("email" to email.trim()),
                            userPropertiesSetOnce = mapOf("sign_up_date" to System.currentTimeMillis()),
                        )
                    }
                }
                Analytics.capture(
                    event = "user signed up",
                    properties = mapOf("method" to "email", "has_session" to session),
                )
                session
            }
            withContext(Dispatchers.Main.immediate) { onResult(result) }
        }
    }

    /**
     * Resends the signup confirmation email. Supabase may return success even if the address is unknown (anti-enumeration).
     */
    fun resendSignupConfirmation(email: String, onResult: (Result<Unit>) -> Unit) {
        viewModelScope.launch {
            val result = runCatching {
                supabase.auth.resendEmail(OtpType.Email.SIGNUP, email.trim())
            }
            withContext(Dispatchers.Main.immediate) { onResult(result) }
        }
    }
}
