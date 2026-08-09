package com.beem.catmap.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.DogrulamaKodYonetici
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.AuthRepository
import com.beem.catmap.managers.OnlinePresenceManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val mAuth = FirebaseAuth.getInstance()
    private val authYonetici = DogrulamaKodYonetici()

    private val repository = AuthRepository.getInstance()

    private val _currentMode = MutableStateFlow(AuthMode.LOGIN)
    val currentMode: StateFlow<AuthMode> = _currentMode.asStateFlow()

    // 🎯 StateFlow: UI durumunu tutar
    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    // ⚡ SharedFlow: Tek seferlik olayları fırlatır
    private val _event = MutableSharedFlow<AuthEvent>()
    val event: SharedFlow<AuthEvent> = _event.asSharedFlow()

    // 🔑 KULLANICI ADI İLE GİRİŞ
    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Lütfen tüm alanları doldurun!")
            return
        }

        _uiState.value = AuthUiState.Loading("Giriş Yapılıyor...")

        viewModelScope.launch {
            repository.login(username, password)
                .onSuccess { user ->
                    _uiState.value = AuthUiState.Success(user, "Giriş Başarılı!")
                    saveUserLocallyAndNavigate(user)
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(exception.localizedMessage ?: "Giriş başarısız!")
                }
        }
    }

    fun setMode(mode: AuthMode) {
        _currentMode.value = mode
        _uiState.value = AuthUiState.Idle
    }


    fun register(user: Kullanici) {
        if (!Patterns.EMAIL_ADDRESS.matcher(user.email).matches()) {
            _uiState.value = AuthUiState.Error("Lütfen geçerli bir email adresi giriniz!")
            return
        }
        if (user.sifre.length < 5) {
            _uiState.value = AuthUiState.Error("Lütfen şifreyi en az 5 haneli giriniz!")
            return
        }

        _uiState.value = AuthUiState.Loading("Kayıt Yapılıyor...")

        viewModelScope.launch {
            repository.register(user)
                .onSuccess { registeredUser ->
                    _uiState.value = AuthUiState.Success(registeredUser, "Kayıt Başarılı!")
                    saveUserLocallyAndNavigate(registeredUser)
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(exception.localizedMessage ?: "Kayıt başarısız!")
                }
        }
    }

    // 🔐 ŞİFRE SIFIRLAMA
    fun resetPassword(email: String) {
        if (email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            _uiState.value = AuthUiState.Error("Geçerli bir e-posta adresi giriniz!")
            return
        }

        _uiState.value = AuthUiState.Loading("Sıfırlama bağlantısı gönderiliyor...")

        viewModelScope.launch {
            repository.resetPassword(email)
                .onSuccess {
                    _uiState.value = AuthUiState.Idle
                    _event.emit(AuthEvent.ShowToast("Sıfırlama bağlantısı e-postanıza gönderildi."))
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(exception.localizedMessage ?: "E-posta gönderilemedi!")
                }
        }
    }

    fun firebaseAuthWithGoogle(idToken: String) {
        _uiState.value = AuthUiState.Loading("Google ile giriş yapılıyor...")

        viewModelScope.launch {
            repository.signInWithGoogle(idToken)
                .onSuccess { result ->
                    when (result) {
                        is GoogleAuthResult.ExistingUser -> {
                            _uiState.value = AuthUiState.Success(result.user, "Tekrar Hoş Geldin!")
                            saveUserLocallyAndNavigate(result.user)
                        }
                        is GoogleAuthResult.NewUser -> {
                            _uiState.value = AuthUiState.Idle
                            _uiState.value = AuthUiState.Success(result.user, "Google ile giriş başarılı!")
                            _event.emit(AuthEvent.NavigateToProfileSetup(result.user))
                        }
                    }
                }
                .onFailure { exception ->
                    _uiState.value = AuthUiState.Error(exception.localizedMessage ?: "Google ile giriş başarısız.")
                }
        }
    }

    private fun saveUserLocallyAndNavigate(user: Kullanici) {
        viewModelScope.launch {
            UserSession.update(user)
            OnlinePresenceManager.setUserOnline()
            _event.emit(AuthEvent.NavigateToMap)
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}