package com.beem.catmap.gonderi

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: ProfileRepository = ProfileRepository()

    private val userManager = CurrentUserManager.getInstance(application)
    val profileState: StateFlow<ProfileState> = userManager.profileState

    private val _userProfile = MutableStateFlow<UiState<UserProfileData>>(UiState.Idle)
    val userProfile: StateFlow<UiState<UserProfileData>> = _userProfile.asStateFlow()

    private val _profileUpdateState = MutableStateFlow<ProfileUpdateResult>(ProfileUpdateResult.Idle)
    val profileUpdateState: StateFlow<ProfileUpdateResult> = _profileUpdateState.asStateFlow()

    fun profilBilgileriniYukle(kullaniciId: String) {
        viewModelScope.launch {
            _userProfile.value = UiState.Loading

            val isMyProfile = kullaniciId == UserSession.userId
            if (isMyProfile) {
                val cachedUser = userManager.getCurrentUser()
                val cachedBiyografi = userManager.profileState.value.biyografi ?: ""

                Log.d("CACHED","ismyprofıle"+ cachedBiyografi)
                val localProfile = UserProfileData(
                    userId = kullaniciId,
                    kullaniciAdi = cachedUser.getKullaniciAdi() ?: "",
                    fotoUrl = cachedUser.getFotoUrl(),
                    hakkinda = cachedBiyografi
                )

                if (localProfile.kullaniciAdi.isNotBlank()) {
                    Log.d("CACHED","buraya mı girdi"+localProfile.kullaniciAdi)
                    _userProfile.value = UiState.Success(localProfile)
                    return@launch
                }
            }
            Log.d("CACHED","yok")
            fetchAndCacheProfileFromDb(kullaniciId, isMyProfile)
        }
    }

    private suspend fun fetchAndCacheProfileFromDb(kullaniciId: String, isMyProfile: Boolean) {
        val profileData = repository.getUserProfile(kullaniciId)

        if (profileData != null) {
            _userProfile.value = UiState.Success(profileData)
            Log.d("PROFILE_DEBUG", "userId=$kullaniciId")
            Log.d("PROFILE_DEBUG", "profileData=$profileData")

            if (isMyProfile) {
                userManager.updateBiyografi(profileData.hakkinda)
                val currentUser = userManager.getCurrentUser().apply {
                    setKullaniciAdi(profileData.kullaniciAdi)
                    profileData.fotoUrl?.let { setFotoUrl(it) }
                }
                userManager.setCurrentUser(currentUser)
            }
        } else {
            Log.d("PROFILE_DEBUG", "profileData NULL")
            _userProfile.value = UiState.Error("Kullanıcı bilgileri alınamadı.")
        }
    }

    // UI bileşeni olan 'uyari' nesnesi buradan çıkarıldı!
    fun tumProfilBilgileriniGuncelle(
        yeniKullaniciAdi: String,
        yeniHakkinda: String,
        yeniResimUri: Uri?,
        currentUserId: String
    ) {
        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateResult.Loading // Yükleniyor durumu eklendi

            val currentUser = userManager.getCurrentUser()
            val currentUsername = currentUser.getKullaniciAdi() ?: ""

            val result = repository.updateFullProfile(
                context = getApplication(),
                currentUserId = currentUserId,
                currentUsername = currentUsername,
                newUsername = yeniKullaniciAdi,
                newHakkinda = yeniHakkinda,
                newImageUri = yeniResimUri
            )

            if (result is ProfileUpdateResult.Success) {
                // 1. Önbellek Güncellemeleri
                currentUser.setKullaniciAdi(result.newUsername)
                result.newPhotoUrl?.let { currentUser.setFotoUrl(it) }
                userManager.setCurrentUser(currentUser)
                userManager.updateBiyografi(result.newHakkinda)

                // 2. StateFlow Güncellemesi
                val currentPhoto = (_userProfile.value as? UiState.Success)?.data?.fotoUrl
                _userProfile.value = UiState.Success(
                    UserProfileData(
                        userId = currentUserId,
                        kullaniciAdi = result.newUsername,
                        fotoUrl = result.newPhotoUrl ?: currentPhoto,
                        hakkinda = result.newHakkinda
                    )
                )
            }

            _profileUpdateState.value = result
        }
    }

    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateResult.Idle
    }
}