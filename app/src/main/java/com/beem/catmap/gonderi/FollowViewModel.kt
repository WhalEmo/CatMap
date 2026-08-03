package com.beem.catmap.gonderi

import android.app.Application
import androidx.collection.LruCache
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

    private val repository: FollowRepository = FollowRepository()
    private val userManager = CurrentUserManager.getInstance(application)

    val profileState: StateFlow<ProfileState> = userManager.profileState

    private val targetUserCache = LruCache<String, TargetUserFollowData>(3)

    private val _benimEngellediklerim = MutableStateFlow<List<String>>(emptyList())
    val benimEngellediklerim: StateFlow<List<String>> = _benimEngellediklerim.asStateFlow()

    private val _beniEngelleyenler = MutableStateFlow<List<String>>(emptyList())
    val beniEngelleyenler: StateFlow<List<String>> = _beniEngelleyenler.asStateFlow()

    // Profil Butonlarının State'i
    private val _followUiState = MutableStateFlow(FollowUiState())
    val followUiState: StateFlow<FollowUiState> = _followUiState.asStateFlow()

    private val _targetUserTakipEdilenSayisi = MutableStateFlow<Long>(0L)
    val targetUserTakipEdilenSayisi: StateFlow<Long> = _targetUserTakipEdilenSayisi.asStateFlow()

    private val _targetUserTakipciSayisi = MutableStateFlow<Long>(0L)
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

        if (isMyProfile) {
            val currentProfile = userManager.profileState.value
            val hasCachedData = currentProfile.takipciSayisi > 0L || currentProfile.takipEdilenSayisi > 0L

            if (hasCachedData && !forceRefresh) {
                return
            }
        } else {
            val cachedData = targetUserCache.get(userId)
            if (cachedData != null && !forceRefresh) {
                _targetUserTakipciSayisi.value = cachedData.followerCount
                _targetUserTakipEdilenSayisi.value = cachedData.followingCount
                return
            }
        }

        viewModelScope.launch {
            val result = repository.fetchAndCacheFollowCounts(getApplication<Application>(), userId)

            result.onSuccess { counts ->
                if (isMyProfile) {
                    userManager.updateFollowCounts(
                        takipciSayisi = counts.followerCount,
                        takipEdilenSayisi = counts.followingCount
                    )
                } else {
                    _targetUserTakipciSayisi.value = counts.followerCount
                    _targetUserTakipEdilenSayisi.value = counts.followingCount

                    targetUserCache.put(
                        userId,
                        TargetUserFollowData(
                            followerCount = counts.followerCount,
                            followingCount = counts.followingCount
                        )
                    )
                }
            }.onFailure {
                if (!isMyProfile && targetUserCache.get(userId) == null) {
                    _targetUserTakipciSayisi.value = 0L
                    _targetUserTakipEdilenSayisi.value = 0L
                }
            }
        }
    }

    fun targetUserClearOrPrepare(targetUserId: String) {
        val cached = targetUserCache.get(targetUserId)
        if (cached != null) {
            _targetUserTakipciSayisi.value = cached.followerCount
            _targetUserTakipEdilenSayisi.value = cached.followingCount
        } else {
            _targetUserTakipciSayisi.value = 0L
            _targetUserTakipEdilenSayisi.value = 0L
        }
    }

    // Takip Et (Optimistic UI destekli)
    fun takipEt(takipEttiginId: String, currentUserId: String) {
        // Anında UI güncellemesi (Buton saniyesinde 'Takip Ediliyor'a geçer)
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

                val targetCache = targetUserCache.get(takipEttiginId)
                val newFollowerCount = (targetCache?.followerCount ?: _targetUserTakipciSayisi.value) + 1
                val newFollowingCount = targetCache?.followingCount ?: _targetUserTakipEdilenSayisi.value

                val updatedData = TargetUserFollowData(newFollowerCount, newFollowingCount)
                targetUserCache.put(takipEttiginId, updatedData)

                _targetUserTakipciSayisi.value = newFollowerCount
            }.onFailure {
                // Hata durumunda butonu eski haline (false) geri çek
                _followUiState.update { it.copy(isFollowing = false) }
            }
        }
    }

    // Takipten Çıkar (Optimistic UI destekli)
    fun takiptenCikar(takiptenCiktiginId: String, currentUserId: String) {
        // Anında UI güncellemesi (Buton saniyesinde 'Takip Et'e geçer)
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

                val targetCache = targetUserCache.get(takiptenCiktiginId)
                val currentCount = targetCache?.followerCount ?: _targetUserTakipciSayisi.value
                val newCount = if (currentCount > 0) currentCount - 1 else 0L
                val newFollowingCount = targetCache?.followingCount ?: _targetUserTakipEdilenSayisi.value

                targetUserCache.put(takiptenCiktiginId, TargetUserFollowData(newCount, newFollowingCount))
                _targetUserTakipciSayisi.value = newCount
            }.onFailure {
                // Hata durumunda butonu eski haline (true) geri çek
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
}