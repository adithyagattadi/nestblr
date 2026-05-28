package com.example.nestblr.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.auth.AuthRepository
import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.remote.dto.RegisterRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthMode { LOGIN, SIGNUP }

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val role: String = "TENANT",        // chosen at signup
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false
)

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val authRepo: AuthRepository,
    private val api: NestBlrApi
) : ViewModel() {

    private val _state = MutableStateFlow(
        AuthUiState(isAuthenticated = authRepo.isLoggedIn)
    )
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onRoleChange(v: String) = _state.update { it.copy(role = v) }
    fun toggleMode() = _state.update {
        it.copy(
            mode = if (it.mode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN,
            error = null
        )
    }

    fun submit() {
        val s = _state.value
        if (s.email.isBlank() || s.password.length < 6) {
            _state.update { it.copy(error = "Enter a valid email and 6+ char password") }
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val authResult = if (s.mode == AuthMode.SIGNUP) {
                authRepo.signUp(s.email, s.password)
            } else {
                authRepo.signIn(s.email, s.password)
            }

            authResult.fold(
                onSuccess = {
                    // On signup, register the user in our backend with their role.
                    // On login, ensure they're registered (idempotent).
                    val reg = runCatching {
                        api.register(
                            RegisterRequest(
                                role = if (s.mode == AuthMode.SIGNUP) s.role else "TENANT",
                                fullName = null,
                                phone = null
                            )
                        )
                    }
                    reg.fold(
                        onSuccess = {
                            _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                        },
                        onFailure = { e ->
                            // Auth succeeded but backend registration failed.
                            // Still let them in — backend /me will retry registration flow.
                            _state.update {
                                it.copy(isLoading = false, isAuthenticated = true)
                            }
                        }
                    )
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = mapAuthError(e.message)
                        )
                    }
                }
            )
        }
    }

    fun signOut() {
        authRepo.signOut()
        _state.update { AuthUiState(isAuthenticated = false) }
    }

    private fun mapAuthError(raw: String?): String = when {
        raw == null -> "Something went wrong"
        raw.contains("password is invalid", true) -> "Wrong password"
        raw.contains("no user record", true) -> "No account with this email"
        raw.contains("email address is already", true) -> "Email already registered — try logging in"
        raw.contains("badly formatted", true) -> "Invalid email format"
        raw.contains("network", true) -> "Network error — check your connection"
        else -> "Login failed. Please try again."
    }
}