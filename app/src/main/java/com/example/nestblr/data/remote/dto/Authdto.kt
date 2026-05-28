package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class RegisterRequest(
    val role: String,
    val fullName: String? = null,
    val phone: String? = null
)

@Serializable
data class UserDto(
    val id: String,
    val firebaseUid: String,
    val email: String? = null,
    val phone: String? = null,
    val fullName: String? = null,
    val role: String,
    val isVerified: Boolean
)