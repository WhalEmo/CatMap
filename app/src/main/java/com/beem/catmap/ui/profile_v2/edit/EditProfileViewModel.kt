package com.beem.catmap.ui.profile_v2.edit

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.ProfileUpdateResult
import com.beem.catmap.data.model.UserProfileData
import com.beem.catmap.data.repository.ProfileRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class EditProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ProfileRepository.getInstance()
    private val userManager = CurrentUserManager.getInstance(application)

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<EditProfileEvent>()
    val event = _eventChannel.receiveAsFlow()

    private val currentUserId: String
        get() = UserSession.userId.orEmpty()

    init {
        loadCurrentProfile()
    }

    private fun loadCurrentProfile() {
        val session = userManager.getCurrentUser()

        if (session != null) {
            val initialData = UserProfileData(
                id = currentUserId,
                name = session.name,
                surname = session.surname,
                username = session.username,
                bio = session.bio,
                photoUrl = session.photoUrl.orEmpty(),
                followersCount = session.followersCount ?: 0,
                followingCount = session.followingCount ?: 0,
                postCount = session.postCount ?: 0
            )

            _uiState.update {
                it.copy(
                    initialUser = initialData,
                    username = initialData.username,
                    name = initialData.name,
                    surname = initialData.surname,
                    bio = initialData.bio,
                    currentPhotoUrl = initialData.photoUrl,
                    isLoading = false
                )
            }
        } else {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Oturum bilgisine ulaşılamadı.") }
        }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value, usernameError = null) }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onSurnameChange(value: String) {
        _uiState.update { it.copy(surname = value, surnameError = null) }
    }

    fun onBioChange(value: String) {
        _uiState.update { it.copy(bio = value) }
    }

    fun onImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun saveProfile() {
        val state = _uiState.value
        val initial = state.initialUser ?: return

        val trimmedUsername = state.username.trim()
        val trimmedName = state.name.trim()
        val trimmedSurname = state.surname.trim()
        val trimmedBio = state.bio.trim()

        var hasError = false
        var uError: String? = null
        var nError: String? = null
        var sError: String? = null

        if (trimmedUsername.isBlank()) {
            uError = "Kullanıcı adı boş bırakılamaz."
            hasError = true
        }
        if (trimmedName.isBlank()) {
            nError = "Ad boş bırakılamaz."
            hasError = true
        }
        if (trimmedSurname.isBlank()) {
            sError = "Soyad boş bırakılamaz."
            hasError = true
        }

        if (hasError) {
            _uiState.update {
                it.copy(usernameError = uError, nameError = nError, surnameError = sError)
            }
            return
        }

        // Herhangi bir değişiklik yapılmış mı kontrolü
        val isChanged = trimmedUsername != initial.username ||
                trimmedName != initial.name ||
                trimmedSurname != initial.surname ||
                trimmedBio != initial.bio ||
                state.selectedImageUri != null

        if (!isChanged) {
            viewModelScope.launch { _eventChannel.send(EditProfileEvent.SaveSuccess) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val result = repository.updateFullProfile(
                currentUserId = currentUserId,
                currentUsername = initial.username,
                newUsername = trimmedUsername,
                currentAd = initial.name,
                newAd = trimmedName,
                currentSoyad = initial.surname,
                newSoyad = trimmedSurname,
                newHakkinda = trimmedBio,
                newImageUri = state.selectedImageUri
            )

            when (result) {
                is ProfileUpdateResult.Success -> {
                    val finalPhotoUrl = result.newPhotoUrl.takeIf { !it.isNullOrBlank() } ?: initial.photoUrl

                    val updatedProfile = initial.copy(
                        username = result.newUsername,
                        name = result.newName,
                        surname = result.newSurname,
                        bio = result.newBio,
                        photoUrl = finalPhotoUrl
                    )

                    // 1. Session güncelle
                    syncSession(updatedProfile)

                    ProfileEventBus.emitEvent(
                        ProfileEvent.ProfileUpdated(
                            updatedUserModel = updatedProfile
                        )
                    )

                    _uiState.update { it.copy(isLoading = false) }
                    _eventChannel.send(EditProfileEvent.SaveSuccess)
                }
                is ProfileUpdateResult.UsernameAlreadyTaken -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            usernameError = "Bu kullanıcı adı zaten kullanılmakta."
                        )
                    }
                }
                is ProfileUpdateResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message
                        )
                    }
                }
                else -> {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun syncSession(profile: UserProfileData) {
        userManager.updateProfileDetails(
            ad = profile.name,
            soyad = profile.surname,
            kullaniciAdi = profile.username,
            takipci = profile.followersCount,
            takipEdilen = profile.followingCount,
            gonderiSayisi = profile.postCount,
            biyografi = profile.bio,
            fotoUrl = profile.photoUrl
        )
    }

    fun clearState() {
        _uiState.value = EditProfileUiState()
    }
}