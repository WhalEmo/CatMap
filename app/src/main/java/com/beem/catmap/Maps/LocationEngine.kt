package com.beem.catmap.Maps

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Looper
import android.animation.ValueAnimator
import android.location.Location
import android.view.animation.LinearInterpolator
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.beem.catmap.R
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

object LocationEngine {

    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var customLocationMarker: Marker? = null

    private var sonCekilenKonum: Location? = null

    private val _fetchDataEvent = MutableLiveData<LatLng>()
    val fetchDataEvent: LiveData<LatLng> get() = _fetchDataEvent

    @SuppressLint("MissingPermission")
    fun startTracking(context: Context, map: GoogleMap) {
        map.isMyLocationEnabled = false
        map.uiSettings.isMyLocationButtonEnabled = false

        if (fusedLocationClient == null) {
            fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
        }

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000)
            .setMinUpdateIntervalMillis(2000)
            .build()

        val puckIcon = getBitmapDescriptorFromVector(context, R.drawable.ic_location_puck)

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                val location = locationResult.lastLocation ?: return
                val currentLatLng = LatLng(location.latitude, location.longitude)

                if (customLocationMarker == null) {
                    customLocationMarker = map.addMarker(
                        MarkerOptions()
                            .position(currentLatLng)
                            .icon(puckIcon)
                            .anchor(0.5f, 0.5f)
                            .zIndex(999.0f)
                    )
                } else {
                    animateMarker(customLocationMarker!!, currentLatLng)
                }
                if (sonCekilenKonum == null || sonCekilenKonum!!.distanceTo(location) > 500f) {
                    sonCekilenKonum = location
                    _fetchDataEvent.value = currentLatLng
                }
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
    }

    private fun getBitmapDescriptorFromVector(context: Context, vectorResId: Int): BitmapDescriptor {
        val vectorDrawable = ContextCompat.getDrawable(context, vectorResId)!!
        vectorDrawable.setBounds(0, 0, vectorDrawable.intrinsicWidth, vectorDrawable.intrinsicHeight)
        val bitmap = Bitmap.createBitmap(
            vectorDrawable.intrinsicWidth,
            vectorDrawable.intrinsicHeight,
            Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        vectorDrawable.draw(canvas)
        return BitmapDescriptorFactory.fromBitmap(bitmap)
    }

    private fun animateMarker(marker: Marker, toPosition: LatLng) {
        val startPosition = marker.position
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        valueAnimator.duration = 800 // 800ms içinde yumuşak geçiş
        valueAnimator.interpolator = LinearInterpolator()

        valueAnimator.addUpdateListener { animation ->
            val v = animation.animatedFraction
            val lng = v * toPosition.longitude + (1 - v) * startPosition.longitude
            val lat = v * toPosition.latitude + (1 - v) * startPosition.latitude
            marker.position = LatLng(lat, lng)
        }
        valueAnimator.start()
    }
}