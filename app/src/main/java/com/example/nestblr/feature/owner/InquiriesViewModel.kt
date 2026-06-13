package com.example.nestblr.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.remote.dto.InquirySummaryDto
import com.example.nestblr.data.repository.InquiriesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class InquiriesViewModel @Inject constructor(
    private val repo: InquiriesRepository
) : ViewModel() {

    data class State(
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val items: List<InquirySummaryDto> = emptyList(),
        val error: String? = null
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            repo.getSummary().fold(
                onSuccess = { list -> _state.update { State(isLoading = false, items = list) } },
                onFailure = { e -> _state.update { State(isLoading = false, error = e.message ?: "Failed to load") } }
            )
        }
    }

    /** Pull-to-refresh: reloads while keeping the current list visible
     *  (gesture indicator via isRefreshing, not the full-screen spinner). */
    fun refresh() {
        viewModelScope.launch {
            _state.update { it.copy(isRefreshing = true, error = null) }
            repo.getSummary().fold(
                onSuccess = { list -> _state.update { it.copy(isRefreshing = false, items = list, error = null) } },
                onFailure = { e -> _state.update { it.copy(isRefreshing = false, error = e.message ?: "Failed to refresh") } }
            )
        }
    }
}
