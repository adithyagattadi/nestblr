package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateReviewRequest(val rating: Int, val comment: String)
