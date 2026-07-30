package com.beem.catmap.gonderi
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.UyariMesaji
import com.beem.catmap.data.session.CurrentUserManager
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val userManager = CurrentUserManager.getInstance(application)
    val profileState: StateFlow<ProfileState> = userManager.profileState
    private val repository: ProfileRepository = ProfileRepository()

    private val _url = MutableLiveData<String?>()
    val url: LiveData<String?> get() = _url
    private val _uploadState = MutableLiveData<Boolean>()
    val uploadState: LiveData<Boolean> get() = _uploadState
    private val _kullaniciAdi = MutableLiveData<String?>()
    val kullaniciAdi: LiveData<String?> get() = _kullaniciAdi

    private val _usernameUpdateState = MutableLiveData<UsernameUpdateResult>()
    val usernameUpdateState: LiveData<UsernameUpdateResult> get() = _usernameUpdateState

    fun profilFotoUrlGetirVeCachele(context: Context, kullaniciId: String) {
        _url.value = null
        viewModelScope.launch {
            val photoUrl = repository.getProfilePhotoUrl(context, kullaniciId)
            _url.postValue(photoUrl)
        }
    }

    fun profilFotoUrlKaydetFirebaseVeCachele(imageUri: Uri, context: Context, currentUserId: String) {
        viewModelScope.launch {
            val result = repository.saveProfilePhotoUrl(imageUri, context, currentUserId)
            result.onSuccess { newUrl ->
                _url.postValue(newUrl)
                _uploadState.postValue(true)
            }.onFailure {
                _uploadState.postValue(false)
            }
        }
    }

    // Kullanıcı adını Firestore'a kaydet ve güncelle
    fun kullaniciAdiKaydet(
        kullaniciAdi: String,
        context: Context,
        currentUserId: String,
        currentUserManager: CurrentUserManager
        uyari: UyariMesaji
    ) {
        uyari.YuklemeDurum("Kaydediliyor...")

        viewModelScope.launch {
            val result = repository.updateUsername(context, kullaniciAdi, currentUserId)
            _usernameUpdateState.value = result

            when (result) {
                is UsernameUpdateResult.Success -> {
                    val user = currentUserManager.getCurrentUser()
                    user?.setKullaniciAdi(kullaniciAdi)
                    currentUserManager.setCurrentUser(user)

                    _kullaniciAdi.postValue(kullaniciAdi)
                    uyari.BasariliDurum("Güncelleme başarılı.", 1000)
                    uyari.DahaOnceAlinmisMi = false
                }
                is UsernameUpdateResult.AlreadyTaken -> {
                    uyari.BasarisizDurum("Bu kullanıcı adı daha önce alınmış", 1000)
                    uyari.DahaOnceAlinmisMi = true
                }
                is UsernameUpdateResult.Error -> {
                    uyari.BasarisizDurum("Bir hata oluştu", 1000)
                }
            }
        }
    }

    fun kullaniciAdiGetirDB(kullaniciId: String) {
        _kullaniciAdi.value = null
        viewModelScope.launch {
            val username = repository.getUsernameFromDb(kullaniciId)
            _kullaniciAdi.postValue(username)
        }
    }
}