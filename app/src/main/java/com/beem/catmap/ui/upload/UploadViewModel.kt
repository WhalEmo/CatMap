package com.beem.catmap.ui.upload

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.local.location.LocationHelper
import com.beem.catmap.data.repository.MapRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.manager.UploadProgressState
import com.beem.catmap.ui.manager.image.ImageUploadManager
import com.beem.catmap.ui.manager.image.UploadSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = MapRepository.getInstance()
    private val postRepository = PostRepository.getInstance(application)

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    private var uploadJob: Job? = null

    init {
        viewModelScope.launch {
            ImageUploadManager.observeSession(UploadSession.GENERAL).collect { uris ->
                _uiState.update { it.copy(selectedImages = uris) }
            }
        }
    }

    fun addCatPostMyProfile(catId: String, onComplete: (Boolean) -> Unit) {
        if (catId.isBlank()) {
            onComplete(false)
            return
        }

        viewModelScope.launch {
            postRepository.userPostSave(UserSession.userId, catId)
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
        userId: String,
        locationHelper: LocationHelper
    ) {
        val trimmedName = catName.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen kediye bir isim veriniz!") }
            return
        }

        val selectedPhotos = ImageUploadManager.getImages(UploadSession.GENERAL)
        if (selectedPhotos.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "En az bir kedi fotoğrafı eklemelisiniz!") }
            return
        }

        uploadJob?.cancel()

        uploadJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    uploadProgress = 0,
                    errorMessage = null,
                    isSuccess = false,
                    isUploadComplete = false,
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

            try {

                // 📍 Adres çözümleme (Geocoder) işlemini IO thread'e alıyoruz (Performans için)
                val addressModel = withContext(Dispatchers.IO) {
                    locationHelper.getFormattedAddress(
                        latitude = location.latitude,
                        longitude = location.longitude
                    )
                }

                val city = addressModel?.city ?: ""
                val district = addressModel?.district ?: ""
                val neighborhood = addressModel?.neighborhood ?: ""

                _uiState.update { it.copy(uploadStage = UploadStage.UPLOADING_ASSETS) }

                repository.uploadCatPostWithProgress(
                    catName = trimmedName,
                    catAbout = catAbout.trim(),
                    latitude = location.latitude,
                    longitude = location.longitude,
                    userId = userId,
                    imageUris = selectedPhotos,
                    city = city,
                    district = district,
                    neighborhood = neighborhood
                ).catch { throwable ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            uploadStage = UploadStage.ERROR,
                            errorMessage = throwable.message ?: "Beklenmedik bir hata oluştu."
                        )
                    }
                }.collect { progressState ->

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
                                    errorMessage = progressState.exception.message
                                        ?: "Bilinmeyen bir hata oluştu!"
                                )
                            }
                        }
                    }
                }
            }catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        uploadStage = UploadStage.ERROR,
                        errorMessage = e.message ?: "İşlem sırasında bir hata meydana geldi."
                    )
                }
            }
        }
    }

    fun cancelUpload() {
        uploadJob?.cancel()
        uploadJob = null
        _uiState.update {
            it.copy(
                isLoading = false,
                uploadStage = UploadStage.IDLE,
                uploadProgress = 0,
                errorMessage = null
            )
        }
    }

    fun dismissStatus() {
        _uiState.update {
            it.copy(
                uploadStage = UploadStage.IDLE,
                errorMessage = null,
                isSuccess = false
            )
        }
    }

    fun resetState() {
        _uiState.update { UploadUiState() }
        ImageUploadManager.clearSession(UploadSession.GENERAL)
    }
}