package com.example.nestblr.data.repository

import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.remote.dto.CreateReviewRequest
import com.example.nestblr.data.remote.dto.ReviewDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReviewsRepository @Inject constructor(
    private val api: NestBlrApi
) {
    suspend fun submitReview(listingId: String, rating: Int, comment: String): Result<ReviewDto> =
        runCatching {
            api.submitReview(listingId, CreateReviewRequest(rating, comment)).data
        }

    suspend fun deleteMyReview(listingId: String): Result<Unit> = runCatching {
        api.deleteMyReview(listingId)
        Unit
    }
}
