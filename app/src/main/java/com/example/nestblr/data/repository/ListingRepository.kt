package com.example.nestblr.data.repository

import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.domain.model.ListingDetail
import com.example.nestblr.domain.model.ListingSummary
import com.example.nestblr.domain.model.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ListingRepository @Inject constructor(
    private val api: NestBlrApi
) {
    suspend fun searchNearby(
        lat: Double,
        lng: Double,
        radiusKm: Double = 5.0,
        gender: String? = null,
        maxRent: Int? = null,
        pgType: String? = null
    ): Result<List<ListingSummary>> = runCatching {
        api.searchListings(
            lat = lat,
            lng = lng,
            radiusKm = radiusKm,
            gender = gender,
            maxRent = maxRent,
            pgType = pgType
        ).data.map { it.toDomain() }
    }

    suspend fun getById(id: String): Result<ListingDetail> = runCatching {
        api.getListingById(id).data.toDomain()
    }
}