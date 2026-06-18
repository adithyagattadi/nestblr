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
import java.time.LocalDate
import java.time.Period
import java.time.format.DateTimeFormatter
import javax.inject.Inject

enum class AuthMode { LOGIN, SIGNUP }

enum class Gender(val display: String, val backendValue: String) {
    Male("Male", "MALE"),
    Female("Female", "FEMALE"),
    Other("Other", "OTHER"),
    PreferNotToSay("Prefer not to say", "PREFER_NOT_TO_SAY")
}

data class AuthUiState(
    val mode: AuthMode = AuthMode.LOGIN,
    val email: String = "",
    val password: String = "",
    val role: String = "TENANT",        // chosen at signup
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAuthenticated: Boolean = false,

    // Signup-only fields
    val fullName: String = "",
    val phone: String = "",
    val confirmPassword: String = "",
    val dob: LocalDate? = null,
    val gender: Gender = Gender.PreferNotToSay,

    // Per-field validation errors (null = no error)
    val fullNameError: String? = null,
    val phoneError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val dobError: String? = null
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

    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null, emailError = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null, passwordError = null) }
    fun onRoleChange(v: String) = _state.update { it.copy(role = v) }

    fun onFullNameChange(v: String) = _state.update { it.copy(fullName = v, fullNameError = null) }
    fun onPhoneChange(v: String) = _state.update { it.copy(phone = v, phoneError = null) }
    fun onConfirmPasswordChange(v: String) =
        _state.update { it.copy(confirmPassword = v, confirmPasswordError = null) }
    fun onDobChange(v: LocalDate) = _state.update { it.copy(dob = v, dobError = null) }
    fun onGenderChange(v: Gender) = _state.update { it.copy(gender = v) }

    fun toggleMode() = _state.update {
        it.copy(
            mode = if (it.mode == AuthMode.LOGIN) AuthMode.SIGNUP else AuthMode.LOGIN,
            error = null
        )
    }

    /**
     * Validates all signup fields, mirroring the backend's checks so the user gets
     * immediate feedback. Sets per-field error strings and returns true iff all pass.
     */
    fun validateSignup(): Boolean {
        val errors = mutableMapOf<String, String>()
        val s = _state.value

        if (s.fullName.trim().isBlank()) {
            errors["fullName"] = "Please enter your name"
        }

        if (!s.phone.matches(Regex("^(\\+91)?[6-9]\\d{9}$"))) {
            errors["phone"] = "Enter a valid 10-digit Indian mobile number"
        }

        if (!s.email.contains("@") || !s.email.contains(".")) {
            errors["email"] = "Enter a valid email address"
        }

        val pwd = s.password
        if (pwd.length < 8 || !pwd.any { it.isLetter() } || !pwd.any { it.isDigit() }) {
            errors["password"] = "Password must be at least 8 characters with a letter and a digit"
        }

        if (s.confirmPassword != s.password) {
            errors["confirmPassword"] = "Passwords don't match"
        }

        val dob = s.dob
        if (dob == null) {
            errors["dob"] = "Please select your date of birth"
        } else {
            val age = Period.between(dob, LocalDate.now()).years
            if (age < 18) errors["dob"] = "You must be 18 or older"
        }

        // Gender has a default, so it never fails validation.

        _state.update {
            it.copy(
                fullNameError = errors["fullName"],
                phoneError = errors["phone"],
                emailError = errors["email"],
                passwordError = errors["password"],
                confirmPasswordError = errors["confirmPassword"],
                dobError = errors["dob"]
            )
        }

        return errors.isEmpty()
    }

    fun submit() {
        val s = _state.value

        if (s.mode == AuthMode.SIGNUP) {
            if (!validateSignup()) return
        } else {
            if (s.email.isBlank() || s.password.length < 6) {
                _state.update { it.copy(error = "Enter a valid email and 6+ char password") }
                return
            }
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
                    // On signup, register the user in our backend with their details.
                    // On login, ensure they're registered (idempotent).
                    val reg = runCatching {
                        api.register(
                            if (s.mode == AuthMode.SIGNUP) {
                                RegisterRequest(
                                    role = s.role,
                                    fullName = s.fullName.trim(),
                                    phone = s.phone,
                                    gender = s.gender.backendValue,
                                    dob = s.dob?.format(DateTimeFormatter.ISO_LOCAL_DATE)
                                )
                            } else {
                                RegisterRequest(role = "TENANT")
                            }
                        )
                    }
                    reg.fold(
                        onSuccess = {
                            _state.update { it.copy(isLoading = false, isAuthenticated = true) }
                        },
                        onFailure = {
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
