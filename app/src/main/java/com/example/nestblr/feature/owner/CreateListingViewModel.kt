package com.example.nestblr.feature.owner

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.example.nestblr.core.navigation.Route
import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.remote.dto.CreateListingRequest
import com.example.nestblr.data.remote.dto.CreateRoomOptionRequest
import com.example.nestblr.data.repository.OwnerRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** A room option row in the form (strings for text-field binding). */
data class RoomOptionForm(
    val sharingType: String = "SINGLE",
    val monthlyRent: String = "",
    val securityDeposit: String = "",
    val totalBeds: String = "",
    val availableBeds: String = ""
)

/**
 * Bengaluru localities with approximate center coordinates.
 * Temporary stand-in for the map pin picker — owner picks a locality,
 * we use its centroid. Later replaced by an exact map pin.
 */
enum class Locality(val displayName: String, val lat: Double, val lng: Double) {
    KORAMANGALA("Koramangala", 12.9352, 77.6245),
    HSR_LAYOUT("HSR Layout", 12.9116, 77.6412),
    BTM_LAYOUT("BTM Layout", 12.9165, 77.6101),
    INDIRANAGAR("Indiranagar", 12.9716, 77.6412),
    JAYANAGAR("Jayanagar", 12.9250, 77.5938),
    WHITEFIELD("Whitefield", 12.9698, 77.7500),
    MARATHAHALLI("Marathahalli", 12.9591, 77.6974),
    BELLANDUR("Bellandur", 12.9259, 77.6649),
    ELECTRONIC_CITY("Electronic City", 12.8452, 77.6602),
    JP_NAGAR("JP Nagar", 12.9077, 77.5853),
    SARJAPUR("Sarjapur Road", 12.9010, 77.6870)
}

data class CreateListingState(
    val title: String = "",
    val description: String = "",
    val addressLine: String = "",
    val locality: Locality = Locality.KORAMANGALA,
    val pincode: String = "",
    val contactPhone: String = "",
    val genderPreference: String = "COED",
    val pgType: String = "PG",
    val foodType: String = "BOTH",
    val rooms: List<RoomOptionForm> = listOf(RoomOptionForm()),
    val selectedAmenityIds: Set<Int> = emptySet(),
    val isSubmitting: Boolean = false,
    val isLoadingExisting: Boolean = false,  // true while pre-filling the form in edit mode
    val error: String? = null,
    val createdId: String? = null   // non-null = success, triggers navigation back
)

/** Amenities — must match the seeded amenity IDs in the backend (01_schema.sql order). */
val AMENITIES: List<Pair<Int, String>> = listOf(
    1 to "WiFi",
    2 to "AC",
    3 to "Laundry",
    4 to "Parking",
    5 to "Power Backup",
    6 to "CCTV",
    7 to "Hot Water",
    8 to "Refrigerator",
    9 to "Housekeeping",
    10 to "TV",
    11 to "Gym",
    12 to "Food Included"
)

@HiltViewModel
class CreateListingViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repo: OwnerRepository,
    private val api: NestBlrApi
) : ViewModel() {

    private val _state = MutableStateFlow(CreateListingState())
    val state: StateFlow<CreateListingState> = _state.asStateFlow()

    /**
     * Non-null only when this VM backs Route.EditListing. The CreateListing route
     * carries no args, so toRoute<EditListing>() throws there — we treat that as create mode.
     */
    val editingListingId: String? = try {
        savedStateHandle.toRoute<Route.EditListing>().listingId
    } catch (e: Exception) {
        null
    }

    init {
        editingListingId?.let { loadForEdit(it) }
    }

    /** Fetch the listing via the public detail endpoint and map it back into form state. */
    private fun loadForEdit(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(isLoadingExisting = true, error = null) }
            runCatching { api.getListingById(id).data }.fold(
                onSuccess = { dto ->
                    _state.update {
                        it.copy(
                            isLoadingExisting = false,
                            title = dto.title,
                            description = dto.description ?: "",
                            addressLine = dto.addressLine,
                            // Backend returns the locality as a display string; map back to the enum.
                            locality = Locality.entries.firstOrNull { l -> l.displayName == dto.locality }
                                ?: Locality.KORAMANGALA,
                            pincode = dto.pincode ?: "",
                            // Listing-level contact phone (owner.phone is a placeholder for email-auth users).
                            contactPhone = dto.contactPhone ?: "",
                            genderPreference = dto.genderPreference,
                            pgType = dto.pgType,
                            foodType = dto.foodType,
                            // RoomOptionDto has Int fields; the form binds to Strings.
                            rooms = dto.roomOptions.map { r ->
                                RoomOptionForm(
                                    sharingType = r.sharingType,
                                    monthlyRent = r.monthlyRent.toString(),
                                    securityDeposit = r.securityDeposit.toString(),
                                    totalBeds = r.totalBeds.toString(),
                                    availableBeds = r.availableBeds.toString()
                                )
                            }.ifEmpty { listOf(RoomOptionForm()) },
                            selectedAmenityIds = dto.amenities.map { a -> a.id }.toSet()
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(isLoadingExisting = false, error = e.message ?: "Failed to load listing")
                    }
                }
            )
        }
    }

    fun onTitle(v: String) = _state.update { it.copy(title = v, error = null) }
    fun onDescription(v: String) = _state.update { it.copy(description = v) }
    fun onAddress(v: String) = _state.update { it.copy(addressLine = v, error = null) }
    fun onLocality(v: Locality) = _state.update { it.copy(locality = v) }
    fun onPincode(v: String) = _state.update { it.copy(pincode = v) }
    fun onContactPhone(v: String) = _state.update { it.copy(contactPhone = v, error = null) }
    fun onGender(v: String) = _state.update { it.copy(genderPreference = v) }
    fun onPgType(v: String) = _state.update { it.copy(pgType = v) }
    fun onFoodType(v: String) = _state.update { it.copy(foodType = v) }

    fun toggleAmenity(id: Int) = _state.update {
        val next = it.selectedAmenityIds.toMutableSet()
        if (id in next) next.remove(id) else next.add(id)
        it.copy(selectedAmenityIds = next)
    }

    fun addRoom() = _state.update { it.copy(rooms = it.rooms + RoomOptionForm()) }

    fun removeRoom(index: Int) = _state.update {
        if (it.rooms.size <= 1) it  // keep at least one
        else it.copy(rooms = it.rooms.filterIndexed { i, _ -> i != index })
    }

    fun updateRoom(index: Int, room: RoomOptionForm) = _state.update {
        it.copy(rooms = it.rooms.mapIndexed { i, r -> if (i == index) room else r })
    }

    fun submit() {
        val s = _state.value

        // Validation
        if (s.title.isBlank()) {
            _state.update { it.copy(error = "Title is required") }; return
        }
        if (s.addressLine.isBlank()) {
            _state.update { it.copy(error = "Address is required") }; return
        }
        if (s.contactPhone.isBlank() || s.contactPhone.filter { it.isDigit() }.length < 10) {
            _state.update { it.copy(error = "Enter a valid contact phone (10+ digits)") }; return
        }
        val parsedRooms = mutableListOf<CreateRoomOptionRequest>()
        for ((idx, room) in s.rooms.withIndex()) {
            val rent = room.monthlyRent.toIntOrNull()
            val deposit = room.securityDeposit.toIntOrNull()
            val total = room.totalBeds.toIntOrNull()
            val available = room.availableBeds.toIntOrNull()
            if (rent == null || rent <= 0) {
                _state.update { it.copy(error = "Room ${idx + 1}: enter a valid rent") }; return
            }
            if (deposit == null || deposit < 0) {
                _state.update { it.copy(error = "Room ${idx + 1}: enter a valid deposit") }; return
            }
            if (total == null || total <= 0) {
                _state.update { it.copy(error = "Room ${idx + 1}: enter total beds") }; return
            }
            if (available == null || available < 0 || available > total) {
                _state.update { it.copy(error = "Room ${idx + 1}: available beds can't exceed total") }; return
            }
            parsedRooms.add(
                CreateRoomOptionRequest(
                    sharingType = room.sharingType,
                    monthlyRent = rent,
                    securityDeposit = deposit,
                    totalBeds = total,
                    availableBeds = available
                )
            )
        }

        val req = CreateListingRequest(
            title = s.title.trim(),
            description = s.description.trim().ifBlank { null },
            addressLine = s.addressLine.trim(),
            locality = s.locality.displayName,
            pincode = s.pincode.trim().ifBlank { null },
            contactPhone = s.contactPhone.trim(),
            latitude = s.locality.lat,
            longitude = s.locality.lng,
            genderPreference = s.genderPreference,
            pgType = s.pgType,
            foodType = s.foodType,
            roomOptions = parsedRooms,
            amenityIds = s.selectedAmenityIds.toList()
        )

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true, error = null) }
            val editId = editingListingId
            val result = if (editId == null) repo.createListing(req)
                         else repo.updateListing(editId, req)
            result.fold(
                // In edit mode, reuse the existing id so the create flow's createdId navigation still fires.
                onSuccess = { id -> _state.update { it.copy(isSubmitting = false, createdId = editId ?: id) } },
                onFailure = { e ->
                    _state.update { it.copy(isSubmitting = false, error = e.message ?: "Failed to save listing") }
                }
            )
        }
    }
}