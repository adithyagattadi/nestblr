package com.example.nestblr.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.core.location.LocationService
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
    // The picked locality, or null when centering on the user's current location.
    val selectedLocality: Locality? = Locality.KORAMANGALA,
    // True while centered on the device location rather than a locality.
    val isUsingCurrentLocation: Boolean = false,
    // FAB spinner: a one-shot location fetch is in flight.
    val isFetchingLocation: Boolean = false,
    // Transient location-fetch failure — shown as a snackbar.
    val locationError: String? = null,
    // Map/list center. Derives from selectedLocality, or from the device fix
    // when isUsingCurrentLocation.
    val centerLat: Double = Locality.KORAMANGALA.lat,
    val centerLng: Double = Locality.KORAMANGALA.lng,
    // Header display name: a locality name, or "you" for the current location.
    val locationName: String = Locality.KORAMANGALA.displayName,
    val radiusKm: Double = 5.0,
    val filters: FilterState = FilterState(),
    // Transient one-off message (favorite toggle failure) — shown as a snackbar,
    // never the full-screen error state.
    val favoriteError: String? = null
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: ListingRepository,
    private val favoritesRepo: FavoritesRepository,
    private val locationService: LocationService
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

    /** Tenant picked a locality — recenter and reload both list and map.
     *  Also clears any current-location state so the picker reflects the locality. */
    fun onLocalityChanged(locality: Locality) {
        _state.update {
            it.copy(
                selectedLocality = locality,
                isUsingCurrentLocation = false,
                locationName = locality.displayName,
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
     * Center the search on the device's current location. The caller (UI) must
     * have already obtained ACCESS_COARSE_LOCATION. A null fix (timeout/failure)
     * surfaces as a transient [locationError]; the existing locality stays put.
     */
    fun useCurrentLocation() {
        viewModelScope.launch {
            _state.update { it.copy(isFetchingLocation = true, locationError = null) }
            val coords = locationService.getCurrentLocation()
            if (coords == null) {
                _state.update {
                    it.copy(
                        isFetchingLocation = false,
                        locationError = "Couldn't get your location. Try again."
                    )
                }
                return@launch
            }
            _state.update {
                it.copy(
                    isFetchingLocation = false,
                    isUsingCurrentLocation = true,
                    selectedLocality = null,
                    centerLat = coords.first,
                    centerLng = coords.second,
                    locationName = "you"
                )
            }
            // Reuse the standard search runner with the new center.
            load()
        }
    }

    fun clearLocationError() {
        _state.update { it.copy(locationError = null) }
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