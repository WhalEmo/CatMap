package com.beem.catmap.ui.spotoperation

import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.compose.BackHandler
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.beem.catmap.engine.location.LocationAccessState
import com.beem.catmap.engine.location.LocationEngine
import com.beem.catmap.engine.location.LocationPermissionCoordinator
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.beem.catmap.ui.spotoperation.SpotOperationIntent.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class SpotOperationFragment : Fragment() {

    companion object {
        const val TAG = "SpotOperationFragment"
        private const val ARG_SPOT_ID = "spotId"
        private const val ARG_LAT = "lat"
        private const val ARG_LNG = "lng"
        private const val ARG_SPOT_NAME = "spotName"
        private const val ARG_DISTRICT = "district"
        private const val ARG_NEIGHBORHOOD = "neighborhood"

        fun newArgs(spotId: String): Bundle {
            return Bundle().apply {
                putString(ARG_SPOT_ID, spotId)
            }
        }

        fun newArgs(
            spotId: String, lat: Double, lng: Double,
            spotName: String, district: String, neighborhood: String
        ): Bundle {
            return Bundle().apply {
                putString(ARG_SPOT_ID, spotId)
                putDouble(ARG_LAT, lat)
                putDouble(ARG_LNG, lng)
                putString(ARG_SPOT_NAME, spotName)
                putString(ARG_DISTRICT, district)
                putString(ARG_NEIGHBORHOOD, neighborhood)
            }
        }
    }

    private val spotId: String
        get() = arguments?.getString(ARG_SPOT_ID) ?: ""

    private val targetLat: Double
        get() = arguments?.getDouble(ARG_LAT) ?: 0.0

    private val targetLng: Double
        get() = arguments?.getDouble(ARG_LNG) ?: 0.0

    private val spotName: String
        get() = arguments?.getString(ARG_SPOT_NAME) ?: "Bilinmeyen Nokta"

    private val district: String
        get() = arguments?.getString(ARG_DISTRICT) ?: ""

    private val neighborhood: String
        get() = arguments?.getString(ARG_NEIGHBORHOOD) ?: ""

    private val viewModel: SpotOperationViewModel by viewModels()

    private val locationCoordinator = LocationPermissionCoordinator(
        caller = this,
        activityProvider = { requireActivity() },
        onStateChanged = { accessState ->
            onIntent(UpdateLocationAccessState(accessState))
            if (accessState == LocationAccessState.READY && isAdded) {
                LocationEngine.startTracking(requireContext())
            }
        }
    )

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val state by viewModel.uiState.collectAsState()

                val snackBarHostState = remember { SnackbarHostState() }
                val scope = rememberCoroutineScope()


                BackHandler(
                    enabled = state.isUploading
                ) {
                    onIntent(SpotOperationIntent.BackHandler)
                }

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is SpotOperationUiEvent.ShowToast -> {
                                scope.launch {
                                    snackBarHostState.currentSnackbarData?.dismiss()
                                    snackBarHostState.showSnackbar(
                                        message = event.message,
                                        actionLabel = event.isSuccess.toString(),
                                        duration = SnackbarDuration.Short
                                    )
                                }
                            }
                            is SpotOperationUiEvent.NavigateBack -> {
                                Log.d("SPOT_DEBUG", "NavigateBack")
                                delay(1500.milliseconds)
                                SmartNavigationEngine.navigateBack()

                                val currentFinalName = if (state.isCreateMode && state.newSpotName.isNotBlank()) {
                                    state.newSpotName
                                } else {
                                    spotName
                                }

                                onIntent(
                                    RestartViewModel(
                                        spotId = spotId,
                                        lat = targetLat,
                                        lng = targetLng,
                                        spotName = currentFinalName,
                                        district = district,
                                        neighborhood = neighborhood
                                    )
                                )
                            }
                            is SpotOperationUiEvent.RequestLocationSettings -> {
                                // Gelecekte GPS açma dialogu için kullanılacak
                            }

                            SpotOperationUiEvent.NavigateToAppSettings -> {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", requireContext().packageName, null)
                                }
                                startActivity(intent)
                            }
                            SpotOperationUiEvent.TriggerPermissionCoordinator -> {
                                locationCoordinator.requestSystemPermission()
                            }

                            SpotOperationUiEvent.ResolveGpsClicked -> {
                                locationCoordinator.resolveGpsHardware()
                            }
                        }
                    }
                }

                SpotOperationScreen(
                    state = state,
                    snackBarHostState = snackBarHostState,
                    onIntent = { intent ->
                        onIntent(intent)
                    }
                )
            }
        }
    }



    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d("DIALOG_CALLER_DEBUG", "🟢 onViewCreated başladı, savedInstanceState null mı? -> ${savedInstanceState == null}")

        handleBackPressWithEngine()

        val spotId = arguments?.getString(ARG_SPOT_ID) ?: ""
        val targetLat = arguments?.getDouble(ARG_LAT) ?: 0.0
        val targetLng = arguments?.getDouble(ARG_LNG) ?: 0.0
        val spotName = arguments?.getString(ARG_SPOT_NAME) ?: "Bilinmeyen Nokta"
        val district = arguments?.getString(ARG_DISTRICT) ?: ""
        val neighborhood = arguments?.getString(ARG_NEIGHBORHOOD) ?: ""


        onIntent(
            intent = SpotOperationIntent.Initialize(
                spotId = spotId,
                lat = targetLat,
                lng = targetLng,
                spotName = spotName,
                district = district,
                neighborhood = neighborhood
            )
        )

        observeLocationEvents()


        Log.d("DIALOG_CALLER_DEBUG", "🟢 locationCoordinator.checkAndRequest() çağrılıyor...")
        locationCoordinator.checkStatus()

    }

    override fun onResume() {
        super.onResume()
        Log.d("SPOT_ONRESUME_DEBUG", "onResume çalıştı checkOnresume metodu çalıştırıldı." )
        locationCoordinator.checkStatus()
    }

    private fun observeLocationEvents() {
        LocationEngine.fetchDataEvent.observe(viewLifecycleOwner) { event ->
            if (event != null) {
                Log.d("SPOT_GPS_DEBUG", "📍 LocationEngine'den veri geldi: ${event.latitude}, ${event.longitude}")
                val loc = Location("LocationEngine").apply {
                    latitude = event.latitude
                    longitude = event.longitude
                }
                onIntent(SpotOperationIntent.LocationUpdated(loc))
            } else {
                Log.w("SPOT_GPS_DEBUG", "⚠️ LocationEngine event null geldi!")
            }
        }
    }


    private fun onIntent(intent: SpotOperationIntent) {
        viewModel.onIntent(intent)
    }

}