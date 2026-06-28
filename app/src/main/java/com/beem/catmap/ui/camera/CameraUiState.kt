package com.beem.catmap.ui.camera

import android.net.Uri

data class CameraUiState(
    val capturedImages: List<Uri> = emptyList(),
    val activePreviewUri: Uri? = null,
    val currentMode: CameraMode = CameraMode.LIVE_PREVIEW,
    val activeImageSource: ImageSource = ImageSource.TEMP_CACHE
)