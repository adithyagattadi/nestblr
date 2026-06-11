package com.example.nestblr.domain.model

import com.example.nestblr.data.remote.dto.AmenityDto
import com.example.nestblr.data.remote.dto.ListingDetailDto
import com.example.nestblr.data.remote.dto.OwnerDto
import com.example.nestblr.data.remote.dto.PhotoDto
import com.example.nestblr.data.remote.dto.ReviewDto
import com.example.nestblr.data.remote.dto.RoomOptionDto

data class ListingDetail(
    val id: String,
    val title: String,
    val description: String?,
    val addressLine: String,
    val locality: String,
    val city: String,
    val pincode: String?,
    val latitude: Double,
    val longitude: Double,
    val genderPreference: Gender,
    val pgType: PgType,
    val foodType: FoodType,
    val avgRating: Double,
    val reviewCount: Int,
    val isFavorite: Boolean,
    val owner: Owner,
    val roomOptions: List<RoomOption>,
    val photos: List<Photo>,
    val amenities: List<Amenity>,
    val recentReviews: List<Review>
)

data class Owner(
    val id: String,
    val fullName: String?,
    val phone: String,
    val isVerified: Boolean
)

data class RoomOption(
    val id: String,
    val sharingType: SharingType,
    val monthlyRent: Int,
    val securityDeposit: Int,
    val totalBeds: Int,
    val availableBeds: Int,
    val noticePeriodDays: Int
)

enum class SharingType { SINGLE, DOUBLE, TRIPLE, QUAD, UNKNOWN;
    companion object {
        fun from(s: String?) = entries.firstOrNull { it.name == s?.uppercase() } ?: UNKNOWN
    }
}

data class Photo(
    val id: String,
    val url: String,
    val thumbnailUrl: String,
    val displayOrder: Int
)

data class Amenity(
    val id: Int,
    val name: String,
    val iconKey: String?
)

data class Review(
    val id: String,
    val userId: String?,
    val userName: String?,
    val rating: Int,
    val comment: String?,
    val stayedFrom: String?,
    val stayedUntil: String?,
    val createdAt: String
)

// Mappers
fun ListingDetailDto.toDomain() = ListingDetail(
    id = id,
    title = title,
    description = description,
    addressLine = addressLine,
    locality = locality,
    city = city,
    pincode = pincode,
    latitude = latitude,
    longitude = longitude,
    genderPreference = Gender.from(genderPreference),
    pgType = PgType.from(pgType),
    foodType = FoodType.from(foodType),
    avgRating = avgRating,
    reviewCount = reviewCount,
    isFavorite = isFavorite,
    owner = owner.toDomain(),
    roomOptions = roomOptions.map { it.toDomain() },
    photos = photos.map { it.toDomain() },
    amenities = amenities.map { it.toDomain() },
    recentReviews = recentReviews.map { it.toDomain() }
)

fun OwnerDto.toDomain() = Owner(id, fullName, phone, isVerified)

fun RoomOptionDto.toDomain() = RoomOption(
    id, SharingType.from(sharingType), monthlyRent, securityDeposit,
    totalBeds, availableBeds, noticePeriodDays
)

fun PhotoDto.toDomain() = Photo(id, url, thumbnailUrl, displayOrder)

fun AmenityDto.toDomain() = Amenity(id, name, iconKey)

fun ReviewDto.toDomain() = Review(id, userId, userName, rating, comment, stayedFrom, stayedUntil, createdAt)