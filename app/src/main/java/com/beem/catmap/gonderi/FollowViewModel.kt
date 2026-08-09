package com.beem.catmap.gonderi

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.session.CurrentUserManager
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FollowRepository.getInstance(application)
    private val userManager = CurrentUserManager.getInstance(application)

    private val _benimEngellediklerim = MutableStateFlow<List<String>>(emptyList())
    val benimEngellediklerim: StateFlow<List<String>> = _benimEngellediklerim.asStateFlow()

    private val _beniEngelleyenler = MutableStateFlow<List<String>>(emptyList())
    val beniEngelleyenler: StateFlow<List<String>> = _beniEngelleyenler.asStateFlow()

    private val _followUiState = MutableStateFlow(FollowUiState())
    val followUiState: StateFlow<FollowUiState> = _followUiState.asStateFlow()
    private var followJob: Job? = null

    private val _targetUserTakipEdilenSayisi = MutableStateFlow(0L)
    val targetUserTakipEdilenSayisi: StateFlow<Long> = _targetUserTakipEdilenSayisi.asStateFlow()

    private val _targetUserTakipciSayisi = MutableStateFlow(0L)
    val targetUserTakipciSayisi: StateFlow<Long> = _targetUserTakipciSayisi.asStateFlow()


    fun setupFromFullProfile(
        followerCount: Long,
        followingCount: Long,
        isSelf: Boolean,
        isFollowing: Boolean = false,
        isFollowed: Boolean = false
    ) {
    Log.d("ISSELF","buraya gırdı"+isSelf)
        _followUiState.update {
            it.copy(
                isSelfProfile = isSelf,
                isFollowing = isFollowing,
                isFollowed = isFollowed,
                isLoadingFollowState = false
            )
        }
            _targetUserTakipciSayisi.value = followerCount
            _targetUserTakipEdilenSayisi.value = followingCount
    }

    fun takipEt(
        takipEttiginId: String,
        currentUserId: String,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit
    ) {
        if (followJob?.isActive == true) return
        val previousFollowerCount = _targetUserTakipciSayisi.value
        _followUiState.update { it.copy(isFollowing = true) }
        _targetUserTakipciSayisi.value = previousFollowerCount + 1

        viewModelScope.launch {
            val result = repository.takipEt(
                currentUserId = currentUserId,
                targetUserId = takipEttiginId,
                myBlockedList = _benimEngellediklerim.value,
                blockedMeList = _beniEngelleyenler.value
            )

            result.onSuccess { followResult ->

                val currentProfile = userManager.profileState.value
                userManager.updateFollowCounts(
                    takipciSayisi = currentProfile.takipciSayisi,
                    takipEdilenSayisi = followResult.currentFollowingCount
                )

                _targetUserTakipciSayisi.value = followResult.targetFollowerCount
                onSuccess()
            }.onFailure {
                onFailure()
                _followUiState.update { it.copy(isFollowing = false) }
                _targetUserTakipciSayisi.value = previousFollowerCount
            }
        }
    }

    fun takiptenCikar(
        takiptenCiktiginId: String,
        currentUserId: String,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit
    ) {
        // Optimistic UI Güncellemesi
        val previousFollowerCount = _targetUserTakipciSayisi.value
        val newOptimisticCount = if (previousFollowerCount > 0) previousFollowerCount - 1 else 0L

        _followUiState.update { it.copy(isFollowing = false) }
        _targetUserTakipciSayisi.value = newOptimisticCount

        viewModelScope.launch {
            val result = repository.unfollowUser(
                currentUserId = currentUserId,
                targetUserId = takiptenCiktiginId
            )

            result.onSuccess { followResult ->
                val currentProfile = userManager.profileState.value
                userManager.updateFollowCounts(
                    takipciSayisi = currentProfile.takipciSayisi,
                    takipEdilenSayisi = followResult.currentFollowingCount
                )
                _targetUserTakipciSayisi.value = followResult.targetFollowerCount
                onSuccess()
            }.onFailure {
                onFailure()
                _followUiState.update { it.copy(isFollowing = true) }
                _targetUserTakipciSayisi.value = previousFollowerCount
            }
        }
    }

    fun takipcidenCikar(takipciId: String, currentUserId: String,  onSuccess: () -> Unit = {}, onFailure: () -> Unit) {
        val previousFollowedCount = _targetUserTakipEdilenSayisi.value
        val newOptimisticCount = if (previousFollowedCount > 0) previousFollowedCount - 1 else 0L

        _followUiState.update { it.copy(isFollowed = false) }
        _targetUserTakipEdilenSayisi.value = newOptimisticCount

        viewModelScope.launch {
            val result = repository.removeFollower(
                currentUserId = currentUserId,
                followerId = takipciId
            )

            result.onSuccess { removeResult ->
                val currentProfile = userManager.profileState.value
                userManager.updateFollowCounts(
                    takipciSayisi = removeResult.currentFollowerCount,
                    takipEdilenSayisi = currentProfile.takipEdilenSayisi
                )
                _targetUserTakipEdilenSayisi.value = removeResult.followerFollowingCount
                onSuccess()
            }.onFailure {
                onFailure()
                _followUiState.update { it.copy(isFollowed = true) }
                _targetUserTakipEdilenSayisi.value = previousFollowedCount
            }
        }
    }
}