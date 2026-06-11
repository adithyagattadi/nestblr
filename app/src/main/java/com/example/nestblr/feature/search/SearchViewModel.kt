package com.example.nestblr.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.core.model.Locality
import com.example.nestblr.data.repository.FavoritesRepository
import com.example.nestblr.data.repository.ListingRepository
import com.example.nestblr.domain.model.ListingSummary
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SearchUiState(
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val listings: List<ListingSummary> = emptyList(),
    val error: String? = null,
    // Single source of truth for the map/list center: the picked locality.
    val selectedLocality: Locality = Locality.KORAMANGALA,
    val centerLat: Double = selectedLocality.lat,
    val centerLng: Double = selectedLocality.lng,
    val radiusKm: Double = 5.0,
    val filters: FilterState = FilterState(),
    // Transient one-off message (favorite toggle failure) — shown as a snackbar,
    // never the full-screen error state.
    val favoriteError: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: ListingRepository,
    private val favoritesRepo: FavoritesRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            doSearch().fold(
                onSuccess = { listings ->
                    _state.update {
                        it.copy(isLoading = false, listings = listings, error = null)
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Unknown error")
                    }
                }
            )
        }
    }

    /** Pull-to-refresh: same fetch as [load] but drives the gesture indicator
     *  (isRefreshing) instead of the full-screen spinner (isLoading). */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            doSearch().fold(
                onSuccess = { listings ->
                    _state.update {
                        it.copy(isRefreshing = false, listings = listings, error = null)
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isRefreshing = false, error = e.message ?: "Unknown error")
                    }
                }
            )
        }
    }

    private suspend fun doSearch() = run {
        val f = _state.value.filters
        repo.searchNearby(
            lat = _state.value.centerLat,
            lng = _state.value.centerLng,
            radiusKm = _state.value.radiusKm,
            gender = f.gender,
            food = f.food,
            pgType = f.pgType,
            minRent = if (f.minRent != FilterState.MIN_RENT) f.minRent else null,
            maxRent = if (f.maxRent != FilterState.MAX_RENT) f.maxRent else null
        )
    }

    /** Tenant picked a locality — recenter and reload both list and map. */
    fun onLocalityChanged(locality: Locality) {
        _state.update {
            it.copy(
                selectedLocality = locality,
                centerLat = locality.lat,
                centerLng = locality.lng,
                // Drop stale results so the area transitions through a clean
                // loading state instead of showing the old locality's data.
                listings = emptyList(),
                isLoading = true
            )
        }
        load()
    }

    /**
     * Toggle favorite for one listing. Sequential (not optimistic): call the API,
     * then flip isFavorite on the matching row so the heart re-renders without a
     * full search round-trip. Failures surface as a transient snackbar.
     */
    fun toggleFavorite(listingId: String, currentlyFavorite: Boolean) {
        viewModelScope.launch {
            val result = if (currentlyFavorite) favoritesRepo.remove(listingId)
                         else favoritesRepo.add(listingId)
            result.fold(
                onSuccess = {
                    _state.update { s ->
                        s.copy(
                            listings = s.listings.map {
                                if (it.id == listingId) it.copy(isFavorite = !currentlyFavorite)
                                else it
                            }
                        )
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

    fun applyFilters(filters: FilterState) {
        _state.update { it.copy(filters = filters) }
        load()
    }

    fun clearFilters() {
        applyFilters(FilterState())
    }
}