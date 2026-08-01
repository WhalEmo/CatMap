package com.beem.catmap.ui.upload

import android.net.Uri

data class UploadUiState(
    val selectedImages: List<Uri> = emptyList(),
    val uploadedPhotoUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val uploadProgress: Int = 0,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val createdDocumentId: String? = null,
    val isUploadComplete: Boolean = false,
    val isAllDone: Boolean = false,
    val uploadStage: UploadStage = UploadStage.FETCHING_LOCATION,
)