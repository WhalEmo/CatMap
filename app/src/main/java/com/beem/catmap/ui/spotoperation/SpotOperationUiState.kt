package com.beem.catmap.ui.spotoperation

import android.location.Location
import com.beem.catmap.data.model.ReportAction
import com.beem.catmap.engine.location.LocationAccessState

data class SpotOperationUiState(
    val userLocation: Location? = null,
    val shieldState: ShieldState = ShieldState.SCANNING,
    val distanceInMeters: Int? = null,           // Uzaksa kaç metre uzakta olduğunu göstermek için
    val selectedActions: List<ReportAction> = emptyList(),
    val photoUri: android.net.Uri? = null,
    val note: String = "",
    val hasPhoto: Boolean = false,               // UploadSession.REPORT'tan beslenecek
    val isSubmitEnabled: Boolean = false,        // Akıllı filtre (Validation) sonucu
    val isUploading: Boolean = false,             // Gönderime basıldığında dönecek loader

    val spotName: String = "Yükleniyor...",
    val spotLocation: String = "", // Örn: "Selçuklu, Yazır"
    val initialSpotState: String = "",
    val currentStep: Int = 1,
    val isCreateMode: Boolean = false,
    val newSpotName: String = "",
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",

    val isLoadingRadar: Boolean = true,
    val nearbySpots: List<SpotDistanceItem> = emptyList(),
    val selectedSpotId: String? = null, // null ise "Yeni Nokta Oluştur" demektir
    val isCreateAllowed: Boolean = false, //
    val isLockedMode: Boolean = false,

    val isGpsEnable: Boolean = false,
    val hasCloseSpot: Boolean = false,

    val userPreference: SpotUserPreference = SpotUserPreference.NewSpot,
    val locationAccessState: LocationAccessState = LocationAccessState.READY
)