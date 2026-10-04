package com.beem.catmap.engine.location

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.beem.catmap.maps.LocationSettingsHandler
import com.google.android.gms.common.api.ResolvableApiException

class LocationPermissionCoordinator(
    caller: ActivityResultCaller,
    private val activityProvider: () -> Activity,
    private val onStateChanged: (LocationAccessState) -> Unit
) {

    private var pendingGpsException: ResolvableApiException? = null

    // 1. GPS Donanım Açma Launcher'ı
    private val gpsLauncher: ActivityResultLauncher<IntentSenderRequest> =
        caller.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                pendingGpsException = null
                onStateChanged(LocationAccessState.READY)
            } else {
                onStateChanged(LocationAccessState.GPS_DISABLED)
            }
        }

    // 2. Sistem İzin Penceresi Launcher'ı
    private val permissionLauncher: ActivityResultLauncher<Array<String>> =
        caller.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            val isFine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val isCoarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true

            if (isFine || isCoarse) {
                checkGps()
            } else {
                evaluateDeniedPermission()
            }
        }

    /**
     * Konum ve GPS durumunu baştan aşağı denetler ve State fırlatır.
     */
    fun checkStatus() {
        val activity = activityProvider()

        val hasFine = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        val hasCoarse = ContextCompat.checkSelfPermission(
            activity, Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

        if (hasFine || hasCoarse) {
            checkGps()
        } else {
            evaluateDeniedPermission()
        }
    }

    /**
     * Kullanıcı butona bastığında doğrudan sistem izin penceresini açar.
     */
    fun requestSystemPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    /**
     * Kullanıcı GPS Aç butonuna bastığında Google Play Services GPS popup'ını açar.
     */
    fun resolveGpsHardware() {
        val exception = pendingGpsException ?: run {
            checkGps()
            return
        }
        try {
            val request = IntentSenderRequest.Builder(exception.resolution).build()
            gpsLauncher.launch(request)
        } catch (_: Exception) {
            onStateChanged(LocationAccessState.GPS_DISABLED)
        }
    }

    private fun checkGps() {
        val activity = activityProvider()
        LocationSettingsHandler.checkLocationSettings(
            activity = activity,
            onGpsEnabled = {
                pendingGpsException = null
                onStateChanged(LocationAccessState.READY)
            },
            onGpsDisabled = { exception ->
                if (exception is ResolvableApiException) {
                    pendingGpsException = exception
                }
                onStateChanged(LocationAccessState.GPS_DISABLED)
            }
        )
    }

    private fun evaluateDeniedPermission() {
        val activity = activityProvider()
        val canShowAgain = ActivityCompat.shouldShowRequestPermissionRationale(
            activity, Manifest.permission.ACCESS_FINE_LOCATION
        )

        if (!canShowAgain) {
            onStateChanged(LocationAccessState.PERMISSION_PERMANENT)
        } else {
            onStateChanged(LocationAccessState.PERMISSION_RATIONALE)
        }
    }
}