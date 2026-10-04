package com.beem.catmap.ui.spotoperation

import android.location.Location
import com.beem.catmap.data.model.ReportAction
import com.beem.catmap.engine.location.LocationAccessState

sealed class SpotOperationIntent {
    data class Initialize(
        val spotId: String,
        val lat: Double,
        val lng: Double,
        val spotName: String,
        val district: String,
        val neighborhood: String
    ) : SpotOperationIntent()
    object RetryShield : SpotOperationIntent()
    object OpenCamera : SpotOperationIntent()
    data class ToggleAction(val action: ReportAction) : SpotOperationIntent()
    data class UpdateNote(val note: String) : SpotOperationIntent()
    object SubmitReport : SpotOperationIntent()
    object NavigateBack : SpotOperationIntent()
    object RemovePhoto : SpotOperationIntent()

    object NextStep : SpotOperationIntent()
    object PreviousStep : SpotOperationIntent()

    data class UpdateNewSpotName(val name: String) : SpotOperationIntent()

    data class SelectSpot(val spotId: String?) : SpotOperationIntent()

    object RestartGpsInitialize: SpotOperationIntent()

    data class GpsError(
        val message: String
    ): SpotOperationIntent()

    data class RestartViewModel(
        val spotId: String,
        val lat: Double,
        val lng: Double,
        val spotName: String,
        val district: String,
        val neighborhood: String
    ) : SpotOperationIntent()

    data class LocationUpdated(val location: Location) : SpotOperationIntent()

    object BackHandler: SpotOperationIntent()

    object UploadReportCancel: SpotOperationIntent()

    data object RequestLocationPermissionClicked : SpotOperationIntent()
    data object OpenAppSettingsClicked : SpotOperationIntent()
    data object ResolveGpsClicked : SpotOperationIntent()

    data class UpdateLocationAccessState(val state: LocationAccessState) : SpotOperationIntent()
}