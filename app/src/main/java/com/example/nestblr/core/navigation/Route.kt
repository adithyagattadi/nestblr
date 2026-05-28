package com.example.nestblr.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Auth : Route

    @Serializable
    data object Search : Route

    @Serializable
    data class Detail(val listingId: String) : Route
}