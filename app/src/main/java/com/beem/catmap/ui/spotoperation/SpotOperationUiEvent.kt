package com.beem.catmap.ui.spotoperation

sealed class SpotOperationUiEvent {
    data class ShowToast(val message: String, val isSuccess: Boolean) : SpotOperationUiEvent()
    object NavigateBack : SpotOperationUiEvent()
    object RequestLocationSettings : SpotOperationUiEvent()

    data object TriggerPermissionCoordinator : SpotOperationUiEvent()
    data object NavigateToAppSettings : SpotOperationUiEvent()
    data object ResolveGpsClicked : SpotOperationUiEvent()
}