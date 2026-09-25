package com.beem.catmap.engine.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.os.SystemClock
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.GoogleMap
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

object LocationEngine {


    var isMockModeEnabled: Boolean = false
    private var mockJob: Job? = null

    // Verdiğin Hedef Mama Noktası Koordinatları
    private const val TARGET_MOCK_LAT = 37.9472149
    private const val TARGET_MOCK_LNG = 32.5199219

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null

    private val _fetchDataEvent = MutableLiveData<Location>()
    val fetchDataEvent: LiveData<Location> get() = _fetchDataEvent


    fun hasLocationPermission(context: Context): Boolean {
        val fineLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val coarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        return fineLocation || coarseLocation
    }

    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    @SuppressLint("MissingPermission")
    fun startTracking(context: Context) {
        val appContext = context.applicationContext

        if (isMockModeEnabled) {
            stopTracking()
            startMockWalkingSimulation()
            return
        }

        if (!hasLocationPermission(appContext) || !isGpsEnabled(appContext)) return

        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext)
        }

        stopTracking()

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateIntervalMillis(1500)
            .setWaitForAccurateLocation(false)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                _fetchDataEvent.value = location
            }
        }

        fusedLocationClient?.requestLocationUpdates(
            locationRequest,
            locationCallback!!,
            Looper.getMainLooper()
        )
    }


    private fun startMockWalkingSimulation() {
        mockJob?.cancel()
        mockJob = CoroutineScope(Dispatchers.Default).launch {
            // Yaklaşık 120m güneyden başlatıyoruz (~0.001 derece lat ≈ 111 metre)
            var currentLat = TARGET_MOCK_LAT - 0.00110
            var currentLng = TARGET_MOCK_LNG - 0.00040

            // Her döngüde yaklaşacağı adım miktarı (~10-15 metre adımlar)
            val stepLat = 0.00010
            val stepLng = 0.00004

            var isApproaching = true

            while (isActive) {
                val mockLoc = Location("MockProvider").apply {
                    latitude = currentLat
                    longitude = currentLng
                    accuracy = 3.0f // Mükemmel GPS hassasiyeti
                    time = System.currentTimeMillis()
                    elapsedRealtimeNanos = SystemClock.elapsedRealtimeNanos()
                }

                // UI ana iş parçacığında dinlediği için postValue / UI thread üzerinden aktar
                withContext(Dispatchers.Main) {
                    _fetchDataEvent.value = mockLoc
                }

                // Hedefe yaklaştıkça yönü tersine çevirelim (Döngüsel yürüyüş testi)
                if (isApproaching) {
                    currentLat += stepLat
                    currentLng += stepLng
                    // Noktaya 5-10m kadar çok yaklaştıysa uzaklaşmaya başlasın
                    if (currentLat >= TARGET_MOCK_LAT) {
                        isApproaching = false
                    }
                } else {
                    currentLat -= stepLat
                    currentLng -= stepLng
                    // 120m geri açılınca tekrar yaklaşmaya başlasın
                    if (currentLat <= TARGET_MOCK_LAT - 0.00110) {
                        isApproaching = true
                    }
                }

                // 2 saniyede bir yeni adım at
                delay(2000L.milliseconds)
            }
        }
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
        map.isMyLocationEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false

        startTracking(context)
    }

    fun stopTracking() {
        mockJob?.cancel()
        mockJob = null

        locationCallback?.let {
            fusedLocationClient?.removeLocationUpdates(it)
        }
    }

}