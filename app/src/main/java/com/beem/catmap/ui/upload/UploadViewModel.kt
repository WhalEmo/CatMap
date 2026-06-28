package com.beem.catmap.ui.upload

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.repository.CatRepository
import com.beem.catmap.ui.manager.ImageUploadManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// MainActivity.kullanici ID'sine erişebilmek veya Context gereksinimleri için AndroidViewModel kullanıyoruz
class UploadViewModel(application: Application) : AndroidViewModel(application) {

    private val catRepository = CatRepository()

    private val _uiState = MutableStateFlow(UploadUiState())
    val uiState: StateFlow<UploadUiState> = _uiState.asStateFlow()

    init {
        // 🎯 MERKEZİ HAVUZU DİNLEME: Kamera veya Galeriden resim geldikçe
        // bu ekranın state'i otomatik güncellenecek!
        viewModelScope.launch {
            ImageUploadManager.selectedImages.collect { uris ->
                _uiState.update { it.copy(selectedImages = uris) }
            }
        }
    }

    /**
     * Kediyi haritaya kaydetme motoru
     */
    fun uploadCat(catName: String, catAbout: String, latitude: Double, longitude: Double, userId: String) {
        val currentImages = _uiState.value.selectedImages

        // 🛑 ÖN VALİDASYON KONTROLLERİ
        if (catName.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Lütfen kedi ismini giriniz!") }
            return
        }
        if (currentImages.isEmpty()) {
            _uiState.update { it.copy(errorMessage = "Lütfen en az bir fotoğraf ekleyiniz!") }
            return
        }
        if (latitude == 0.0 && longitude == 0.0) {
            _uiState.update { it.copy(errorMessage = "Konum alınamadı, lütfen izinleri kontrol edin!") }
            return
        }

        // 🚀 YÜKLEME SÜRECİNİ BAŞLAT
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null, isSuccess = false) }

            try {
                // Repository katmanına asenkron paslıyoruz, o arkada her şeyi halledip ID dönecek
                val docId = catRepository.uploadCatPost(
                    catName = catName.trim(),
                    catAbout = catAbout.trim(),
                    latitude = latitude,
                    longitude = longitude,
                    userId = userId,
                    imageUris = currentImages
                )

                ImageUploadManager.clearImages()

                _uiState.update {
                    it.copy(isLoading = false, isSuccess = true, createdDocumentId = docId)
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.message ?: "Yükleme sırasında bir hata oluştu!")
                }
            }
        }
    }

    // Durumu sıfırlamak için emniyet sibobu
    fun resetState() {
        _uiState.update { UploadUiState() }
    }
}