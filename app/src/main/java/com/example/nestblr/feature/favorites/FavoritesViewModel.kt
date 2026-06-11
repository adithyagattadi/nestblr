package com.example.nestblr.feature.favorites

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.repository.FavoritesRepository
import com.example.nestblr.domain.model.ListingSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class FavoritesState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val listings: List<ListingSummary> = emptyList(),
    val error: String? = null,
    // Transient un-favorite failure — shown as a snackbar.
    val favoriteError: String? = null
)

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val favoritesRepo: FavoritesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(FavoritesState())
    val state: StateFlow<FavoritesState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            favoritesRepo.list().fold(
                onSuccess = { list ->
                    _state.update { it.copy(isLoading = false, listings = list, error = null) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't load favorites")
                    }
                }
            )
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            favoritesRepo.list().fold(
                onSuccess = { list ->
                    _state.update { it.copy(isRefreshing = false, listings = list, error = null) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isRefreshing = false, error = e.message ?: "Couldn't load favorites")
                    }
                }
            )
        }
    }

    /**
     * Everything on this screen is favorited, so toggling always un-favorites.
     * On success the row is removed immediately (no confirmation — re-add via search).
     */
    fun toggleFavorite(listingId: String, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            favoritesRepo.remove(listingId).fold(
                onSuccess = {
                    _state.update { s ->
                        s.copy(listings = s.listings.filterNot { it.id == listingId })
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
