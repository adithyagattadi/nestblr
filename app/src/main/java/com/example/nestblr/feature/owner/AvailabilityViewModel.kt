package com.example.nestblr.feature.owner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.remote.dto.RoomOptionDto
import com.example.nestblr.data.repository.OwnerRepository
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

data class AvailabilityRoomItem(
    val roomId: String,
    val sharingType: String,
    val totalBeds: Int,
    val originalAvailable: Int,
    val currentAvailable: Int   // user can adjust via +/- buttons; clamped 0..totalBeds
)

data class AvailabilityState(
    val isLoading: Boolean = true,
    val rooms: List<AvailabilityRoomItem> = emptyList(),
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val hasChanges: Boolean
        get() = rooms.any { it.currentAvailable != it.originalAvailable }
}

@HiltViewModel(assistedFactory = AvailabilityViewModel.Factory::class)
class AvailabilityViewModel @AssistedInject constructor(
    @Assisted private val listingId: String,
    private val ownerRepo: OwnerRepository,
    private val api: NestBlrApi
) : ViewModel() {

    @AssistedFactory
    interface Factory {
        fun create(listingId: String): AvailabilityViewModel
    }

    private val _state = MutableStateFlow(AvailabilityState())
    val state: StateFlow<AvailabilityState> = _state.asStateFlow()

    /**
     * One-shot "all changes saved" signal. A Channel (not sticky state) so a
     * retained ViewModel — this one is scoped to the OwnerHome nav entry — can't
     * re-fire a stale dismiss when the sheet is reopened.
     */
    private val _saveComplete = Channel<Unit>(Channel.CONFLATED)
    val saveComplete = _saveComplete.receiveAsFlow()

    /** Called each time the sheet opens, so reopening always shows fresh data. */
    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            runCatching { api.getListingById(listingId).data.roomOptions }.fold(
                onSuccess = { rooms ->
                    _state.update {
                        it.copy(isLoading = false, rooms = rooms.map(::toItem))
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoading = false, error = e.message ?: "Couldn't load rooms")
                    }
                }
            )
        }
    }

    private fun toItem(dto: RoomOptionDto) = AvailabilityRoomItem(
        roomId = dto.id,
        sharingType = dto.sharingType,
        totalBeds = dto.totalBeds,
        originalAvailable = dto.availableBeds,
        currentAvailable = dto.availableBeds
    )

    fun increment(roomId: String) = adjust(roomId, +1)

    fun decrement(roomId: String) = adjust(roomId, -1)

    private fun adjust(roomId: String, delta: Int) {
        _state.update { state ->
            state.copy(
                rooms = state.rooms.map { room ->
                    if (room.roomId == roomId) {
                        room.copy(
                            currentAvailable =
                                (room.currentAvailable + delta).coerceIn(0, room.totalBeds)
                        )
                    } else {
                        room
                    }
                }
            )
        }
    }

    fun save() {
        if (_state.value.isSaving) return
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, error = null) }
            val changed = _state.value.rooms.filter { it.currentAvailable != it.originalAvailable }
            for (room in changed) {
                val result = ownerRepo.updateRoomAvailability(
                    listingId = listingId,
                    roomId = room.roomId,
                    availableBeds = room.currentAvailable
                )
                result.fold(
                    onSuccess = { updated ->
                        // Persist the saved value so a later partial-failure can't re-save it.
                        _state.update { state ->
                            state.copy(
                                rooms = state.rooms.map {
                                    if (it.roomId == room.roomId) {
                                        it.copy(originalAvailable = updated.availableBeds)
                                    } else it
                                }
                            )
                        }
                    },
                    onFailure = { e ->
                        // Stop on first failure; rooms saved before this stay saved.
                        _state.update {
                            it.copy(isSaving = false, error = e.message ?: "Save failed")
                        }
                        return@launch
                    }
                )
            }
            _state.update { it.copy(isSaving = false) }
            _saveComplete.send(Unit)
        }
    }
}
