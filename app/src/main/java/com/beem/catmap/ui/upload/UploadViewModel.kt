package com.beem.catmap.ui.upload

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.CatMapApp
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.MapRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.manager.UploadProgressState
import com.beem.catmap.ui.manager.ImageUploadManager
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.security.Timestamp
import java.util.Date

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MapRepository.getInstance()
    private val postRepository = PostRepository.getInstance(application)


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

    fun addCatPostMyProfile(catId: String, onComplete: (Boolean) -> Unit) {
        if (catId.isBlank()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {

            postRepository.kullaniciGonderiKaydet(UserSession.userId, catId)
                .onSuccess {
                    onComplete(true)
                }
                .onFailure { exception ->
                    onComplete(false)
                }
        }
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
                    createdDocument = null,
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
                                createdDocument = progressState.catModel,
                                uploadedPhotoUrls = progressState.catModel.photoUri,
                                uploadStage = UploadStage.SUCCESS
                            )
                        }

                        CatEventBus.emitEvent(
                            CatMapEvent.Created(progressState.catModel)
                        )
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