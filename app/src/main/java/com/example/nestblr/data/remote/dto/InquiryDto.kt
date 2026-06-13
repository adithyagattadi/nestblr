package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class InquiryDto(
    val listingId: String,
    val tenantId: String,
    val createdAt: String,
    val lastAttemptAt: String
)
