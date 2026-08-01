package com.beem.catmap.data.session

import android.content.Context
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.gonderi.ProfileState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import com.google.firebase.auth.FirebaseAuth

class CurrentUserManager private constructor(context: Context) {

    private val sessionManager = UserSessionManager.getInstance(context)
    private val profileSessionManager = ProfileSessionManager.getInstance(context)

    private var currentUserCache: Kullanici? = null

    // --- STATEFLOW TANIMLAMASI ---
    // Başlangıç değerlerini SharedPreferences'tan okuyarak yüklüyoruz
    private val _profileState = MutableStateFlow(
        ProfileState(
            takipciSayisi = profileSessionManager.getTakipciSayisi(),
            takipEdilenSayisi = profileSessionManager.getTakipEdilenSayisi(),
            gonderiSayisi = profileSessionManager.getGonderiSayisi(),
            biyografi = profileSessionManager.getBiyografi()
        )
    )
    // Dışarıya sadece okunabilir (Read-Only) StateFlow sunuyoruz
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    companion object {
        @Volatile
        private var INSTANCE: CurrentUserManager? = null

        fun getInstance(context: Context): CurrentUserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrentUserManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getCurrentUser(): Kullanici {
        if (FirebaseAuth.getInstance().currentUser == null) {
            clearLocalCache()
            return Kullanici()
        }

        if (currentUserCache == null) {
            currentUserCache = sessionManager.getUserSession()
        }
        return currentUserCache ?: Kullanici()
    }

    fun getCurrentUserId(): String {
        return getCurrentUser().id
    }

    fun setCurrentUser(kullanici: Kullanici) {
        this.currentUserCache = kullanici
        sessionManager.saveUserSession(kullanici)
    }

    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null && sessionManager.isLoggedIn()
    }

    // --- REAKTİF PROFİL GÜNCELLEME METOTLARI ---

    /**
     * Tüm profil istatistiklerini günceller. Hem SharedPreferences'a yazar hem Flow'a yayınlar.
     */
    fun updateProfileDetails(
        takipci: Long,
        takipEdilen: Long,
        gonderiSayisi: Long = 0L,
        biyografi: String? = null
    ) {
        // 1. SharedPreferences'ı güncelle
        profileSessionManager.saveProfileDetails(
            takipciSayisi = takipci,
            takipEdilenSayisi = takipEdilen,
            gonderiSayisi = gonderiSayisi,
            biyografi = biyografi
        )

        // 2. StateFlow'u güncelle (Abone olan tüm UI'lar anında tetiklenir)
        _profileState.update { currentState ->
            currentState.copy(
                takipciSayisi = takipci,
                takipEdilenSayisi = takipEdilen,
                gonderiSayisi = gonderiSayisi,
                biyografi = biyografi ?: currentState.biyografi
            )
        }
    }

    /**
     * Sadece takip/takipçi sayılarını anlık günceller.
     */
    fun updateFollowCounts(takipciSayisi: Long, takipEdilenSayisi: Long) {
        profileSessionManager.saveFollowCounts(takipciSayisi, takipEdilenSayisi)

        _profileState.update { currentState ->
            currentState.copy(
                takipciSayisi = takipciSayisi,
                takipEdilenSayisi = takipEdilenSayisi
            )
        }
    }

    /**
     * Sadece gönderi sayısını anlık günceller.
     */
    fun updateGonderiSayisi(gonderiSayisi: Long) {
        profileSessionManager.saveGonderiSayisi(gonderiSayisi)

        _profileState.update { currentState ->
            currentState.copy(gonderiSayisi = gonderiSayisi)
        }
    }

    /**
     * Sadece biyografiyi anlık günceller.
     */
    fun updateBiyografi(biyografi: String) {
        profileSessionManager.saveBiyografi(biyografi)

        _profileState.update { currentState ->
            currentState.copy(biyografi = biyografi)
        }
    }

    // --- ÇIKIŞ YAP (LOGOUT) ---

    fun logout() {
        FirebaseAuth.getInstance().signOut()

        clearLocalCache()
    }

    fun clearLocalCache() {
        currentUserCache = null
        sessionManager.clearSession()
        profileSessionManager.clearProfileCache()

        // Flow'u sıfırla
        _profileState.value = ProfileState()
    }
}