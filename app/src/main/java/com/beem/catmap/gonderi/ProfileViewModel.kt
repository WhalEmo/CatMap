package com.beem.catmap.gonderi

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.local.UserSession
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

    private val repository: ProfileRepository = ProfileRepository.getInstance()
    private val postRepository: PostRepository = PostRepository.getInstance(application)
    private val userManager: CurrentUserManager = CurrentUserManager.getInstance(application)

    val profileState: StateFlow<ProfileState> = userManager.profileState

    private val getProfileFullDataUseCase = GetProfileFullDataUseCase(
        profileRepository = repository,
        postRepository = postRepository
    )

    private val _fullProfileState = MutableStateFlow<UiState<FullProfileData>>(UiState.Idle)
    val fullProfileState: StateFlow<UiState<FullProfileData>> = _fullProfileState.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateResult>(ProfileUpdateResult.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateResult> = _profileUpdateState.asStateFlow()

    fun tumProfilVerileriniYukle(targetUserId: String, isFollowing: Boolean = false, forceRefresh: Boolean = false) {
        if (targetUserId.isBlank()) return

        viewModelScope.launch {
            val isMyProfile = targetUserId == UserSession.userId

            if (isMyProfile && !forceRefresh) {
                val cachedUser = userManager.getCurrentUser()
                if (!cachedUser.kullaniciAdi.isNullOrBlank()) {
                    val currentData = (_fullProfileState.value as? UiState.Success)?.data
                    if (currentData != null) {
                        _fullProfileState.value = UiState.Success(currentData.copy(profile = cachedUser))
                    }
                }
            }

            _fullProfileState.value = UiState.Loading
            getProfileFullDataUseCase(
                targetUserId = targetUserId,
                isFollowing = isFollowing,
                forceRefresh = forceRefresh
            )
                .onSuccess { fullData ->
                    if (isMyProfile) {
                        updateLocalSession(fullData.profile)
                    }
                    _fullProfileState.value = UiState.Success(fullData)
                }
                .onFailure { exception ->
                    _fullProfileState.value = UiState.Error(
                        exception.localizedMessage ?: "Profil yüklenemedi."
                    )
                }
        }
    }

    fun lokalProfilVerisiniGuncelle(guncelKullanici: Kullanici) {
        _fullProfileState.update { currentState ->
            if (currentState is UiState.Success) {
                UiState.Success(currentState.data.copy(profile = guncelKullanici))
            } else {
                currentState
            }
        }
        if (guncelKullanici.id == UserSession.userId) {
            updateLocalSession(guncelKullanici)
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
                val finalPhotoUrl = result.newPhotoUrl ?: currentProfile.fotoUrl
                val guncellenmisProfileData = currentProfile.copy(
                    kullaniciAdi = result.newUsername,
                    ad = result.newAd,
                    soyad = result.newSoyad,
                    fotoUrl = finalPhotoUrl,
                    biyografi = result.newHakkinda
                )

                // Yerel önbelleği güncelle
                updateLocalSession(guncellenmisProfileData)

                _fullProfileState.update {
                    UiState.Success(currentFullData.copy(profile = guncellenmisProfileData))
                }
            }
            _profileUpdateState.value = result
        }
    }

    private fun updateLocalSession(user: Kullanici) {
        userManager.updateProfileDetails(
            ad = user.ad.orEmpty(),
            soyad = user.soyad.orEmpty(),
            kullaniciAdi = user.kullaniciAdi.orEmpty(),
            takipci = user.takipciSayisi ?: 0L,
            takipEdilen = user.takipEdilenSayisi ?: 0L,
            gonderiSayisi = user.gonderiSayisi ?: 0L,
            biyografi = user.biyografi.orEmpty(),
            fotoUrl = user.fotoUrl
        )
        userManager.setCurrentUser(user)
    }

    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateResult.Idle
    }
}