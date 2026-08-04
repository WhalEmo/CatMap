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
    private val blockSessionManager = BlockSessionManager.getInstance(context) // <-- Eklendi

    private var currentUserCache: Kullanici? = null

    // --- STATEFLOW TANIMLAMALARI ---
    private val _profileState = MutableStateFlow(
        ProfileState(
            takipciSayisi = profileSessionManager.getTakipciSayisi(),
            takipEdilenSayisi = profileSessionManager.getTakipEdilenSayisi(),
            gonderiSayisi = profileSessionManager.getGonderiSayisi(),
            biyografi = profileSessionManager.getBiyografi()
        )
    )
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private val _benimEngellediklerimState = MutableStateFlow(blockSessionManager.getBenimEngellediklerim())
    val benimEngellediklerimState: StateFlow<List<String>> = _benimEngellediklerimState.asStateFlow()

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


    fun updateProfileDetails(
        takipci: Long,
        takipEdilen: Long,
        gonderiSayisi: Long = 0L,
        biyografi: String? = null
    ) {
        profileSessionManager.saveProfileDetails(
            takipciSayisi = takipci,
            takipEdilenSayisi = takipEdilen,
            gonderiSayisi = gonderiSayisi,
            biyografi = biyografi
        )

        _profileState.update { currentState ->
            currentState.copy(
                takipciSayisi = takipci,
                takipEdilenSayisi = takipEdilen,
                gonderiSayisi = gonderiSayisi,
                biyografi = biyografi ?: currentState.biyografi
            )
        }
    }

    fun updateFollowCounts(takipciSayisi: Long, takipEdilenSayisi: Long) {
        profileSessionManager.saveFollowCounts(takipciSayisi, takipEdilenSayisi)

        _profileState.update { currentState ->
            currentState.copy(
                takipciSayisi = takipciSayisi,
                takipEdilenSayisi = takipEdilenSayisi
            )
        }
    }

    fun updateGonderiSayisi(gonderiSayisi: Long) {
        profileSessionManager.saveGonderiSayisi(gonderiSayisi)

        _profileState.update { currentState ->
            currentState.copy(gonderiSayisi = gonderiSayisi)
        }
    }

    fun updateBiyografi(biyografi: String) {
        profileSessionManager.saveBiyografi(biyografi)

        _profileState.update { currentState ->
            currentState.copy(biyografi = biyografi)
        }
    }

    /**
     * Engellenenler listesini hem SharedPreferences'a yazar hem de Flow'u günceller.
     */
    private var blockedUsersLoaded = false

    fun isBlockedUsersLoaded(): Boolean = blockedUsersLoaded

    fun setBlockedUsersLoaded(loaded: Boolean) {
        blockedUsersLoaded = loaded
    }
    fun updateBenimEngellediklerim(liste: List<String>) {
        blockSessionManager.saveBenimEngellediklerim(liste)
        _benimEngellediklerimState.value = liste
        blockedUsersLoaded = true
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
        blockSessionManager.clearBlockCache()

        _profileState.value = ProfileState()
        _benimEngellediklerimState.value = emptyList()

        blockedUsersLoaded = false
    }
}