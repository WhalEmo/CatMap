package com.beem.catmap.ui.camera

data class CameraUiState(
    val capturedImages: List<CapturedImage> = emptyList(),
    val previewedImage: CapturedImage? = null,

    // 🚀 Spam/Hızlı Tetik engelleyici
    val isCapturing: Boolean = false,

    // 🚀 Galeriye yazma/silme sırasında ortada dönecek loader
    val isProcessing: Boolean = false
) {
    // currentMode artık hata yapmaya kapalı, dinamik hesaplanıyor
    val currentMode: CameraMode
        get() = if (previewedImage != null) CameraMode.IMAGE_PREVIEW else CameraMode.LIVE_PREVIEW
}