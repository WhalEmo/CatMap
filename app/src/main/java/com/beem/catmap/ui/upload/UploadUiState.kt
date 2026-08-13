package com.beem.catmap.ui.upload

import android.net.Uri
import com.beem.catmap.data.model.CatModel

data class UploadUiState(
    val selectedImages: List<Uri> = emptyList(),
    val uploadedPhotoUrls: List<String> = emptyList(),
    val isLoading: Boolean = false,
    val uploadProgress: Int = 0,
    val isSuccess: Boolean = false,
    val errorMessage: String? = null,
    val createdDocument: CatModel? = null,
    val isUploadComplete: Boolean = false,
    val isAllDone: Boolean = false,
    val uploadStage: UploadStage = UploadStage.FETCHING_LOCATION,
)