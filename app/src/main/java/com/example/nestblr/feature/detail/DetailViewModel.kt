package com.example.nestblr.feature.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.nestblr.core.navigation.Route
import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.repository.FavoritesRepository
import com.example.nestblr.data.repository.ListingRepository
import com.example.nestblr.domain.model.ListingDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DetailUiState(
    val isLoading: Boolean = true,
    val detail: ListingDetail? = null,
    val error: String? = null,
    // Transient favorite-toggle failure — shown as a snackbar, not the error screen.
    val favoriteError: String? = null,
    // Whether the signed-in user is a tenant (gates the "Write a review" button).
    val isTenant: Boolean = false,
    // Backend user id of the signed-in user — used to find their own review in the
    // loaded list so "Edit your review" survives an app restart.
    val currentUserId: String? = null
)

@HiltViewModel
class DetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: ListingRepository,
    private val favoritesRepo: FavoritesRepository,
    private val api: NestBlrApi
) : ViewModel() {

    // Extract listing ID from the navigation route — type-safe
    private val listingId: String = savedStateHandle.toRoute<Route.Detail>().listingId

    private val _state = MutableStateFlow(DetailUiState())
    val state: StateFlow<DetailUiState> = _state.asStateFlow()

    init {
        load()
        resolveRole()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.getById(listingId).fold(
                onSuccess = { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail, error = null) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Unknown error")
                    }
                }
            )
        }
    }

    /** Reviews are tenant-only; resolve role once to gate the review affordance.
     *  Also capture the user id to detect their own review across restarts.
     *  On failure, fall back to showing the button (the backend still enforces it). */
    private fun resolveRole() {
        viewModelScope.launch {
            runCatching { api.getMe().data }.fold(
                onSuccess = { user ->
                    _state.update { it.copy(isTenant = user.role == "TENANT", currentUserId = user.id) }
                },
                onFailure = {
                    _state.update { it.copy(isTenant = true) }
                }
            )
        }
    }

    /** Toggle favorite for the loaded listing; flip isFavorite on success. */
    fun toggleFavorite() {
        val current = _state.value.detail ?: return
        viewModelScope.launch {
            val result = if (current.isFavorite) favoritesRepo.remove(current.id)
                         else favoritesRepo.add(current.id)
            result.fold(
                onSuccess = {
                    _state.update { s ->
                        s.copy(detail = s.detail?.copy(isFavorite = !current.isFavorite))
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(favoriteError = e.message ?: "Couldn't update favorite")
                    }
                }
            )
        }
    }

    fun clearFavoriteError() {
        _state.update { it.copy(favoriteError = null) }
    }
}