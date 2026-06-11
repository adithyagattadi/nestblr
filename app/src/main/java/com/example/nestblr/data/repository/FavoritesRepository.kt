package com.example.nestblr.data.repository

import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.domain.model.ListingSummary
import com.example.nestblr.domain.model.toDomain
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FavoritesRepository @Inject constructor(
    private val api: NestBlrApi
) {
    suspend fun add(listingId: String): Result<Unit> = runCatching {
        api.addFavorite(listingId)
        Unit
    }

    suspend fun remove(listingId: String): Result<Unit> = runCatching {
        api.removeFavorite(listingId)
        Unit
    }

    /** Maps to the domain [ListingSummary] so the shared ListingCard renders these. */
    suspend fun list(): Result<List<ListingSummary>> = runCatching {
        api.getFavorites().data.map { it.toDomain() }
    }
}
