package com.beem.catmap.ui.feedingspot

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.spotoperation.SpotOperationFragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class FeedingSpotBottomSheetFragment : BottomSheetDialogFragment() {

    // Test veya Canlı veriyi buraya aktaracağız
    private var feedingSpot: FeedingSpot? = null

    private val viewModel: FeedingSpotViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Eğer Bundle üzerinden veri geçiyorsan burada yakalayabilirsin
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog
        dialog.setOnShowListener {
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        observeFeedingSpotEvent()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                feedingSpot?.let { spot ->
                    val realReports by viewModel.spotReports.collectAsState()
                    val uiState by viewModel.uiState.collectAsState()

                    androidx.compose.runtime.key(spot.id) {
                        LaunchedEffect(spot.id) {
                            viewModel.fetchReportsForSpot(spot.id)
                        }

                        // 🚀 EKSTRA KOD YOK, SADECE PASLIYORUZ
                        FeedingSpotDetailScreenHybrid(
                            spot = spot,
                            uiState = uiState, // State'i direkt içeri gönder
                            reports = realReports,
                            onIntent = { intent ->
                                onIntent(intent)
                            },
                            onUpdateClick = {

                                Log.d("SPOT_DEBUG", "-----------------------------------------")
                                Log.d("SPOT_DEBUG", "1. BottomSheet Çıkış Raporu:")
                                Log.d("SPOT_DEBUG", "Spot ID: '${spot.id}'")
                                Log.d("SPOT_DEBUG", "Spot Adı: '${spot.spotName}'")
                                Log.d("SPOT_DEBUG", "İlçe/Mahalle: '${spot.district}' / '${spot.neighborhood}'")
                                Log.d("SPOT_DEBUG", "Koordinat: ${spot.coordinates?.latitude}, ${spot.coordinates?.longitude}")

                                dismiss()
                                val args = SpotOperationFragment.newArgs(
                                    spotId = spot.id,
                                    lat = spot.coordinates?.latitude ?: 0.0,
                                    lng = spot.coordinates?.longitude ?: 0.0,
                                    spotName = spot.spotName,
                                    district = spot.district,
                                    neighborhood = spot.neighborhood
                                )
                                SmartNavigationEngine.navigateTo(Screen.SPOT_OPERATION, args, spot.id)
                            }
                        )
                    }
                }
            }
        }
    }

    private fun observeFeedingSpotEvent() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.eventFlow.collect { event ->
                    handleEvent(event)
                }
            }
        }
    }

    private fun onIntent(intent: FeedingSpotIntent) {
        viewModel.onIntent(intent)
    }

    private fun handleEvent(event: FeedingSpotEvent) {
        when (event) {
            is FeedingSpotEvent.OpenUserProfile -> {
                dismiss()
                NavigationHelper.navigateToProfile(event.userId)
            }
            else -> {

            }
        }
    }

    companion object {
        const val TAG = "FeedingSpotBottomSheet"

        fun newInstance(spot: FeedingSpot): FeedingSpotBottomSheetFragment {
            return FeedingSpotBottomSheetFragment().apply {
                this.feedingSpot = spot
            }
        }
    }
}