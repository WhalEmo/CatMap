package com.beem.catmap.utils

import android.location.Location

object LocationUtils {
    /**
     * İki koordinat arasındaki mesafeyi "Metre" cinsinden verir.
     */
    fun calculateDistance(
        userLat: Double, userLng: Double,
        spotLat: Double, spotLng: Double
    ): Float {
        val results = FloatArray(1)
        Location.distanceBetween(userLat, userLng, spotLat, spotLng, results)
        return results[0] // Metre cinsinden mesafe
    }

    // Kabul edilebilir yarıçap (50 metre)
    const val ALLOWED_RADIUS_METERS = 50f
}