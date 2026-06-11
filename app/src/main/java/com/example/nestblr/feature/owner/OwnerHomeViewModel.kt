package com.example.nestblr.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.remote.dto.OwnerListingDto
import com.example.nestblr.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OwnerHomeState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val listings: List<OwnerListingDto> = emptyList(),
    val error: String? = null
)

@HiltViewModel
class OwnerHomeViewModel @Inject constructor(
    private val repo: OwnerRepository
) : ViewModel() {

    private val _state = MutableStateFlow(OwnerHomeState())
    val state: StateFlow<OwnerHomeState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.getMyListings().fold(
                onSuccess = { list ->
                    _state.update { OwnerHomeState(isLoading = false, listings = list) }
                },
                onFailure = { e ->
                    _state.update {
                        OwnerHomeState(isLoading = false, error = e.message ?: "Failed to load")
                    }
                }
            )
        }
    }

    /** Pull-to-refresh: reloads the listings while keeping the current list
     *  visible (gesture indicator via isRefreshing, not the full-screen spinner). */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            repo.getMyListings().fold(
                onSuccess = { list ->
                    _state.update {
                        it.copy(isRefreshing = false, listings = list, error = null)
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isRefreshing = false, error = e.message ?: "Failed to load")
                    }
                }
            )
        }
    }

    fun deleteListing(id: String) {
        viewModelScope.launch {
            repo.deleteListing(id).onSuccess { load() }
        }
    }
}