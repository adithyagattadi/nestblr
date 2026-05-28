package com.example.nestblr.data.remote

import com.example.nestblr.data.remote.dto.ApiResponse
import com.example.nestblr.data.remote.dto.ListingDetailDto
import com.example.nestblr.data.remote.dto.ListingSummaryDto
import com.example.nestblr.data.remote.dto.RegisterRequest
import com.example.nestblr.data.remote.dto.UserDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
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
}