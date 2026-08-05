package com.beem.catmap.gonderi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.model.FullProfileData
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.domain.usecase.GetProfileFullDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProfileRepository = ProfileRepository(application)
    private val postRepository = PostRepository.getInstance(application)

    private val userManager = CurrentUserManager.getInstance(application)

    val profileState: StateFlow<ProfileState> = userManager.profileState
    private val getProfileFullDataUseCase = GetProfileFullDataUseCase(
        profileRepository = repository,
        postRepository = postRepository

    )

    private val _fullProfileState = MutableStateFlow<UiState<FullProfileData>>(UiState.Idle)
    val fullProfileState: StateFlow<UiState<FullProfileData>> = _fullProfileState.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateResult>(ProfileUpdateResult.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateResult> = _profileUpdateState.asStateFlow()

    fun tumProfilVerileriniYukle(targetUserId: String, forceRefresh: Boolean = false) {
        if (targetUserId.isBlank()) return

        viewModelScope.launch {
            _fullProfileState.value = UiState.Loading
            getProfileFullDataUseCase(targetUserId, forceRefresh)
                .onSuccess { fullData ->
                    _fullProfileState.value = UiState.Success(fullData)
                }
                .onFailure { exception ->
                    _fullProfileState.value = UiState.Error(
                        exception.localizedMessage ?: "Profil yüklenemedi."
                    )
                }
        }
    }

    /**
     * Dışarıdan (örneğin EventBus dinleyicisinden) gelen güncel kullanıcı
     * verisini UI State'e yansıtmak için kullanılır.
     */
    fun lokalProfilVerisiniGuncelle(guncelKullanici: Kullanici) {
        _fullProfileState.update { currentState ->
            if (currentState is UiState.Success) {
                UiState.Success(currentState.data.copy(profile = guncelKullanici))
            } else {
                currentState
            }
        }
    }

    fun tumProfilBilgileriniGuncelle(
        yeniKullaniciAdi: String,
        yeniAd: String,
        yeniSoyad: String,
        yeniHakkinda: String,
        yeniResimUri: Uri?,
        currentUserId: String
    ) {
        if (_profileUpdateState.value is ProfileUpdateResult.Loading) return

        val currentFullData = (_fullProfileState.value as? UiState.Success)?.data
        val currentProfile = currentFullData?.profile

        // Güvenlik Koruması: Eğer state yoksa işlemi başlatma
        if (currentProfile == null) {
            _profileUpdateState.value = ProfileUpdateResult.Error("Profil verisi henüz yüklenmedi.")
            return
        }

        val currentUsername = currentProfile.kullaniciAdi.orEmpty()
        val currentAd = currentProfile.ad.orEmpty()
        val currentSoyad = currentProfile.soyad.orEmpty()
        val currentBio = currentProfile.biyografi.orEmpty()

        val isUsernameChanged = yeniKullaniciAdi != currentUsername
        val isAdChanged = yeniAd != currentAd
        val isSoyadChanged = yeniSoyad != currentSoyad
        val isBioChanged = yeniHakkinda != currentBio
        val isImageChanged = yeniResimUri != null

        // Hiçbir değişiklik yapılmadıysa doğrudan mevcut değerlerle Success dön
        if (!isUsernameChanged && !isAdChanged && !isSoyadChanged && !isBioChanged && !isImageChanged) {
            _profileUpdateState.value = ProfileUpdateResult.Success(
                newUsername = currentUsername,
                newAd = currentAd,
                newSoyad = currentSoyad,
                newHakkinda = currentBio,
                newPhotoUrl = currentProfile.fotoUrl
            )
            return
        }

        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateResult.Loading

            val result = repository.updateFullProfile(
                currentUserId = currentUserId,
                currentUsername = currentUsername,
                newUsername = yeniKullaniciAdi,
                currentAd = currentAd,
                newAd = yeniAd,
                currentSoyad = currentSoyad,
                newSoyad = yeniSoyad,
                newHakkinda = yeniHakkinda,
                newImageUri = yeniResimUri
            )

            if (result is ProfileUpdateResult.Success) {
                val guncellenmisProfileData = currentProfile.copy(
                    kullaniciAdi = result.newUsername,
                    ad = result.newAd,
                    soyad = result.newSoyad,
                    fotoUrl = result.newPhotoUrl ?: currentProfile.fotoUrl,
                    biyografi = result.newHakkinda
                )

                // StateThread-Safe şekilde güncelleniyor
                _fullProfileState.update {
                    UiState.Success(currentFullData.copy(profile = guncellenmisProfileData))
                }
            }
            _profileUpdateState.value = result
        }
    }

    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateResult.Idle
    }
}