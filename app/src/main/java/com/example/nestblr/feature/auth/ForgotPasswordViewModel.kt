package com.example.nestblr.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.auth.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepo: AuthRepository
) : ViewModel() {

    data class State(
        val email: String = "",
        val isSending: Boolean = false,
        val message: String? = null, // shown via snackbar
        val isError: Boolean = false
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun onEmailChange(e: String) {
        _state.update { it.copy(email = e, message = null) }
    }

    fun sendResetEmail() {
        val email = _state.value.email.trim()
        if (email.isBlank() || !email.contains("@")) {
            _state.update { it.copy(message = "Please enter a valid email", isError = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSending = true, message = null) }
            authRepo.sendPasswordReset(email).fold(
                onSuccess = {
                    _state.update {
                        it.copy(
                            isSending = false,
                            message = "If that email is registered, we've sent a reset link. Check your inbox.",
                            isError = false
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isSending = false,
                            message = e.message ?: "Couldn't send reset email. Try again.",
                            isError = true
                        )
                    }
                }
            )
        }
    }

    fun clearMessage() {
        _state.update { it.copy(message = null) }
    }
}
