package com.beem.catmap.Maps

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import android.content.pm.PackageManager
import android.location.Location
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.beem.catmap.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import kotlinx.coroutines.tasks.await


object LocationEngine {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var customLocationMarker: Marker? = null

    private val _fetchDataEvent = MutableLiveData<LatLng>()
    val fetchDataEvent: LiveData<LatLng> get() = _fetchDataEvent


    fun hasLocationPermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    @SuppressLint("MissingPermission")
    suspend fun getLastKnownLocation(context: Context): Location? {
        if (!hasLocationPermission(context)) return null

        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }

        return try {
            fusedLocationClient?.lastLocation?.await()
        } catch (e: Exception) {
            null
        }
    }

    @SuppressLint("MissingPermission")
    fun startTracking(context: Context, map: GoogleMap) {
        if (!hasLocationPermission(context)) return

        map.isMyLocationEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false

        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val currentLatLng = LatLng(location.latitude, location.longitude)

                _fetchDataEvent.value = currentLatLng
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }

    fun stopTracking() {
        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
        customLocationMarker = null
    }

}