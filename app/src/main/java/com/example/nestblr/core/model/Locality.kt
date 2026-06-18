package com.example.nestblr.core.model

/**
 * Bengaluru localities with approximate center coordinates.
 * Shared between the owner create-listing form (centroid for a new listing)
 * and the tenant search screen (locality picker → map/list center).
 */
enum class Locality(val displayName: String, val lat: Double, val lng: Double) {
    KORAMANGALA("Koramangala", 12.9352, 77.6245),
    HSR_LAYOUT("HSR Layout", 12.9116, 77.6412),
    BTM_LAYOUT("BTM Layout", 12.9165, 77.6101),
    INDIRANAGAR("Indiranagar", 12.9716, 77.6412),
    JAYANAGAR("Jayanagar", 12.9250, 77.5938),
    WHITEFIELD("Whitefield", 12.9698, 77.7500),
    MARATHAHALLI("Marathahalli", 12.9591, 77.6974),
    BELLANDUR("Bellandur", 12.9259, 77.6649),
    ELECTRONIC_CITY("Electronic City", 12.8452, 77.6602),
    JP_NAGAR("JP Nagar", 12.9077, 77.5853),
    RAJAJINAGAR("Rajajinagar", 12.9866, 77.5520),
    SARJAPUR("Sarjapur Road", 12.9010, 77.6870)
}
