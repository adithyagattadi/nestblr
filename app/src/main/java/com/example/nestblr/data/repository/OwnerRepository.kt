package com.example.nestblr.data.repository

import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.remote.dto.CreateListingRequest
import com.example.nestblr.data.remote.dto.OwnerListingDto
import com.example.nestblr.data.remote.dto.RoomOptionDto
import com.example.nestblr.data.remote.dto.UpdateRoomAvailabilityRequest
import com.example.nestblr.domain.model.Photo
import com.example.nestblr.domain.model.toDomain
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OwnerRepository @Inject constructor(
    private val api: NestBlrApi
) {
    suspend fun getMyListings(): Result<List<OwnerListingDto>> = runCatching {
        api.getMyListings().data
    }

    suspend fun createListing(req: CreateListingRequest): Result<String> = runCatching {
        api.createListing(req).data.id
    }

    suspend fun updateListing(id: String, req: CreateListingRequest): Result<String> = runCatching {
        api.updateListing(id, req).data.id
    }

    suspend fun deleteListing(id: String): Result<Unit> = runCatching {
        api.deleteListing(id)
        Unit
    }

    suspend fun uploadPhoto(listingId: String, file: File): Result<Photo> = runCatching {
        val mediaType = guessImageMediaType(file.name).toMediaTypeOrNull()
        val body = file.asRequestBody(mediaType)
        val part = MultipartBody.Part.createFormData("file", file.name, body)
        api.uploadPhoto(listingId, part).data.toDomain()
    }

    suspend fun deletePhoto(listingId: String, photoId: String): Result<Unit> = runCatching {
        api.deletePhoto(listingId, photoId)
        Unit
    }

    suspend fun updateRoomAvailability(
        listingId: String,
        roomId: String,
        availableBeds: Int
    ): Result<RoomOptionDto> = runCatching {
        api.updateRoomAvailability(listingId, roomId, UpdateRoomAvailabilityRequest(availableBeds)).data
    }

    /** Reuses the public detail endpoint — backend already returns photos there. */
    suspend fun getListingPhotos(listingId: String): Result<List<Photo>> = runCatching {
        api.getListingById(listingId).data.photos.map { it.toDomain() }
    }

    /** Promotes a photo to cover (display_order 0); returns photos in new order. */
    suspend fun setCoverPhoto(listingId: String, photoId: String): Result<List<Photo>> = runCatching {
        api.setCoverPhoto(listingId, photoId).data.map { it.toDomain() }
    }

    private fun guessImageMediaType(filename: String): String = when (
        filename.substringAfterLast('.', "").lowercase()
    ) {
        "png" -> "image/png"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }
}