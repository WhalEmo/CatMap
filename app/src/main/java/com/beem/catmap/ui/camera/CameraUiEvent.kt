package com.beem.catmap.ui.camera

sealed interface CameraUiEvent {
    data class ShowToast(val message: String, val isSuccess: Boolean) : CameraUiEvent
}