package com.budgetty.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.budgetty.app.data.repository.AuthRepository
import com.budgetty.app.data.settings.SettingsStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** Top-level authentication state used to gate the app. */
sealed interface AuthState {
    data object Loading : AuthState
    data object SignedOut : AuthState
    data class SignedIn(val email: String?) : AuthState
}

class AuthViewModel(
    private val repository: AuthRepository,
    private val settingsStore: SettingsStore,
) : ViewModel() {

    val authState: StateFlow<AuthState> = repository.currentUser
        .map { user -> if (user == null) AuthState.SignedOut else AuthState.SignedIn(user.email) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AuthState.Loading)

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun signIn(email: String, password: String) =
        launchAuth("Sign-in failed") { repository.signInEmail(email.trim(), password) }

    fun signUp(email: String, password: String) =
        launchAuth("Sign-up failed") {
            // Arm the one-time Insights setup quiz before the auth state flips to SignedIn, so the
            // quiz gate is already pending when the main gate recomposes (no Home flash). A failed
            // sign-up disarms it again.
            settingsStore.setInsightsQuizPending(true)
            try {
                repository.signUpEmail(email.trim(), password)
            } catch (e: Exception) {
                settingsStore.setInsightsQuizPending(false)
                throw e
            }
        }

    fun signInWithGoogle(idToken: String) =
        launchAuth("Google sign-in failed") { repository.signInWithGoogle(idToken) }

    fun signOut() = repository.signOut()

    /** Sends a password-reset email; [onSent] fires on success. Errors surface via [error]. */
    fun resetPassword(email: String, onSent: () -> Unit) {
        if (email.isBlank()) {
            _error.value = "Enter your email to reset your password"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                repository.sendPasswordReset(email.trim())
                onSent()
            } catch (e: Exception) {
                _error.value = e.message ?: "Couldn't send reset email"
            } finally {
                _loading.value = false
            }
        }
    }

    fun setError(message: String?) {
        _error.value = message
    }

    private fun launchAuth(failureMessage: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _loading.value = true
            _error.value = null
            try {
                block()
            } catch (e: Exception) {
                _error.value = e.message ?: failureMessage
            } finally {
                _loading.value = false
            }
        }
    }
}
