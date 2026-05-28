package com.example.nestblr.core.navigation

import kotlinx.serialization.Serializable

sealed interface Route {

    @Serializable
    data object Auth : Route

    /** Post-login gate that fetches role and redirects to Search or OwnerHome. */
    @Serializable
    data object RoleGate : Route

    @Serializable
    data object Search : Route

    @Serializable
    data class Detail(val listingId: String) : Route

    @Serializable
    data object OwnerHome : Route

    @Serializable
    data object CreateListing : Route
}