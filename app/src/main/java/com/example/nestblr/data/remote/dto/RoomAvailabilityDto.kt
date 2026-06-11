package com.example.nestblr.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UpdateRoomAvailabilityRequest(val availableBeds: Int)
