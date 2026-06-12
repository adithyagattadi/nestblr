package com.example.nestblr.core.location

import android.annotation.SuppressLint
import android.content.Context
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Thin wrapper around [FusedLocationProviderClient] that returns a single
 * coordinate. Hilt singleton — one client for the app's lifetime.
 */
@Singleton
class LocationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val client by lazy { LocationServices.getFusedLocationProviderClient(context) }

    /**
     * Returns the user's current location, or null if it can't be obtained within
     * ~5 seconds. Caller MUST have already obtained ACCESS_COARSE_LOCATION permission.
     *
     * Uses PRIORITY_BALANCED_POWER_ACCURACY (city-block accuracy) so it works with
     * the coarse permission — PRIORITY_HIGH_ACCURACY would require ACCESS_FINE_LOCATION.
     */
    @SuppressLint("MissingPermission")
    suspend fun getCurrentLocation(): Pair<Double, Double>? = withTimeoutOrNull(5_000L) {
        suspendCancellableCoroutine { cont ->
            client.getCurrentLocation(Priority.PRIORITY_BALANCED_POWER_ACCURACY, null)
                .addOnSuccessListener { loc ->
                    if (loc != null) cont.resume(loc.latitude to loc.longitude)
                    else cont.resume(null)
                }
                .addOnFailureListener { cont.resume(null) }
        }
    }
}
