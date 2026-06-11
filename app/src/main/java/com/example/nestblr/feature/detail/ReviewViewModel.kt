package com.example.nestblr.feature.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.remote.dto.ReviewDto
import com.example.nestblr.data.repository.ReviewsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel(assistedFactory = ReviewViewModel.Factory::class)
class ReviewViewModel @AssistedInject constructor(
    @Assisted private val listingId: String,
    private val reviewsRepo: ReviewsRepository
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(listingId: String): ReviewViewModel
    }

    data class State(
        val rating: Int = 0,
        val comment: String = "",
        val isSubmitting: Boolean = false,
        val isDeleting: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    /**
     * One-shot "done" signal — carries the submitted [ReviewDto] (so the detail
     * screen can recognise the user's review for in-session edit mode) or null on
     * delete. CONFLATED + receiveAsFlow, the same pattern as AvailabilityViewModel,
     * so a retained ViewModel can't re-fire a stale dismiss when reopened.
     */
    private val _submitComplete = Channel<ReviewDto?>(Channel.CONFLATED)
    val submitComplete = _submitComplete.receiveAsFlow()

    /** Resets the editable state each time the sheet opens (VM is retained). */
    fun setInitial(rating: Int, comment: String) {
        _state.value = State(rating = rating, comment = comment)
    }

    fun onRatingChange(r: Int) {
        _state.update { it.copy(rating = r, error = null) }
    }

    fun onCommentChange(c: String) {
        if (c.length <= MAX_COMMENT) _state.update { it.copy(comment = c, error = null) }
    }

    fun submit() {
        val s = _state.value
        if (s.isSubmitting || s.isDeleting) return
        if (s.rating == 0) {
            _state.update { it.copy(error = "Please select a rating") }; return
        }
        if (s.comment.isBlank()) {
            _state.update { it.copy(error = "Please add a comment") }; return
        }
        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            reviewsRepo.submitReview(listingId, s.rating, s.comment.trim()).fold(
                onSuccess = { review -> _submitComplete.send(review) },
                onFailure = { e ->
                    _state.update { it.copy(isSubmitting = false, error = e.message ?: "Submit failed") }
                }
            )
        }
    }

    fun delete() {
        if (_state.value.isDeleting) return
        viewModelScope.launch {
            _state.update { it.copy(isDeleting = true, error = null) }
            reviewsRepo.deleteMyReview(listingId).fold(
                onSuccess = { _submitComplete.send(null) },
                onFailure = { e ->
                    _state.update { it.copy(isDeleting = false, error = e.message ?: "Delete failed") }
                }
            )
        }
    }

    companion object {
        const val MAX_COMMENT = 1000
    }
}
