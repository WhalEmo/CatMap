package com.beem.catmap.ui.upload

import android.net.Uri

data class UploadUiState(
    val selectedImages: List<Uri> = emptyList(),
    val isLoading: Boolean = false,
    val uploadProgress: Int = 0,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val createdDocumentId: String? = null
)