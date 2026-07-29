package com.beem.catmap.ui.manager

sealed class UploadProgressState {
    data class Loading(val progress: Int) : UploadProgressState()
    data class Success(val documentId: String) : UploadProgressState()
    data class Error(val exception: Throwable) : UploadProgressState()
}