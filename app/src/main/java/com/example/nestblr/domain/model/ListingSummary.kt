package com.example.nestblr.domain.model

class ListingSummary {
}package com.example.nestblr.domain.model

import com.example.nestblr.data.remote.dto.ListingSummaryDto

/**
 * Domain model — independent of network DTO shape.
 * If the API changes, only the mapper changes, not the UI.
 */
data class ListingSummary(
    val id: String,
    val title: String,
    val locality: String,
    val addressLine: String,
    val latitude: Double,
    val longitude: Double,
    val genderPreference: Gender,
    val pgType: PgType,
    val foodType: FoodType,
    val avgRating: Double,
    val reviewCount: Int,
    val minRent: Int?,
    val coverPhotoUrl: String?,
    val distanceKm: Double
)

enum class Gender { MALE, FEMALE, COED, UNKNOWN;
    companion object {
        fun from(s: String?) = entries.firstOrNull { it.name == s?.uppercase() } ?: UNKNOWN
    }
}

enum class PgType { PG, HOSTEL, COLIVING, UNKNOWN;
    companion object {
        fun from(s: String?) = entries.firstOrNull { it.name == s?.uppercase() } ?: UNKNOWN
    }
}

enum class FoodType { VEG, NON_VEG, BOTH, NONE, UNKNOWN;
    companion object {
        fun from(s: String?) = entries.firstOrNull { it.name == s?.uppercase() } ?: UNKNOWN
    }
}

fun ListingSummaryDto.toDomain() = ListingSummary(
    id = id,
    title = title,
    locality = locality,
    addressLine = addressLine,
    latitude = latitude,
    longitude = longitude,
    genderPreference = Gender.from(genderPreference),
    pgType = PgType.from(pgType),
    foodType = FoodType.from(foodType),
    avgRating = avgRating,
    reviewCount = reviewCount,
    minRent = minRent,
    coverPhotoUrl = coverPhotoUrl,
    distanceKm = distanceMeters / 1000.0
)