package com.example.nestblr.data.repository

import com.example.nestblr.data.remote.NestBlrApi
import com.example.nestblr.data.remote.dto.InquirySummaryDto
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InquiriesRepository @Inject constructor(
    private val api: NestBlrApi
) {
    /** Logs that the tenant attempted to contact the owner. Idempotent upsert
     *  per (tenant, listing) on the backend. The response is discarded — callers
     *  treat this as best-effort. */
    suspend fun logInquiry(listingId: String): Result<Unit> = runCatching {
        api.logInquiry(listingId)
        Unit
    }

    /** Owner's per-listing inquiry summary, ordered by most recent inquiry. */
    suspend fun getSummary(): Result<List<InquirySummaryDto>> = runCatching {
        api.getInquirySummary().data
    }
}
