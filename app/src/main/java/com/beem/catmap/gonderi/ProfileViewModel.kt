package com.beem.catmap.gonderi

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
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

    // ProfileViewModel.kt içine eklenecek:
    fun lokalProfilVerisiniGuncelle(guncelKullanici: Kullanici) {
        val currentState = _userProfile.value
        if (currentState is UiState.Success) {
            val eskiData = currentState.data

            // Kullanici nesnesinden gelen yeni değerleri alıyoruz (Null ise eski veriyi koruyoruz)
            val yeniKullaniciAdi = guncelKullanici.kullaniciAdi?.takeIf { it.isNotBlank() } ?: eskiData.kullaniciAdi
            val yeniAd = guncelKullanici.ad?.takeIf { it.isNotBlank() } ?: eskiData.ad
            val yeniSoyad = guncelKullanici.soyad ?: eskiData.soyad
            val yeniBio = guncelKullanici.biyografi ?: eskiData.hakkinda
            val yeniFotoUrl = guncelKullanici.fotoUrl?.takeIf { it.isNotBlank() } ?: eskiData.fotoUrl

            // 1. ViewModel'deki UI State'i doğrudan güncelliyoruz
            val yeniProfileData = UserProfileData(
                userId = eskiData.userId,
                kullaniciAdi = yeniKullaniciAdi,
                ad = yeniAd,
                soyad = yeniSoyad,
                fotoUrl = yeniFotoUrl,
                hakkinda = yeniBio
            )
            _userProfile.value = UiState.Success(yeniProfileData)

            // 2. Local Cache / UserManager nesnelerimizi de senkronize tutuyoruz
            userManager.updateBiyografi(yeniBio)
            val currentUser = userManager.getCurrentUser().apply {
                setKullaniciAdi(yeniKullaniciAdi)
                setAd(yeniAd)
                setSoyad(yeniSoyad)
                yeniFotoUrl?.let { setFotoUrl(it) }
            }
            userManager.setCurrentUser(currentUser)
        }
    }
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
                    ad = cachedUser.getAd() ?:"",
                    soyad = cachedUser.getSoyad(),
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
                    setAd(profileData.ad)
                    setSoyad(profileData.soyad)
                    profileData.fotoUrl?.let { setFotoUrl(it) }
                }
                userManager.setCurrentUser(currentUser)
            }
        } else {
            Log.d("PROFILE_DEBUG", "profileData NULL")
            _userProfile.value = UiState.Error("Kullanıcı bilgileri alınamadı.")
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
        // 1. Halihazırda güncelleme yapılıyorsa tekrar başlatma
        if (_profileUpdateState.value is ProfileUpdateResult.Loading) return

        // 2. Mevcut profil verilerini al
        val currentProfile = (_userProfile.value as? UiState.Success)?.data
        val currentUser = userManager.getCurrentUser()

        val currentUsername = currentProfile?.kullaniciAdi ?: currentUser.getKullaniciAdi() ?: ""
        val currentAd = currentProfile?.ad ?: currentUser.getAd() ?: ""
        val currentSoyad = currentProfile?.soyad ?: currentUser.getSoyad() ?: ""
        val currentBio = currentProfile?.hakkinda ?: userManager.profileState.value.biyografi ?: ""

        // 3. Değişiklik kontrolü: Herhangi bir alan değişmiş mi ya da yeni resim seçilmiş mi?
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
                newPhotoUrl = currentProfile?.fotoUrl
            )
            return
        }

        viewModelScope.launch {
            _profileUpdateState.value = ProfileUpdateResult.Loading

            try {
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
                    currentUser.setKullaniciAdi(result.newUsername)
                    currentUser.setAd(result.newAd)
                    currentUser.setSoyad(result.newSoyad)
                    result.newPhotoUrl?.let { currentUser.setFotoUrl(it) }

                    userManager.setCurrentUser(currentUser)
                    userManager.updateBiyografi(result.newHakkinda)

                    val currentPhoto = (_userProfile.value as? UiState.Success)?.data?.fotoUrl
                    _userProfile.value = UiState.Success(
                        UserProfileData(
                            userId = currentUserId,
                            kullaniciAdi = result.newUsername,
                            ad = result.newAd,
                            soyad = result.newSoyad,
                            fotoUrl = result.newPhotoUrl ?: currentPhoto,
                            hakkinda = result.newHakkinda
                        )
                    )
                }
                _profileUpdateState.value = result
            } catch (e: Exception) {
                _profileUpdateState.value = ProfileUpdateResult.Error(e.localizedMessage ?: "Bilinmeyen bir hata oluştu.")
            }
        }
    }
    fun resetUpdateState() {
        _profileUpdateState.value = ProfileUpdateResult.Idle
    }
}