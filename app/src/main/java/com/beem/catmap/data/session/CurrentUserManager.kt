package com.beem.catmap.data.session

import android.content.Context
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.model.ProfileState
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

    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val _currentUserModelState = MutableStateFlow(
        if (FirebaseAuth.getInstance().currentUser != null) {
            sessionManager.getUserSession() ?: UserModel()
        } else {
            UserModel()
        }
    )
    val currentUserModelState: StateFlow<UserModel> = _currentUserModelState.asStateFlow()

    // profileState, currentUserState'teki değişikliklerde ad, soyad ve kullaniciAdi ile otomatik güncellenir
    val profileState: StateFlow<ProfileState> = _currentUserModelState.map { user ->
        ProfileState(
            name = user.name,
            surname = user.surname,
            username = user.username,
            followersCount = user.followersCount ?: 0L,
            followingCount = user.followingCount ?: 0L,
            postCount = user.postCount ?: 0L,
            bio = user.bio,
            photoUrl = user.photoUrl
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

    // --- SAYAÇ ÖNBELLEK GEÇERLİLİK KONTROLÜ (STATS CACHE VALIDATION) ---

    fun isStatsCacheValid(timeoutMillis: Long = 2 * 60 * 1000L): Boolean {
        if (FirebaseAuth.getInstance().currentUser == null) return false
        val currentUser = _currentUserModelState.value
        if (currentUser.username.isNullOrBlank()) return false

        // Zaman damgasını RAM yerine sessionManager üzerinden kalıcı bellekten alıyoruz
        val lastFetchTime = sessionManager.getLastStatsFetchTime()
        if (lastFetchTime == 0L) return false

        val currentTime = System.currentTimeMillis()
        return (currentTime - lastFetchTime) < timeoutMillis
    }

    private fun markStatsFetched() {
        sessionManager.saveLastStatsFetchTime(System.currentTimeMillis())
    }

    // --- KULLANICI ERİŞİM VE GÜNCELLEME İŞLEMLERİ ---

    fun getCurrentUser(): UserModel {
        if (FirebaseAuth.getInstance().currentUser == null) {
            clearLocalCache()
            return UserModel()
        }
        return _currentUserModelState.value
    }

    fun getCurrentUserId(): String {
        return getCurrentUser().id
    }

    fun setCurrentUser(userModel: UserModel) {
        sessionManager.saveUserSession(userModel)
        _currentUserModelState.value = userModel
        markStatsFetched()
    }

    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null && sessionManager.isLoggedIn()
    }

    /**
     * Kullanıcı nesnesinin belirli alanlarını güvenli bir şekilde güncellemek için yardımcı metot.
     */
    fun updateCurrentUser(updateBlock: (UserModel) -> UserModel) {
        val currentUser = _currentUserModelState.value
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
                name = ad ?: user.name,
                surname = soyad ?: user.surname,
                username = kullaniciAdi ?: user.username,
                followersCount = takipci ?: user.followersCount,
                followingCount = takipEdilen ?: user.followingCount,
                postCount = gonderiSayisi ?: user.postCount,
                bio = biyografi ?: user.bio,
                photoUrl = fotoUrl ?: user.photoUrl
            )
        }
    }

    fun updateFollowCounts(takipciSayisi: Long, takipEdilenSayisi: Long) {
        updateCurrentUser { user ->
            user.copy(
                followersCount = takipciSayisi,
                followingCount = takipEdilenSayisi
            )
        }
        // Sayaçlar güncellendiği için zaman damgasını kaydet
        markStatsFetched()
    }

    fun updateGonderiSayisi(gonderiSayisi: Long) {
        updateCurrentUser { user ->
            user.copy(postCount = gonderiSayisi)
        }
    }

    fun updateBiyografi(biyografi: String) {
        updateCurrentUser { user ->
            user.copy(bio = biyografi)
        }
    }

    fun updateFotoUrl(fotoUrl: String) {
        updateCurrentUser { user ->
            user.copy(photoUrl = fotoUrl)
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

        _currentUserModelState.value = UserModel()
        _benimEngellediklerimState.value = emptyList()

        blockedUsersLoaded = false
    }
}