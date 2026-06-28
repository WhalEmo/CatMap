package com.beem.catmap.ui.manager

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ImageUploadManager {
    private const val MAX_IMAGE_COUNT = 5

    private val _selectedImages = MutableStateFlow<List<Uri>>(emptyList())
    val selectedImages: StateFlow<List<Uri>> = _selectedImages.asStateFlow()

    fun addImage(uri: Uri) {
        if (_selectedImages.value.size < MAX_IMAGE_COUNT) {
            _selectedImages.update { currentList ->
                currentList.toMutableList().apply { add(uri) }.toList()
            }
        }
    }

    fun addImages(newUris: List<Uri>) {
        val currentList = _selectedImages.value
        val remainingSpace = MAX_IMAGE_COUNT - currentList.size
        if (remainingSpace > 0) {
            val toAdd = newUris.take(remainingSpace)
            _selectedImages.update { list ->
                list.toMutableList().apply { addAll(toAdd) }.toList()
            }
        }
    }

    fun removeImage(uri: Uri) {
        if (_selectedImages.value.contains(uri)) {
            _selectedImages.update { currentList ->
                currentList.toMutableList().apply { remove(uri) }.toList()
            }
        }
    }

    fun clearImages() {
        _selectedImages.value = emptyList()
    }
}