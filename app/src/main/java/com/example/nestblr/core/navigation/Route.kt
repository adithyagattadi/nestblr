package com.example.nestblr.core.navigation

import kotlinx.serialization.Serializable

/**
 * All navigation destinations defined as @Serializable types.
 * Compile-time safe — no string routes anywhere.
 *
 * Reference: https://developer.android.com/guide/navigation/design/type-safety
 */
sealed interface Route {

    @Serializable
    data object Search : Route

    @Serializable
    data class Detail(val listingId: String) : Route
}