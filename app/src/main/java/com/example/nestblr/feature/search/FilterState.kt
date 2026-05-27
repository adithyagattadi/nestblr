package com.example.nestblr.feature.search

/**
 * UI filter state. Null = "no filter applied" for that dimension.
 * Rent range uses 2000-50000 INR/month as sensible Bengaluru bounds.
 */
data class FilterState(
    val gender: String? = null,        // "MALE" | "FEMALE" | "COED"
    val food: String? = null,          // "VEG" | "NON_VEG" | "BOTH"
    val pgType: String? = null,        // "PG" | "HOSTEL" | "COLIVING"
    val minRent: Int = MIN_RENT,
    val maxRent: Int = MAX_RENT
) {
    val hasActiveFilters: Boolean
        get() = gender != null || food != null || pgType != null ||
                minRent != MIN_RENT || maxRent != MAX_RENT

    val activeCount: Int
        get() = listOfNotNull(gender, food, pgType).size +
                (if (minRent != MIN_RENT || maxRent != MAX_RENT) 1 else 0)

    companion object {
        const val MIN_RENT = 2000
        const val MAX_RENT = 50000
    }
}