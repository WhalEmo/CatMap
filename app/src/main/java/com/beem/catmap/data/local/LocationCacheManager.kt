package com.beem.catmap.data.local

import android.content.Context
import com.beem.catmap.CatMapApp
import com.google.android.gms.maps.model.LatLng

object LocationCacheManager {

    private const val PREF_NAME = "LocationCachePrefs"
    private const val KEY_LAT = "last_lat"
    private const val KEY_LNG = "last_lng"
    private const val KEY_ZOOM = "last_zoom"

    private const val DEFAULT_ZOOM = 14f

    private val prefs by lazy {
        CatMapApp.instance.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }


    fun saveLastLocation(latLng: LatLng, zoom: Float = 16f) {
        prefs.edit().apply {
            putFloat(KEY_LAT, latLng.latitude.toFloat())
            putFloat(KEY_LNG, latLng.longitude.toFloat())
            putFloat(KEY_ZOOM, zoom)
            apply()
        }
    }

    fun getLastLocation(): LatLng {
        val lat = prefs.getFloat(KEY_LAT, 0f).toDouble()
        val lng = prefs.getFloat(KEY_LNG, 0f).toDouble()
        return LatLng(lat, lng)
    }

    fun getLastZoom(): Float {
        return prefs.getFloat(KEY_ZOOM, DEFAULT_ZOOM)
    }

    fun hasSavedLocation(): Boolean {
        return prefs.contains(KEY_LAT) && prefs.contains(KEY_LNG)
    }
}