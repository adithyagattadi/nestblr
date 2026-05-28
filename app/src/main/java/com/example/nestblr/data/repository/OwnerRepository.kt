package com.example.nestblr.data.repository

import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.remote.dto.CreateListingRequest
import com.example.nestblr.data.remote.dto.OwnerListingDto
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
}