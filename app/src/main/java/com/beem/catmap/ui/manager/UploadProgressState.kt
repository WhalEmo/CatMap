package com.beem.catmap.ui.manager

sealed class UploadProgressState {
    data class Loading(val progress: Int) : UploadProgressState()
    data class Success( val documentId: String, val imageUrls: List<String>) : UploadProgressState()
    data class Error(val exception: Throwable) : UploadProgressState()
}