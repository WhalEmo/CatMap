package com.beem.catmap.ui.camera

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.ui.manager.ImageUploadManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CameraUiEvent>()
    val uiEvent: SharedFlow<CameraUiEvent> = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            ImageUploadManager.selectedImages.collect { uris ->
                _uiState.update { it.copy(capturedImages = uris) }
            }
        }
    }


    fun exitPreviewMode() {
        _uiState.update {
            it.copy(activePreviewUri = null, currentMode = CameraMode.LIVE_PREVIEW)
        }
    }

    fun removeImageFromStrip(uri: Uri) {
        ImageUploadManager.removeImage(uri)
        if (_uiState.value.activePreviewUri == uri) {
            exitPreviewMode()
        }
    }

    fun deleteImageFromDevice(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                contentResolver.delete(uri, null, null)
                withContext(Dispatchers.Main) {
                    ImageUploadManager.removeImage(uri)
                    if (_uiState.value.activePreviewUri == uri) {
                        exitPreviewMode()
                    }
                    _uiEvent.emit(CameraUiEvent.ShowToast("Fotoğraf cihazdan tamamen silindi.", true))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiEvent.emit(CameraUiEvent.ShowToast("Fiziksel silme operasyonu başarısız!", false))
                }
            }
        }
    }


    fun deleteImage(contentResolver: ContentResolver, uri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (_uiState.value.activeImageSource == ImageSource.TEMP_CACHE) {
                    File(uri.path ?: "").delete()
                } else {

                    contentResolver.delete(uri, null, null)
                }

                withContext(Dispatchers.Main) {
                    ImageUploadManager.removeImage(uri)
                    exitPreviewMode()
                    _uiEvent.emit(CameraUiEvent.ShowToast("Fotoğraf tamamen silindi.", true))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiEvent.emit(CameraUiEvent.ShowToast("Silme işlemi başarısız!", false))
                }
            }
        }
    }

    fun saveTempImageToGallery(context: Context, uri: Uri, shouldKeepInStrip: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val timeStamp = System.currentTimeMillis()
                val filename = "CatMap_$timeStamp.jpg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CatMap")
                }

                val galleryUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("MediaStore kaydı başlatılamadı.")

                contentResolver.openInputStream(uri)?.use { inputStream ->
                    contentResolver.openOutputStream(galleryUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                withContext(Dispatchers.Main) {
                    ImageUploadManager.removeImage(uri)

                    if (shouldKeepInStrip) {
                        ImageUploadManager.addImage(galleryUri)
                        _uiEvent.emit(CameraUiEvent.ShowToast("Fotoğraf CatMap şeridine ve galeriye kaydedildi!", true))
                    } else {
                        _uiEvent.emit(CameraUiEvent.ShowToast("Fotoğraf sadece cihaz galerisine kaydedildi.", true))
                    }

                    File(uri.path ?: "").delete()
                    exitPreviewMode()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiEvent.emit(CameraUiEvent.ShowToast("Galeriye kaydetme başarısız oldu!", false))
                }
            }
        }
    }

    fun onPhotoCaptured(uri: Uri) {
        ImageUploadManager.addImage(uri)
        _uiState.update {
            it.copy(activePreviewUri = uri, currentMode = CameraMode.IMAGE_PREVIEW, activeImageSource = ImageSource.TEMP_CACHE)
        }
    }

    fun selectImageForPreview(uri: Uri, source: ImageSource = ImageSource.GALERI) {
        _uiState.update {
            it.copy(activePreviewUri = uri, currentMode = CameraMode.IMAGE_PREVIEW, activeImageSource = source)
        }
    }
}