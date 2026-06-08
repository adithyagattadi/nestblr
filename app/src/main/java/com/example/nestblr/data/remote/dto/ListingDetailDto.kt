package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ListingDetailDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val addressLine: String,
    val locality: String,
    val city: String,
    val pincode: String? = null,
    val latitude: Double,
    val longitude: Double,
    val genderPreference: String,
    val pgType: String,
    val foodType: String,
    val contactPhone: String? = null,
    val avgRating: Double,
    val reviewCount: Int,
    val status: String,
    val owner: OwnerDto,
    val roomOptions: List<RoomOptionDto> = emptyList(),
    val photos: List<PhotoDto> = emptyList(),
    val amenities: List<AmenityDto> = emptyList(),
    val recentReviews: List<ReviewDto> = emptyList()
)

@Serializable
data class OwnerDto(
    val id: String,
    val fullName: String? = null,
    val phone: String,
    val isVerified: Boolean
)

@Serializable
data class RoomOptionDto(
    val id: String,
    val sharingType: String,
    val monthlyRent: Int,
    val securityDeposit: Int,
    val totalBeds: Int,
    val availableBeds: Int,
    val noticePeriodDays: Int
)

@Serializable
data class PhotoDto(
    val id: String,
    val url: String,
    val thumbnailUrl: String,
    val displayOrder: Int
)

@Serializable
data class AmenityDto(
    val id: Int,
    val name: String,
    val iconKey: String? = null
)

@Serializable
data class ReviewDto(
    val id: String,
    val userName: String? = null,
    val rating: Int,
    val comment: String? = null,
    val stayedFrom: String? = null,
    val stayedUntil: String? = null,
    val createdAt: String
)