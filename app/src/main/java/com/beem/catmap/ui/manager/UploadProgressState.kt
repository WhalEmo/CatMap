package com.beem.catmap.ui.manager

import com.beem.catmap.data.model.CatModel

sealed class UploadProgressState {
    data class Loading(val progress: Int) : UploadProgressState()
    data class Success(val catModel: CatModel) : UploadProgressState()
    data class Error(val exception: Throwable) : UploadProgressState()
}