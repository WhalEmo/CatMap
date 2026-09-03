package com.beem.catmap.ui.profile_v2.otherprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.UserProfileData
import com.beem.catmap.data.model.exception.UserBlockedByException
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.data.repository.ProfileRepository
import com.beem.catmap.data.repository.UserBlockRepository
import com.beem.catmap.ui.auth.exceptions.IsBlockedByException
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OtherProfileViewModel(
    application: Application,
    private val targetUserId: String
) : AndroidViewModel(application) {

    private val profileRepository = ProfileRepository.getInstance()
    private val postRepository = PostRepository.getInstance(application)
    private val followRepository = FollowRepository.getInstance(application)
    private val userBlockRepository = UserBlockRepository.getInstance()

    private val _uiState = MutableStateFlow(OtherProfileUiState())
    val uiState: StateFlow<OtherProfileUiState> = _uiState.asStateFlow()

    private var lastPostDoc: DocumentSnapshot? = null
    private val currentUserId: String
        get() = UserSession.userId.orEmpty()

    init {
        loadProfile(forceRefresh = false)
        observeEvents()
    }

    /**
     * Tüm verileri paralel (async) ve önbellekleri (Cache) gözeterek yükler.
     */
    fun loadProfile(forceRefresh: Boolean = false) {
        if (targetUserId.isBlank()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                // 1. ADIM: Ben bu kullanıcıyı engelledim mi? (UserBlockRepo önbelleğini kullanır)
                val amIBlocking = userBlockRepository.isUserBlocked(currentUserId, targetUserId)
                if (amIBlocking) {
                    loadBlockedByMeProfile()
                    return@launch
                }

                // 2. ADIM: Profili Firestore'dan çek (Bizi engellediyse PERMISSION_DENIED fırlatır)

                val profileResult = profileRepository.getUserProfileV2(targetUserId, forceRefresh = forceRefresh)

                profileResult.onSuccess { userProfile ->
                    coroutineScope {
                        val isFollowingDeferred = async {
                            followRepository.isFollowing(targetUserId, forceRefresh).getOrDefault(false)
                        }
                        val isFollowedByDeferred = async {
                            followRepository.isFollowedBy(targetUserId, forceRefresh).getOrDefault(false)
                        }

                        val isFollowing = isFollowingDeferred.await()
                        val isFollowedBy = isFollowedByDeferred.await()

                        // Takip durumuna göre buton modunu belirle
                        val followStatus = when {
                            isFollowing -> OtherFollowStatus.FOLLOWING
                            isFollowedBy -> OtherFollowStatus.FOLLOW_BACK
                            else -> OtherFollowStatus.NOT_FOLLOWING
                        }

                        // Gizli profil kuralı: Takip etmiyorsak gönderiler kilitli
                        val isAccessDenied = !isFollowing

                        _uiState.update {
                            it.copy(
                                user = userProfile,
                                followStatus = followStatus,
                                isMyFollower = isFollowedBy,
                                isBlockedByMe = false,
                                isBlockedByThem = false,
                                isAccessDenied = isAccessDenied,
                                isLoading = false
                            )
                        }

                        // Gönderilere erişim iznimiz varsa çek
                        if (!isAccessDenied) {
                            loadPosts(forceRefresh)
                        }
                    }
                }.onFailure { exception ->
                    if (exception is UserBlockedByException) {
                        loadBlockedByThemFallback()
                    } else {
                        _uiState.update {
                            it.copy(isLoading = false, errorMessage = exception.localizedMessage ?: "Profil yüklenemedi.")
                        }
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, errorMessage = e.localizedMessage ?: "Profil yüklenemedi.")
                }
            }
        }
    }

    /**
     * Benim engellediğim kullanıcının durumunu bağlar.
     */

    private suspend fun loadBlockedByMeProfile() {
        val result = profileRepository.getUserProfileV2(targetUserId)

        result.onSuccess { profile ->
            _uiState.update { current ->
                current.copy(
                    user = profile,
                    followStatus = OtherFollowStatus.BLOCKED_BY_ME,
                    isBlockedByMe = true,
                    isBlockedByThem = false,
                    isAccessDenied = true,
                    isLoading = false,
                    posts = emptyList(),
                    errorMessage = null
                )
            }
        }.onFailure { exception ->
            if (exception is UserBlockedByException) {

                val publicUserResult = profileRepository.getPublicUserProfile(targetUserId)
                val publicUser = publicUserResult.getOrNull()

                val fallbackProfile = UserProfileData(
                    id = targetUserId,
                    username = publicUser?.username.orEmpty(),
                    photoUrl = publicUser?.photoUrl.orEmpty()
                )

                _uiState.update { current ->
                    current.copy(
                        user = fallbackProfile,
                        followStatus = OtherFollowStatus.LOADING,
                        isBlockedByMe = true,
                        isBlockedByThem = true,
                        isAccessDenied = true,
                        isLoading = false,
                        posts = emptyList(),
                        errorMessage = null
                    )
                }
            } else {
                _uiState.update { current ->
                    current.copy(
                        isLoading = false,
                        errorMessage = "Engellenen profil yüklenemedi: ${exception.localizedMessage}"
                    )
                }
            }
        }
    }

    /**
     * Karşı taraf bizi engellediğinde (PERMISSION_DENIED) Public Users'tan veriyi getirir.
     */
    private suspend fun loadBlockedByThemFallback() {
        val publicUserResult = profileRepository.getPublicUserProfile(targetUserId).getOrNull()
        val fallbackData = UserProfileData(
            id = targetUserId,
            username = publicUserResult?.username.orEmpty(),
            photoUrl = publicUserResult?.photoUrl.orEmpty()
        )

        _uiState.update {
            it.copy(
                user = fallbackData,
                isBlockedByThem = true,
                isBlockedByMe = false,
                isAccessDenied = true,
                isLoading = false,
                posts = emptyList()
            )
        }
    }

    /**
     * Kullanıcının gönderilerini yükler (PostRepository Cache'i ile).
     */
    fun loadPosts(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isPostsLoading = true) }

            val result = postRepository.getUsersPost(
                userId = targetUserId,
                lastDocument = null,
                forceRefresh = forceRefresh
            )

            result.onSuccess { pageResult ->
                lastPostDoc = pageResult.lastDocument
                _uiState.update {
                    it.copy(
                        posts = pageResult.posts.distinctBy { p -> p.catId },
                        isPostsLoading = false,
                        isLastPage = pageResult.isLastPage
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isPostsLoading = false) }
            }
        }
    }


    fun loadMorePosts() {
        val currentState = _uiState.value
        if (currentState.isMoreLoading || currentState.isLastPage || currentState.isPostsLoading || currentState.isAccessDenied) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMoreLoading = true) }

            val result = postRepository.getUsersPost(
                userId = targetUserId,
                lastDocument = lastPostDoc,
                forceRefresh = false
            )

            result.onSuccess { pageResult ->
                lastPostDoc = pageResult.lastDocument
                _uiState.update { current ->
                    val combined = (current.posts + pageResult.posts).distinctBy { it.catId }
                    current.copy(
                        posts = combined,
                        isMoreLoading = false,
                        isLastPage = pageResult.isLastPage
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isMoreLoading = false) }
            }
        }
    }

    /**
     * Takip Etme İşlemi (Optimistic UI)
     */
    fun followUser() {
        if (_uiState.value.isActionLoading) return

        val previousFollowStatus = _uiState.value.followStatus
        val previousFollowers = _uiState.value.user?.followersCount ?: 0L

        // Hızlı UI yanıtı
        _uiState.update {
            it.copy(
                followStatus = OtherFollowStatus.FOLLOWING,
                isActionLoading = true,
                isAccessDenied = false,
                user = it.user?.copy(followersCount = previousFollowers + 1)
            )
        }

        viewModelScope.launch {
            val result = followRepository.takipEt(targetUserId)

            result.onSuccess { followResult ->
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        user = it.user?.copy(followersCount = followResult.targetFollowerCount)
                    )
                }
                loadPosts(forceRefresh = true)

                ProfileEventBus.emitEvent(
                    ProfileEvent.FollowUser(
                        userId = targetUserId,
                        kullaniciAdi = _uiState.value.user?.username.orEmpty(),
                        fotoUrl = _uiState.value.user?.photoUrl.orEmpty(),
                        operatorUserId = currentUserId
                    )
                )
            }.onFailure { exception ->
                if (exception is IsBlockedByException) {
                    // İşlem sırasında engellendiğimiz anlaşıldı -> Kilitli ekrana geçir
                    loadBlockedByThemFallback()
                } else {
                    _uiState.update {
                        it.copy(
                            followStatus = previousFollowStatus,
                            isActionLoading = false,
                            isAccessDenied = true,
                            user = it.user?.copy(followersCount = previousFollowers)
                        )
                    }
                }
            }
        }
    }

    /**
     * Takipten Çıkma İşlemi
     */
    fun unfollowUser() {
        if (_uiState.value.isActionLoading) return

        val previousFollowStatus = _uiState.value.followStatus
        val previousFollowers = _uiState.value.user?.followersCount ?: 0L
        val newStatus = if (_uiState.value.isMyFollower) OtherFollowStatus.FOLLOW_BACK else OtherFollowStatus.NOT_FOLLOWING

        _uiState.update {
            it.copy(
                followStatus = newStatus,
                isActionLoading = true,
                isAccessDenied = true,
                posts = emptyList(),
                user = it.user?.copy(followersCount = (previousFollowers - 1).coerceAtLeast(0L))
            )
        }

        viewModelScope.launch {
            val result = followRepository.unfollowUser(targetUserId)

            result.onSuccess { followResult ->
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        user = it.user?.copy(followersCount = followResult.targetFollowerCount)
                    )
                }
                ProfileEventBus.emitEvent(
                    ProfileEvent.UnFollowUser(userId = targetUserId, operatorUserId = currentUserId)
                )
            }.onFailure {
                // Rollback
                _uiState.update {
                    it.copy(
                        followStatus = previousFollowStatus,
                        isActionLoading = false,
                        isAccessDenied = false,
                        user = it.user?.copy(followersCount = previousFollowers)
                    )
                }
            }
        }
    }

    /**
     * Kullanıcıyı Engelle
     */
    fun blockUser() {
        val user = _uiState.value.user ?: return
        val state = _uiState.value

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }

            try {
                // 1. Repository Firestore ve Realtime DB kayıtlarını silip kendi oturum sayaçlarımızı düşer
                userBlockRepository.blockUser(currentUserId, user.toLegacyUserModel())

                // 2. Karşı tarafın sayaçlarını hesapla
                val wasFollowing = state.followStatus == OtherFollowStatus.FOLLOWING
                val wasFollower = state.isMyFollower

                val newFollowersCount = if (wasFollowing) {
                    (user.followersCount - 1).coerceAtLeast(0L)
                } else {
                    user.followersCount
                }

                val newFollowingCount = if (wasFollower) {
                    (user.followingCount - 1).coerceAtLeast(0L)
                } else {
                    user.followingCount
                }

                val updatedUser = user.copy(
                    followersCount = newFollowersCount,
                    followingCount = newFollowingCount
                )

                if (!state.isBlockedByThem) {
                    profileRepository.updateCachedUserProfile(targetUserId, updatedUser)
                } else {
                    profileRepository.invalidateProfileCache(targetUserId)
                }


                // 3. UI State'i güncelle
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        isBlockedByMe = true,
                        isMyFollower = false,
                        followStatus = if (state.isBlockedByThem) OtherFollowStatus.LOADING else OtherFollowStatus.BLOCKED_BY_ME,
                        isAccessDenied = true,
                        user = updatedUser,
                        posts = emptyList()
                    )
                }

                // 4. Profil ekranlarına ve listelere haber ver
                ProfileEventBus.emitEvent(
                    ProfileEvent.BlockedUser(
                        userId = user.id,
                        kullaniciAdi = user.username,
                        fotoUrl = user.photoUrl,
                        operatorUserId = currentUserId
                    )
                )
            } catch (e: Exception) {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    /**
     * Engeli Kaldır
     */
    fun unblockUser() {
        val currentUser = _uiState.value.user ?: return
        val wasBlockedByThem = _uiState.value.isBlockedByThem

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }

            try {
                // 1. Veritabanından engeli sil
                userBlockRepository.unblockUser(currentUserId, targetUserId)
                followRepository.clearUserCache(targetUserId)

                if (!wasBlockedByThem) {
                    profileRepository.updateCachedUserProfile(targetUserId, currentUser)
                } else {
                    profileRepository.invalidateProfileCache(targetUserId)
                }

                // 3. UI State'i doğrudan RAM'den anında uyandır
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        isBlockedByMe = false,
                        followStatus = OtherFollowStatus.NOT_FOLLOWING,
                        isAccessDenied = true, // Takip etmediğimiz için gönderiler kilitli kalır
                        user = currentUser
                    )
                }

                // 4. Diğer ekranlara haber ver
                ProfileEventBus.emitEvent(
                    ProfileEvent.UnblockedUser(userId = targetUserId, operatorUserId = currentUserId)
                )

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(isActionLoading = false, errorMessage = "Engel kaldırılamadı.")
                }
            }
        }
    }


    /**
     * Takipçiden Çıkar
     */
    fun removeFollower() {
        val user = _uiState.value.user ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isActionLoading = true) }

            val result = followRepository.removeFollower(targetUserId)
            result.onSuccess { removeResult ->
                _uiState.update {
                    it.copy(
                        isActionLoading = false,
                        isMyFollower = false,
                        user = user.copy(followingCount = removeResult.followerFollowingCount),
                        followStatus = if (it.followStatus == OtherFollowStatus.FOLLOW_BACK) {
                            OtherFollowStatus.NOT_FOLLOWING
                        } else {
                            it.followStatus
                        }
                    )
                }
                ProfileEventBus.emitEvent(
                    ProfileEvent.UnFollowerUser(userId = targetUserId, operatorUserId = currentUserId)
                )
            }.onFailure {
                _uiState.update { it.copy(isActionLoading = false) }
            }
        }
    }

    private fun observeEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                when (event) {
                    is ProfileEvent.UnblockedUser -> {
                        if (event.userId == targetUserId) {
                            _uiState.update {
                                it.copy(
                                    isBlockedByMe = false,
                                    followStatus = OtherFollowStatus.NOT_FOLLOWING,
                                    isAccessDenied = true
                                )
                            }
                        }
                    }
                    is ProfileEvent.BlockedUser -> {
                        if (event.userId == targetUserId) {
                            _uiState.update {
                                it.copy(
                                    isBlockedByMe = true,
                                    followStatus = OtherFollowStatus.BLOCKED_BY_ME,
                                    isAccessDenied = true,
                                    posts = emptyList()
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun UserProfileData.toLegacyUserModel(): com.beem.catmap.data.model.UserModel {
        return com.beem.catmap.data.model.UserModel().apply {
            id = this@toLegacyUserModel.id
            name = this@toLegacyUserModel.name
            surname = this@toLegacyUserModel.surname
            username = this@toLegacyUserModel.username
            bio = this@toLegacyUserModel.bio
            photoUrl = this@toLegacyUserModel.photoUrl
        }
    }
}