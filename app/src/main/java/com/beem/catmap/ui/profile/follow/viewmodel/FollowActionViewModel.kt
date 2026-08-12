package com.beem.catmap.ui.profile.follow.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.auth.exceptions.IsBlockedByException
import com.beem.catmap.ui.profile.follow.state.FollowState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowActionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FollowRepository.Companion.getInstance(application)
    private val userManager = CurrentUserManager.Companion.getInstance(application)


    private val _followState = MutableStateFlow(FollowState())
    val followState: StateFlow<FollowState> = _followState.asStateFlow()
    private var followJob: Job? = null

    private val _targetUserFollowingCount = MutableStateFlow(0L)
    val targetUserFollowingCount: StateFlow<Long> = _targetUserFollowingCount.asStateFlow()

    private val _targetUserFollowersCount = MutableStateFlow(0L)
    val targetUserFollowersCount: StateFlow<Long> = _targetUserFollowersCount.asStateFlow()


    fun setupFromFullProfile(
        followerCount: Long,
        followingCount: Long,
        isSelf: Boolean,
        isFollowing: Boolean = false,
        isFollowed: Boolean = false,
        isBlocked: Boolean = false
    ) {
    Log.d("ISSELF","buraya gırdı"+isSelf)
        _followState.update {
            it.copy(
                isSelfProfile = isSelf,
                isFollowing = if (isBlocked) false else isFollowing,
                isFollowed = if (isBlocked) false else isFollowed,
                isBlocked = isBlocked,
                isLoadingFollowState = false,
            )
        }
            _targetUserFollowersCount.value = followerCount
            _targetUserFollowingCount.value = followingCount
    }

    fun takipEt(
        takipEttiginId: String,
        onSuccess: () -> Unit = {},
        onFailure: (Throwable) -> Unit = {}
    ) {
        Log.d("FOLLOW_DEBUG", "4. ViewModel: takipEt() tetiklendi. Job active mi? ${followJob?.isActive}")
        if (followJob?.isActive == true) return

        val previousFollowerCount = _targetUserFollowersCount.value
        _followState.update { it.copy(isFollowing = true) }
        _targetUserFollowersCount.value = previousFollowerCount + 1

        followJob = viewModelScope.launch {
            Log.d("FOLLOW_DEBUG", "5. ViewModel: Coroutine başladı, repository çağrılıyor.")
            val result = repository.takipEt(targetUserId = takipEttiginId)

            result.onSuccess { followResult ->
                Log.d("FOLLOW_DEBUG", "6. ViewModel: Result SUCCESS")
                val currentProfile = userManager.profileState.value
                userManager.updateFollowCounts(
                    takipciSayisi = currentProfile.followersCount,
                    takipEdilenSayisi = followResult.currentFollowingCount
                )

                _targetUserFollowersCount.value = followResult.targetFollowerCount
                onSuccess()
            }.onFailure { exception ->
                Log.e("FOLLOW_DEBUG", "6. ViewModel: Result FAILURE! Exception Class: ${exception.javaClass.name}, Message: ${exception.message}")

                val isBlockedBy = exception is IsBlockedByException
                Log.d("FOLLOW_DEBUG", "7. ViewModel: exception is IsBlockedByException -> $isBlockedBy")

                _followState.update {
                    it.copy(
                        isFollowing = false,
                        isFollowed = false,
                        isBlockedBy = isBlockedBy,
                        isLoadingFollowState = false
                    )
                }
                _targetUserFollowersCount.value = previousFollowerCount
                onFailure(exception)
            }
        }
    }
    fun takiptenCikar(
        takiptenCiktiginId: String,
        onSuccess: () -> Unit = {},
        onFailure: () -> Unit
    ) {
        // Optimistic UI Güncellemesi
        val previousFollowerCount = _targetUserFollowersCount.value
        val newOptimisticCount = if (previousFollowerCount > 0) previousFollowerCount - 1 else 0L

        _followState.update { it.copy(isFollowing = false) }
        _targetUserFollowersCount.value = newOptimisticCount

        viewModelScope.launch {
            val result = repository.unfollowUser(
                targetUserId = takiptenCiktiginId
            )

            result.onSuccess { followResult ->
                val currentProfile = userManager.profileState.value
                userManager.updateFollowCounts(
                    takipciSayisi = currentProfile.followersCount,
                    takipEdilenSayisi = followResult.currentFollowingCount
                )
                _targetUserFollowersCount.value = followResult.targetFollowerCount
                onSuccess()
            }.onFailure {
                onFailure()
                _followState.update { it.copy(isFollowing = true, isLoadingFollowState = false) }
                _targetUserFollowersCount.value = previousFollowerCount
            }
        }
    }
    fun setInitialFollowedState(isFollowed: Boolean) {
        _followState.update { currentState ->
            currentState.copy(
                isFollowed = isFollowed,
                isSelfProfile = false
            )
        }
    }


    fun takipcidenCikar(takipciId: String,  onSuccess: () -> Unit = {}, onFailure: () -> Unit) {
        val previousFollowedCount = _targetUserFollowingCount.value
        val newOptimisticCount = if (previousFollowedCount > 0) previousFollowedCount - 1 else 0L

        _followState.update { it.copy(isFollowed = false) }
        _targetUserFollowingCount.value = newOptimisticCount

        viewModelScope.launch {
            val result = repository.removeFollower(
                followerId = takipciId
            )

            result.onSuccess { removeResult ->
                val currentProfile = userManager.profileState.value
                userManager.updateFollowCounts(
                    takipciSayisi = removeResult.currentFollowerCount,
                    takipEdilenSayisi = currentProfile.followingCount
                )
                _targetUserFollowingCount.value = removeResult.followerFollowingCount
                onSuccess()
            }.onFailure {
                onFailure()
                _followState.update { it.copy(isFollowed = true, isLoadingFollowState = false) }
                _targetUserFollowingCount.value = previousFollowedCount
            }
        }
    }
    fun setBlockedState(isBlocked: Boolean) {

        _followState.update { currentState ->
            currentState.copy(
                isBlocked = isBlocked,
                isSelfProfile = false,
                isLoadingFollowState = false,
                isFollowing = false,
                isFollowed = false
            )
        }
    }
    fun applyBlockToTargetCounts(wasIWasFollowing: Boolean, wasHeWasFollowing: Boolean) {
        if (wasIWasFollowing) {
            val currentTakipci = _targetUserFollowersCount.value
            _targetUserFollowersCount.value = if (currentTakipci > 0) currentTakipci - 1 else 0L
        }

        if (wasHeWasFollowing) {
            val currentTakipEdilen = _targetUserFollowingCount.value
            _targetUserFollowingCount.value = if (currentTakipEdilen > 0) currentTakipEdilen - 1 else 0L
        }

    }


    /**
     * Karşı taraf bizi engellediğinde tetiklenecek durum.
     */
    fun setBlockedByState(isBlockedBy: Boolean) {
        _followState.update { currentState ->
            currentState.copy(
                isBlockedBy = isBlockedBy,
                isFollowing = if (isBlockedBy) false else currentState.isFollowing,
                isFollowed = if (isBlockedBy) false else currentState.isFollowed
            )
        }
    }
}