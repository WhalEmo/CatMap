package com.beem.catmap.ui.profile.common

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.model.FullProfileData
import com.beem.catmap.data.model.ProfileState
import com.beem.catmap.data.model.ProfileUpdateResult
import com.beem.catmap.data.model.exception.UserBlockedException
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.data.repository.ProfileRepository
import com.beem.catmap.data.repository.UserBlockRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.auth.exceptions.IsBlockedByException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: ProfileRepository = ProfileRepository.Companion.getInstance()
    private val blockRepo: UserBlockRepository = UserBlockRepository.Companion.getInstance()
    private val postRepository: PostRepository = PostRepository.Companion.getInstance(application)
    private val followRepository: FollowRepository = FollowRepository.Companion.getInstance(application)
    private val userManager: CurrentUserManager = CurrentUserManager.Companion.getInstance(application)

    val profileState: StateFlow<ProfileState> = userManager.profileState

    private val getProfileFullDataUseCase = GetProfileFullDataUseCase(
        profileRepository = repository,
        postRepository = postRepository,
        followRepository = followRepository,
        userManager = userManager,
        userBlockRepository = blockRepo
    )

    private val _fullProfileState = MutableStateFlow<UiState<FullProfileData>>(UiState.Idle)
    val fullProfileState: StateFlow<UiState<FullProfileData>> = _fullProfileState.asStateFlow()

    private val _profileUpdateState =
        MutableStateFlow<ProfileUpdateResult>(ProfileUpdateResult.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateResult> = _profileUpdateState.asStateFlow()

    fun allProfileLoad(targetUserId: String, forceRefresh: Boolean = false) {
        if (targetUserId.isBlank()) return
        Log.d("SHIMMER", "tumppcalıstı")

        viewModelScope.launch {
            _fullProfileState.value = UiState.Loading

            getProfileFullDataUseCase(
                targetUserId = targetUserId,
                forceRefresh = forceRefresh
            )
                .onSuccess { fullData ->
                    Log.d("SHIMMER", "SUCCESS CALISTI")
                    if (fullData.isSelfProfile && forceRefresh) {
                        updateLocalSession(fullData.profile)
                    }

                    _fullProfileState.value = UiState.Success(fullData)
                }

                .onFailure { exception ->
                    when (exception) {
                        is UserBlockedException -> {
                            Log.d("ENGELLENENLERCLICK", "UserBlockedException")
                            _fullProfileState.value = UiState.Blocked(exception.profile)
                        }
                        is IsBlockedByException -> {
                            Log.d("ENGELLENENLERCLICK", "IsBlockedByException")
                            val publicUser = exception.publicProfile
                            val fallbackProfile = UserModel().apply {
                                id = targetUserId
                                username = publicUser?.username.orEmpty()
                                photoUrl = publicUser?.photoUrl.orEmpty()
                            }
                            _fullProfileState.value = UiState.BlockedBy(profile = fallbackProfile)
                        }
                        else -> {
                            Log.d("ENGELLENENLERCLICK", exception.localizedMessage )
                            _fullProfileState.value = UiState.Error(
                                exception.localizedMessage ?: "Profil yüklenemedi."
                            )
                        }
                    }
                }
        }
    }

    fun allProfileUpdate(
        yeniKullaniciAdi: String,
        yeniAd: String,
        yeniSoyad: String,
        yeniHakkinda: String,
        yeniResimUri: Uri?,
        currentUserId: String
    ) {
        if (_profileUpdateState.value is ProfileUpdateResult.Loading) return

        val currentFullData = (_fullProfileState.value as? UiState.Success)?.data
        val currentProfile = currentFullData?.profile ?: run {
            _profileUpdateState.value = ProfileUpdateResult.Error("Profil verisi henüz yüklenmedi.")
            return
        }

        val currentUsername = currentProfile.username.orEmpty()
        val currentAd = currentProfile.name.orEmpty()
        val currentSoyad = currentProfile.surname.orEmpty()
        val currentBio = currentProfile.bio.orEmpty()

        val isChanged = yeniKullaniciAdi != currentUsername ||
                yeniAd != currentAd ||
                yeniSoyad != currentSoyad ||
                yeniHakkinda != currentBio ||
                yeniResimUri != null

        if (!isChanged) {
            _profileUpdateState.value = ProfileUpdateResult.Success(
                newUsername = currentUsername,
                newName = currentAd,
                newSurname = currentSoyad,
                newBio = currentBio,
                newPhotoUrl = currentProfile.photoUrl
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
                val finalPhotoUrl = result.newPhotoUrl.takeIf { !it.isNullOrBlank() } ?: currentProfile.photoUrl

                // 1. Yeni profil modelini oluştur
                val guncellenmisProfileData = currentProfile.copy(
                    username = result.newUsername,
                    name = result.newName,
                    surname = result.newSurname,
                    photoUrl = finalPhotoUrl,
                    bio = result.newBio // Hakkında alanı kesin olarak güncelleniyor
                )

                // 2. Local Session'ı güncelle
                updateLocalSession(guncellenmisProfileData)

            }

            _profileUpdateState.value = result
        }
    }


    private fun updateLocalSession(userModel: UserModel) {
        userManager.updateProfileDetails(
            ad = userModel.name.orEmpty(),
            soyad = userModel.surname.orEmpty(),
            kullaniciAdi = userModel.username.orEmpty(),
            takipci = userModel.followersCount ?: 0L,
            takipEdilen = userModel.followingCount ?: 0L,
            gonderiSayisi = userModel.postCount ?: 0L,
            biyografi = userModel.bio.orEmpty(),
            fotoUrl = userModel.photoUrl
        )
        userManager.setCurrentUser(userModel)
    }

    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateResult.Idle
    }
    fun resetProfileState() {
        _fullProfileState.value = UiState.Idle
    }

    fun setBlockedState(fallbackProfile: UserModel? = null) {
        val currentState = _fullProfileState.value

        val currentProfile = when (currentState) {
            is UiState.Success -> currentState.data.profile
            is UiState.Blocked -> currentState.profile
            is UiState.BlockedBy -> currentState.profile
            else -> fallbackProfile
        }

        _fullProfileState.value = UiState.Blocked(profile = currentProfile)
    }


}