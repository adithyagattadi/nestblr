package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class ApiResponse<T>(
    val data: T,
    val error: String? = null,
    val meta: PageMeta? = null
)

@Serializable
data class PageMeta(
    val page: Int,
    val size: Int,
    val total: Int
)

@Serializable
data class ListingSummaryDto(
    val id: String,
    val title: String,
    val locality: String,
    val addressLine: String,
    val latitude: Double,
    val longitude: Double,
    val genderPreference: String,
    val pgType: String,
    val foodType: String,
    val avgRating: Double,
    val reviewCount: Int,
    val minRent: Int? = null,
    val coverPhotoUrl: String? = null,
    val distanceMeters: Double? = null,
    val isFavorite: Boolean = false
)