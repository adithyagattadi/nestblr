package com.example.nestblr.feature.auth

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.remote.NestBlrApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.hilt.navigation.compose.hiltViewModel
import javax.inject.Inject

sealed interface RoleGateState {
    data object Loading : RoleGateState
    data class Resolved(val role: String) : RoleGateState
    data class Error(val message: String) : RoleGateState
}

@HiltViewModel
class RoleGateViewModel @Inject constructor(
    private val api: NestBlrApi
) : ViewModel() {

    private val _state = MutableStateFlow<RoleGateState>(RoleGateState.Loading)
    val state: StateFlow<RoleGateState> = _state.asStateFlow()

    init {
        resolve()
    }

    fun resolve() {
        _state.value = RoleGateState.Loading
        viewModelScope.launch {
            runCatching { api.getMe().data }.fold(
                onSuccess = { user -> _state.value = RoleGateState.Resolved(user.role) },
                onFailure = { e ->
                    // If /me fails (e.g. user row missing), default to TENANT view
                    _state.value = RoleGateState.Error(e.message ?: "Couldn't load profile")
                }
            )
        }
    }
}

/**
 * Shown briefly after login. Fetches the user's role from the backend,
 * then calls onResolved with "TENANT" or "OWNER" so navigation can route.
 * On error, defaults to TENANT (safe fallback — owner features stay gated server-side).
 */
@Composable
fun RoleGateScreen(
    onResolved: (String) -> Unit,
    viewModel: RoleGateViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        when (val s = state) {
            is RoleGateState.Resolved -> onResolved(s.role)
            is RoleGateState.Error -> onResolved("TENANT")  // safe fallback
            RoleGateState.Loading -> Unit
        }
    }

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}