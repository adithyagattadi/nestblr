package com.example.nestblr.feature.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
    val listings: List<ListingSummary> = emptyList(),
    val error: String? = null,
    val centerLat: Double = 12.9352,  // Default: Koramangala
    val centerLng: Double = 77.6245,
    val radiusKm: Double = 5.0,
    val filters: FilterState = FilterState()
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repo: ListingRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SearchUiState())
    val state: StateFlow<SearchUiState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            val f = _state.value.filters
            val result = repo.searchNearby(
                lat = _state.value.centerLat,
                lng = _state.value.centerLng,
                radiusKm = _state.value.radiusKm,
                gender = f.gender,
                food = f.food,
                pgType = f.pgType,
                minRent = if (f.minRent != FilterState.MIN_RENT) f.minRent else null,
                maxRent = if (f.maxRent != FilterState.MAX_RENT) f.maxRent else null
            )
            result.fold(
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

    fun applyFilters(filters: FilterState) {
        _state.update { it.copy(filters = filters) }
        load()
    }

    fun clearFilters() {
        applyFilters(FilterState())
    }
}