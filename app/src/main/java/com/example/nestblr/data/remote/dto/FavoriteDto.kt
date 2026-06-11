package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class FavoriteDto(
    val listingId: String,
    val createdAt: String
)
