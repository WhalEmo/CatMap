package com.beem.catmap.data.session
import android.content.Context
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.gonderi.ProfileState
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class CurrentUserManager private constructor(context: Context) {

    private val sessionManager = UserSessionManager.getInstance(context)
    private val blockSessionManager = BlockSessionManager.getInstance(context)

    // Manager yaşam döngüsüne bağlı coroutine scope
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // --- TEK VERİ KAYNAĞI (SINGLE SOURCE OF TRUTH) ---
    private val _currentUserState = MutableStateFlow(
        if (FirebaseAuth.getInstance().currentUser != null) {
            sessionManager.getUserSession() ?: Kullanici()
        } else {
            Kullanici()
        }
    )
    val currentUserState: StateFlow<Kullanici> = _currentUserState.asStateFlow()

    // profileState, currentUserState'teki değişikliklerde ad, soyad ve kullaniciAdi ile otomatik güncellenir
    val profileState: StateFlow<ProfileState> = _currentUserState.map { user ->
        ProfileState(
            ad = user.ad,
            soyad = user.soyad,
            kullaniciAdi = user.kullaniciAdi,
            takipciSayisi = user.takipciSayisi ?: 0L,
            takipEdilenSayisi = user.takipEdilenSayisi ?: 0L,
            gonderiSayisi = user.gonderiSayisi ?: 0L,
            biyografi = user.biyografi,
            fotoUrl = user.fotoUrl
        )
    }.stateIn(
        scope = managerScope,
        started = SharingStarted.Eagerly,
        initialValue = ProfileState()
    )

    private val _benimEngellediklerimState = MutableStateFlow(blockSessionManager.getBenimEngellediklerim())
    val benimEngellediklerimState: StateFlow<List<String>> = _benimEngellediklerimState.asStateFlow()

    private var blockedUsersLoaded = false

    companion object {
        @Volatile
        private var INSTANCE: CurrentUserManager? = null

        fun getInstance(context: Context): CurrentUserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrentUserManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    // --- KULLANICI ERİŞİM VE GÜNCELLEME İŞLEMLERİ ---

    fun getCurrentUser(): Kullanici {
        if (FirebaseAuth.getInstance().currentUser == null) {
            clearLocalCache()
            return Kullanici()
        }
        return _currentUserState.value
    }

    fun getCurrentUserId(): String {
        return getCurrentUser().id
    }

    fun setCurrentUser(kullanici: Kullanici) {
        sessionManager.saveUserSession(kullanici)
        _currentUserState.value = kullanici
    }

    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null && sessionManager.isLoggedIn()
    }

    /**
     * Kullanıcı nesnesinin belirli alanlarını güvenli bir şekilde güncellemek için yardımcı metot.
     */
    fun updateCurrentUser(updateBlock: (Kullanici) -> Kullanici) {
        val currentUser = _currentUserState.value
        val updatedUser = updateBlock(currentUser)
        setCurrentUser(updatedUser)
    }

    // --- ALAN BAZLI GÜNCELLEMELER ---

    fun updateProfileDetails(
        ad: String? = null,
        soyad: String? = null,
        kullaniciAdi: String? = null,
        takipci: Long? = null,
        takipEdilen: Long? = null,
        gonderiSayisi: Long? = null,
        biyografi: String? = null,
        fotoUrl: String? = null
    ) {
        updateCurrentUser { user ->
            user.copy(
                ad = ad ?: user.ad,
                soyad = soyad ?: user.soyad,
                kullaniciAdi = kullaniciAdi ?: user.kullaniciAdi,
                takipciSayisi = takipci ?: user.takipciSayisi,
                takipEdilenSayisi = takipEdilen ?: user.takipEdilenSayisi,
                gonderiSayisi = gonderiSayisi ?: user.gonderiSayisi,
                biyografi = biyografi ?: user.biyografi,
                fotoUrl = fotoUrl ?: user.fotoUrl
            )
        }
    }

    fun updateFollowCounts(takipciSayisi: Long, takipEdilenSayisi: Long) {
        updateCurrentUser { user ->
            user.copy(
                takipciSayisi = takipciSayisi,
                takipEdilenSayisi = takipEdilenSayisi
            )
        }
    }

    fun updateGonderiSayisi(gonderiSayisi: Long) {
        updateCurrentUser { user ->
            user.copy(gonderiSayisi = gonderiSayisi)
        }
    }

    fun updateBiyografi(biyografi: String) {
        updateCurrentUser { user ->
            user.copy(biyografi = biyografi)
        }
    }

    fun updateFotoUrl(fotoUrl: String) {
        updateCurrentUser { user ->
            user.copy(fotoUrl = fotoUrl)
        }
    }

    // --- ENGELLEME YÖNETİMİ ---

    fun isBlockedUsersLoaded(): Boolean = blockedUsersLoaded

    fun setBlockedUsersLoaded(loaded: Boolean) {
        blockedUsersLoaded = loaded
    }

    fun updateBenimEngellediklerim(liste: List<String>) {
        blockSessionManager.saveBenimEngellediklerim(liste)
        _benimEngellediklerimState.value = liste
        blockedUsersLoaded = true
    }

    // --- LOGOUT / SIFIRLAMA ---

    fun logout() {
        FirebaseAuth.getInstance().signOut()
        clearLocalCache()
    }

    fun clearLocalCache() {
        sessionManager.clearSession()
        blockSessionManager.clearBlockCache()

        _currentUserState.value = Kullanici()
        _benimEngellediklerimState.value = emptyList()

        blockedUsersLoaded = false
    }
}