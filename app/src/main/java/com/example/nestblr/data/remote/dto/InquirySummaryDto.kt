package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

/**
 * Owner-facing, aggregated-by-listing inquiry view. Carries NO tenant identity —
 * only how many distinct tenants inquired and when the latest one was.
 */
@Serializable
data class InquirySummaryDto(
    val listingId: String,
    val listingTitle: String,
    val uniqueInquirerCount: Int,
    val lastInquiryAt: String
)
