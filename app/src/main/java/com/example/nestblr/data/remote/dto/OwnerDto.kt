package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateListingRequest(
    val title: String,
    val description: String? = null,
    val addressLine: String,
    val locality: String,
    val city: String = "Bengaluru",
    val pincode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val genderPreference: String,
    val pgType: String,
    val foodType: String,
    val roomOptions: List<CreateRoomOptionRequest> = emptyList(),
    val amenityIds: List<Int> = emptyList()
)

@Serializable
data class CreateRoomOptionRequest(
    val sharingType: String,
    val monthlyRent: Int,
    val securityDeposit: Int,
    val totalBeds: Int,
    val availableBeds: Int,
    val noticePeriodDays: Int = 30
)

@Serializable
data class OwnerListingDto(
    val id: String,
    val title: String,
    val locality: String,
    val pgType: String,
    val genderPreference: String,
    val status: String,
    val avgRating: Double,
    val reviewCount: Int,
    val minRent: Int? = null,
    val roomCount: Int
)

/** Response from POST create — backend returns {"id": "..."}. */
@Serializable
data class CreatedIdDto(
    val id: String
)