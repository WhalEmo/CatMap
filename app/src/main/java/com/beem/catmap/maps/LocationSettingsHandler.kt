package com.beem.catmap.maps

import android.app.Activity
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.Priority

object LocationSettingsHandler {

    const val REQUEST_CHECK_SETTINGS = 1001

    fun checkLocationSettings(
        activity: Activity,
        onGpsEnabled: () -> Unit,
        onGpsDisabled: (Exception) -> Unit
    ) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 4000).build()
        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val client = LocationServices.getSettingsClient(activity)
        val task = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            onGpsEnabled()
        }

        task.addOnFailureListener { exception ->
            onGpsDisabled(exception)
        }
    }
}