package com.example.nestblr.data.remote

import com.example.nestblr.data.remote.dto.ApiResponse
import com.example.nestblr.data.remote.dto.CreateListingRequest
import com.example.nestblr.data.remote.dto.CreatedIdDto
import com.example.nestblr.data.remote.dto.ListingDetailDto
import com.example.nestblr.data.remote.dto.ListingSummaryDto
import com.example.nestblr.data.remote.dto.OwnerListingDto
import com.example.nestblr.data.remote.dto.PhotoDto
import com.example.nestblr.data.remote.dto.RegisterRequest
import com.example.nestblr.data.remote.dto.RoomOptionDto
import com.example.nestblr.data.remote.dto.UpdateRoomAvailabilityRequest
import com.example.nestblr.data.remote.dto.UserDto
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface NestBlrApi {

    @GET("api/v1/listings/search")
    suspend fun searchListings(
        @Query("lat") lat: Double,
        @Query("lng") lng: Double,
        @Query("radius_km") radiusKm: Double = 5.0,
        @Query("min_rent") minRent: Int? = null,
        @Query("max_rent") maxRent: Int? = null,
        @Query("gender") gender: String? = null,
        @Query("food") food: String? = null,
        @Query("pg_type") pgType: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20
    ): ApiResponse<List<ListingSummaryDto>>

    @GET("api/v1/listings/{id}")
    suspend fun getListingById(
        @Path("id") id: String
    ): ApiResponse<ListingDetailDto>

    @POST("api/v1/auth/register")
    suspend fun register(
        @Body body: RegisterRequest
    ): ApiResponse<UserDto>

    @GET("api/v1/auth/me")
    suspend fun getMe(): ApiResponse<UserDto>

    // ── Owner endpoints ──
    @GET("api/v1/owner/listings")
    suspend fun getMyListings(): ApiResponse<List<OwnerListingDto>>

    @POST("api/v1/owner/listings")
    suspend fun createListing(
        @Body body: CreateListingRequest
    ): ApiResponse<CreatedIdDto>

    @PUT("api/v1/owner/listings/{id}")
    suspend fun updateListing(
        @Path("id") id: String,
        @Body body: CreateListingRequest
    ): ApiResponse<CreatedIdDto>

    @DELETE("api/v1/owner/listings/{id}")
    suspend fun deleteListing(
        @Path("id") id: String
    ): ApiResponse<Map<String, String>>

    @Multipart
    @POST("api/v1/owner/listings/{id}/photos")
    suspend fun uploadPhoto(
        @Path("id") id: String,
        @Part file: MultipartBody.Part
    ): ApiResponse<PhotoDto>

    @DELETE("api/v1/owner/listings/{listingId}/photos/{photoId}")
    suspend fun deletePhoto(
        @Path("listingId") listingId: String,
        @Path("photoId") photoId: String
    ): ApiResponse<Map<String, String>>

    @PATCH("api/v1/owner/listings/{listingId}/rooms/{roomId}")
    suspend fun updateRoomAvailability(
        @Path("listingId") listingId: String,
        @Path("roomId") roomId: String,
        @Body body: UpdateRoomAvailabilityRequest
    ): ApiResponse<RoomOptionDto>
}