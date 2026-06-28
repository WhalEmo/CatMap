package com.beem.catmap.repository

sealed class UploadProgressState {
    data class Loading(val progress: Int) : UploadProgressState()
    data class Success(val imageUrls: List<String>) : UploadProgressState()
    data class Error(val exception: Exception) : UploadProgressState()
}