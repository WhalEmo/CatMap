package com.beem.catmap.gonderi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.model.FullProfileData
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.domain.usecase.GetProfileFullDataUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProfileRepository = ProfileRepository(application)
    private val postRepository :PostRepository = PostRepository(application)
    private val getProfileFullDataUseCase = GetProfileFullDataUseCase(
        profileRepository = repository,
        postRepository = postRepository,
    )

    // Artık Tek Kaynak (Single Source of Truth) Olarak _fullProfileState Kullanıyoruz
    private val _fullProfileState = MutableStateFlow<UiState<FullProfileData>>(UiState.Idle)
    val fullProfileState: StateFlow<UiState<FullProfileData>> = _fullProfileState.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateResult>(ProfileUpdateResult.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateResult> = _profileUpdateState.asStateFlow()

    fun tumProfilVerileriniYukle(targetUserId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _fullProfileState.value = UiState.Loading
            getProfileFullDataUseCase(targetUserId, forceRefresh)
                .onSuccess { fullData ->
                    _fullProfileState.value = UiState.Success(fullData)
                }
                .onFailure { exception ->
                    _fullProfileState.value = UiState.Error(exception.localizedMessage ?: "Profil yüklenemedi.")
                }
        }
    }

    fun lokalProfilVerisiniGuncelle(guncelKullanici: Kullanici) {
        val currentState = _fullProfileState.value
        if (currentState is UiState.Success) {
            val mevcutFullData = currentState.data
            val yeniProfileData = repository.updateLokalUserSession(guncelKullanici, mevcutFullData.profile)

            // FullProfileData içerisindeki profile nesnesini güncelleyip State'e yazıyoruz
            _fullProfileState.value = UiState.Success(
                mevcutFullData.copy(profile = yeniProfileData)
            )
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

        // Mevcut profil verisini _fullProfileState içerisinden alıyoruz
        val currentFullData = (_fullProfileState.value as? UiState.Success)?.data
        val currentProfile = currentFullData?.profile

        val currentUsername = currentProfile?.kullaniciAdi ?: ""
        val currentAd = currentProfile?.ad ?: ""
        val currentSoyad = currentProfile?.soyad ?: ""
        val currentBio = currentProfile?.hakkinda ?: ""

        val isUsernameChanged = yeniKullaniciAdi != currentUsername
        val isAdChanged = yeniAd != currentAd
        val isSoyadChanged = yeniSoyad != currentSoyad
        val isBioChanged = yeniHakkinda != currentBio
        val isImageChanged = yeniResimUri != null

        // Hiçbir değişiklik yoksa işlemi sonlandır
        if (!isUsernameChanged && !isAdChanged && !isSoyadChanged && !isBioChanged && !isImageChanged) {
            _profileUpdateState.value = ProfileUpdateResult.Success(
                newUsername = currentUsername,
                newAd = currentAd,
                newSoyad = currentSoyad,
                newHakkinda = currentBio,
                newPhotoUrl = currentProfile?.fotoUrl
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
                // Başarılı güncelleme sonrası mevcut FullProfileData nesnesini güncelliyoruz
                if (currentFullData != null && currentProfile != null) {
                    val guncellenmisProfileData = UserProfileData(
                        userId = currentUserId,
                        kullaniciAdi = result.newUsername,
                        ad = result.newAd,
                        soyad = result.newSoyad,
                        fotoUrl = result.newPhotoUrl ?: currentProfile.fotoUrl,
                        hakkinda = result.newHakkinda,
                    )

                    _fullProfileState.value = UiState.Success(
                        currentFullData.copy(profile = guncellenmisProfileData)
                    )
                }
            }
            _profileUpdateState.value = result
        }
    }

    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateResult.Idle
    }
}