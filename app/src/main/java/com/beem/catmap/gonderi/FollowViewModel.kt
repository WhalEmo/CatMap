package com.beem.catmap.gonderi

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.session.CurrentUserManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: FollowRepository = FollowRepository(application)
    private val userManager = CurrentUserManager.getInstance(application)

    val profileState: StateFlow<ProfileState> = userManager.profileState

    private val _benimEngellediklerim = MutableStateFlow<List<String>>(emptyList())
    val benimEngellediklerim: StateFlow<List<String>> = _benimEngellediklerim.asStateFlow()

    private val _beniEngelleyenler = MutableStateFlow<List<String>>(emptyList())
    val beniEngelleyenler: StateFlow<List<String>> = _beniEngelleyenler.asStateFlow()

    private val _followUiState = MutableStateFlow(FollowUiState())
    val followUiState: StateFlow<FollowUiState> = _followUiState.asStateFlow()

    private val _targetUserTakipEdilenSayisi = MutableStateFlow(0L)
    val targetUserTakipEdilenSayisi: StateFlow<Long> = _targetUserTakipEdilenSayisi.asStateFlow()

    private val _targetUserTakipciSayisi = MutableStateFlow(0L)
    val targetUserTakipciSayisi: StateFlow<Long> = _targetUserTakipciSayisi.asStateFlow()

    fun profilDurumunuHazirla(targetUserId: String) {
        val currentUserId = UserSession.userId
        val isSelf = (targetUserId == currentUserId)

        _followUiState.update { it.copy(isSelfProfile = isSelf) }

        if (!isSelf) {
            kullaniciTakipDurumunuKontrolEt(targetUserId)
        }
    }

    private fun kullaniciTakipDurumunuKontrolEt(targetUserId: String) {
        viewModelScope.launch {
            _followUiState.update { it.copy(isLoadingFollowState = true) }

            val isFollowing = repository.isFollowing(targetUserId).getOrDefault(false)
            val isFollowed = repository.isFollowedBy(targetUserId).getOrDefault(false)

            _followUiState.update {
                it.copy(
                    isFollowing = isFollowing,
                    isFollowed = isFollowed,
                    isLoadingFollowState = false
                )
            }
        }
    }

    fun takipTakipciSayisiGetir(userId: String, forceRefresh: Boolean = false) {
        val isMyProfile = (userId == userManager.getCurrentUserId())

        viewModelScope.launch {
            val result = repository.fetchAndCacheFollowCounts(
                userId = userId,
                isMyProfile = isMyProfile,
                forceRefresh = forceRefresh
            )

            result.onSuccess { counts ->
                if (isMyProfile) {
                    userManager.updateFollowCounts(
                        takipciSayisi = counts.followerCount,
                        takipEdilenSayisi = counts.followingCount
                    )
                } else {
                    _targetUserTakipciSayisi.value = counts.followerCount
                    _targetUserTakipEdilenSayisi.value = counts.followingCount
                }
            }.onFailure {
                if (!isMyProfile) {
                    _targetUserTakipciSayisi.value = 0L
                    _targetUserTakipEdilenSayisi.value = 0L
                }
            }
        }
    }

    fun targetUserClearOrPrepare(targetUserId: String) {
        val cached = repository.getCachedTargetUserData(targetUserId)
        if (cached != null) {
            _targetUserTakipciSayisi.value = cached.followerCount
            _targetUserTakipEdilenSayisi.value = cached.followingCount
        } else {
            _targetUserTakipciSayisi.value = 0L
            _targetUserTakipEdilenSayisi.value = 0L
        }
    }

    fun takipEt(takipEttiginId: String, currentUserId: String) {
        _followUiState.update { it.copy(isFollowing = true) }

        viewModelScope.launch {
            val result = repository.takipet(
                currentUserId = currentUserId,
                targetUserId = takipEttiginId,
                myBlockedList = _benimEngellediklerim.value,
                blockedMeList = _beniEngelleyenler.value
            )

            result.onSuccess {
                val currentProfile = userManager.profileState.value
                userManager.updateFollowCounts(
                    takipciSayisi = currentProfile.takipciSayisi,
                    takipEdilenSayisi = currentProfile.takipEdilenSayisi + 1
                )

                // Güncellenmiş sayıyı Repository önbelleğinden alıp UI'a yansıt
                repository.getCachedTargetUserData(takipEttiginId)?.let { cachedData ->
                    _targetUserTakipciSayisi.value = cachedData.followerCount
                }
            }.onFailure {
                _followUiState.update { it.copy(isFollowing = false) }
            }
        }
    }

    fun takiptenCikar(takiptenCiktiginId: String, currentUserId: String) {
        _followUiState.update { it.copy(isFollowing = false) }

        viewModelScope.launch {
            val result = repository.unfollowUser(
                currentUserId = currentUserId,
                targetUserId = takiptenCiktiginId
            )

            result.onSuccess {
                val currentProfile = userManager.profileState.value
                val yeniTakipEdilen = if (currentProfile.takipEdilenSayisi > 0) currentProfile.takipEdilenSayisi - 1 else 0L

                userManager.updateFollowCounts(
                    takipciSayisi = currentProfile.takipciSayisi,
                    takipEdilenSayisi = yeniTakipEdilen
                )

                // Güncellenmiş sayıyı Repository önbelleğinden alıp UI'a yansıt
                repository.getCachedTargetUserData(takiptenCiktiginId)?.let { cachedData ->
                    _targetUserTakipciSayisi.value = cachedData.followerCount
                }
            }.onFailure {
                _followUiState.update { it.copy(isFollowing = true) }
            }
        }
    }

    fun takipcidenCikar(takipciId: String, currentUserId: String) {
        viewModelScope.launch {
            val result = repository.removeFollower(
                currentUserId = currentUserId,
                followerId = takipciId
            )

            result.onSuccess {
                val currentProfile = userManager.profileState.value
                val yeniTakipci = if (currentProfile.takipciSayisi > 0) currentProfile.takipciSayisi - 1 else 0L

                userManager.updateFollowCounts(
                    takipciSayisi = yeniTakipci,
                    takipEdilenSayisi = currentProfile.takipEdilenSayisi
                )
            }
        }
    }
    // FollowViewModel.kt içerisine eklenecek metot:
    fun setupFromFullProfile(
        followerCount: Long,
        followingCount: Long,
        isSelf: Boolean
    ) {
        _followUiState.update { it.copy(isSelfProfile = isSelf) }

        if (isSelf) {
            // Kendi profilimizse sayıları CurrentUserManager zaten yönetiyor/senkronize ediyor
            userManager.updateFollowCounts(
                takipciSayisi = followerCount,
                takipEdilenSayisi = followingCount
            )
        } else {
            // Başka kullanıcı ise ViewModel state'lerini güncelliyoruz
            _targetUserTakipciSayisi.value = followerCount
            _targetUserTakipEdilenSayisi.value = followingCount

        }
    }
}