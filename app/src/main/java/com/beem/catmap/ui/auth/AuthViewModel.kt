package com.beem.catmap.ui.auth

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.DogrulamaKodYonetici
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.managers.OnlinePresenceManager
import com.google.firebase.auth.FirebaseAuth
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
    private val authYonetici = DogrulamaKodYonetici()

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

        db.collection("users")
            .whereEqualTo("KullaniciAdi", username)
            .limit(1)
            .get()
            .addOnSuccessListener { query ->
                if (query.isEmpty) {
                    _uiState.value = AuthUiState.Error("Kullanıcı adı bulunamadı!")
                    return@addOnSuccessListener
                }

                val doc = query.documents[0]
                val user = Kullanici(username, password).apply {
                    kullaniciAdi = doc.getString("KullaniciAdi") ?: username
                    ad = doc.getString("Ad") ?: ""
                    soyad = doc.getString("Soyad") ?: ""
                    email = doc.getString("Email") ?: ""
                    fotoUrl = doc.getString("profilFotoUrl") ?: ""
                    biyografi = doc.getString("Hakkinda") ?: ""
                    takipEdilenSayisi = doc.getLong("TakipEdilenSayisi")
                    takipciSayisi = doc.getLong("takipciSayisi")
                    gonderiSayisi = doc.getLong("gonderiSayisi") ?: 0L
                    id = doc.id
                }

                if (user.email.isNullOrEmpty()) {
                    _uiState.value = AuthUiState.Error("Kullanıcı mail bilgisi eksik!")
                    return@addOnSuccessListener
                }

                authYonetici.girisYap(user.email, password) { basarili ->
                    if (basarili) {
                        OnlinePresenceManager.setUserOnline()
                        _uiState.value = AuthUiState.Success(user, "Giriş Başarılı!")
                        viewModelScope.launch {
                            _event.emit(AuthEvent.NavigateToMap)
                        }
                    } else {
                        _uiState.value = AuthUiState.Error("Şifre hatalı veya giriş başarısız!")
                    }
                }
            }
            .addOnFailureListener {
                _uiState.value = AuthUiState.Error("Bağlantı hatası oluştu!")
            }
    }

    fun setMode(mode: AuthMode) {
        _currentMode.value = mode
        _uiState.value = AuthUiState.Idle
    }

    // 📝 YENİ KAYIT
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

        db.collection("users").whereEqualTo("Email", user.email).get()
            .addOnSuccessListener { emailSonuc ->
                if (!emailSonuc.isEmpty) {
                    _uiState.value = AuthUiState.Error("Email ile daha önce kayıt yapılmış.")
                    return@addOnSuccessListener
                }

                db.collection("users").whereEqualTo("KullaniciAdi", user.kullaniciAdi).get()
                    .addOnSuccessListener { userSonuc ->
                        if (!userSonuc.isEmpty) {
                            _uiState.value = AuthUiState.Error("Bu kullanıcı adı zaten alınmış.")
                            return@addOnSuccessListener
                        }

                        authYonetici.kaydetSifreEmail(user.email, user.sifre) { basarili ->
                            if (basarili) {
                                val currentUid = FirebaseAuth.getInstance().currentUser?.uid
                                if (currentUid != null) {
                                    db.collection("users").document(currentUid)
                                        .set(user.KullaniciData())
                                        .addOnSuccessListener {
                                            user.id = currentUid
                                            OnlinePresenceManager.setUserOnline()
                                            _uiState.value = AuthUiState.Success(user, "Kayıt Başarılı!")
                                            viewModelScope.launch {
                                                _event.emit(AuthEvent.NavigateToMap)
                                            }
                                        }
                                        .addOnFailureListener {
                                            _uiState.value = AuthUiState.Error("Kayıt verisi yazılamadı!")
                                        }
                                }
                            } else {
                                _uiState.value = AuthUiState.Error("Email/Şifre kaydı başarısız!")
                            }
                        }
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

        authYonetici.sifreSifirla(email) { basarili ->
            if (basarili) {
                _uiState.value = AuthUiState.Idle
                viewModelScope.launch {
                    _event.emit(AuthEvent.ShowToast("Sıfırlama bağlantısı e-postanıza gönderildi."))
                }
            } else {
                _uiState.value = AuthUiState.Error("E-posta gönderilemedi!")
            }
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}