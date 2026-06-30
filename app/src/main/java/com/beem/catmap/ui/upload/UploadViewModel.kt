package com.beem.catmap.ui.upload

import android.app.Application
import android.content.Context
import android.location.Location
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.Maps.LocationEngine
import com.beem.catmap.repository.CatRepository
import com.beem.catmap.repository.UploadProgressState
import com.beem.catmap.ui.manager.ImageUploadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = CatRepository()

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ImageUploadManager.selectedImages.collect { uris ->
                _uiState.update { it.copy(selectedImages = uris) }
            }
        }
    }


    fun onProgressDialogDismissed() {
        _uiState.update { it.copy(isAllDone = true) }
    }


    fun uploadCat(
        catName: String,
        catAbout: String,
        location: Location?,
        userId: String
    ) {
        if (catName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen kediye bir isim veriniz!") }
            return
        }

        val selectedPhotos = ImageUploadManager.selectedImages.value
        if (selectedPhotos.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "En az bir kedi fotoğrafı eklemelisiniz!") }
            return
        }

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    uploadProgress = 0,
                    errorMessage = null,
                    isSuccess = false,
                    isAllDone = false,
                    createdDocumentId = null,
                    uploadStage = UploadStage.FETCHING_LOCATION
                )
            }

            if (location == null) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        uploadStage = UploadStage.ERROR,
                        errorMessage = "Cihazın konum bilgisi okunamadı! Lütfen GPS açın."
                    )
                }
                return@launch
            }

            _uiState.update { it.copy(uploadStage = UploadStage.UPLOADING_ASSETS) }

            repository.uploadCatPostWithProgress(
                catName = catName,
                catAbout = catAbout,
                latitude = location.latitude,
                longitude = location.longitude,
                userId = userId,
                imageUris = selectedPhotos
            ).collect { progressState ->

                when (progressState) {
                    is UploadProgressState.Loading -> {
                        _uiState.update {
                            it.copy(uploadProgress = progressState.progress)
                        }
                    }
                    is UploadProgressState.Success -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                uploadProgress = 100,
                                isUploadComplete = true,
                                isSuccess = true,
                                createdDocumentId = progressState.documentId,
                                uploadStage = UploadStage.SUCCESS
                            )
                        }
                    }
                    is UploadProgressState.Error -> {
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                uploadStage = UploadStage.ERROR,
                                errorMessage = progressState.exception.message ?: "Bilinmeyen bir hata oluştu!"
                            )
                        }
                    }
                }
            }
        }
    }

    fun resetState() {
        _uiState.update { UploadUiState() }
        ImageUploadManager.clearImages()
    }
}