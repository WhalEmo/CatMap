package com.beem.catmap.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.managers.OnlinePresenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

class ProfileSetupViewModel : ViewModel() {

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _loadingState = MutableStateFlow<String?>(null)
    val loadingState: StateFlow<String?> = _loadingState

    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent: SharedFlow<UiEvent> = _uiEvent

    /**
     * 🐾 Kullanıcı profilini mühürler.
     * [isAutoGenerateIfTaken] true ise ve kullanıcı adı kapılmışsa arkasına rastgele sayı takar.
     */
    fun completeProfile(
        userModel: UserModel?,
        preferredUsername: String,
        fullName: String,
        bio: String,
        isAutoGenerateIfTaken: Boolean = false
    ) {
        val uid = auth.currentUser?.uid ?: userModel?.id ?: return

        viewModelScope.launch {
            _loadingState.value = "Profiliniz doğrulanıyor..."

            try {
                var finalUsername = preferredUsername

                // 1. 🔍 Kullanıcı adı benzersizlik kontrolü
                var isUsernameTaken = checkIsUsernameTaken(finalUsername)

                if (isUsernameTaken) {
                    if (isAutoGenerateIfTaken) {
                        // 💨 Kullanıcı "Atla" dediyse ama isim kapıldıysa benzersiz isim üretene kadar dönecek
                        _loadingState.value = "Sizin için benzersiz bir ad oluşturuluyor..."
                        while (isUsernameTaken) {
                            val randomNumber = Random.nextInt(100, 9999)
                            finalUsername = "$preferredUsername$randomNumber"
                            isUsernameTaken = checkIsUsernameTaken(finalUsername)
                        }
                    } else {
                        // 🛑 Normal kayıtta kullanıcı adı kapılmışsa UI'a hata fırlat
                        _loadingState.value = null
                        _uiEvent.emit(UiEvent.Error("Bu kullanıcı adı zaten alınmış! 🙀"))
                        return@launch
                    }
                }
                
                _loadingState.value = "Profiliniz oluşturuluyor..."

                val nameParts = fullName.trim().split("\\s+".toRegex())
                val firstName = nameParts.firstOrNull() ?: ""
                val lastName = if (nameParts.size > 1) nameParts.drop(1).joinToString(" ") else ""

                // Kullanıcı nesnesini güncelle
                val updatedUserModel = (userModel ?: UserModel()).apply {
                    id = uid
                    username = finalUsername
                    name = firstName
                    surname = lastName
                    this.bio = bio
                    email = auth.currentUser?.email ?: email
                }

                // 🎯 Senkronize Map verisini çekiyoruz
                val userMap = updatedUserModel.KullaniciData()

                // Firestore'a kaydet
                firestore.collection("users").document(uid).set(userMap).await()

                _loadingState.value = null
                saveUserLocallyAndNavigate(updatedUserModel)

            } catch (e: Exception) {
                _loadingState.value = null
                _uiEvent.emit(UiEvent.Error("Bir hata oluştu: ${e.localizedMessage}"))
            }
        }
    }

    /**
     * Firestore'da kullanıcı adının varlığını kontrol eden asenkron yardımcı metot
     */
    private suspend fun checkIsUsernameTaken(username: String): Boolean {
        val query = firestore.collection("users")
            .whereEqualTo("KullaniciAdi", username)
            .get()
            .await()
        return !query.isEmpty
    }

    private fun saveUserLocallyAndNavigate(userModel: UserModel) {
        viewModelScope.launch {
            UserSession.update(userModel)
            OnlinePresenceManager.setUserOnline()
            _uiEvent.emit(UiEvent.Success)
        }
    }

    sealed class UiEvent {
        object Success : UiEvent()
        data class Error(val message: String) : UiEvent()
    }
}