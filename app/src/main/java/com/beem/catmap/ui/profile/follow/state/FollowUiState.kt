package com.beem.catmap.ui.profile.follow.state

import com.beem.catmap.data.model.UserModel

sealed interface FollowUiState {
    object Idle : FollowUiState
    object Loading : FollowUiState
    data class Success(
        val userModels: List<UserModel>,
        val isLastPage: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : FollowUiState
    data class Error(val message: String) : FollowUiState
}
data class FollowState(
    val isSelfProfile: Boolean? = null,
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
    val isLoadingFollowState: Boolean = false,
    val isBlocked: Boolean = false, // Benim engellediğim durum
    val isBlockedBy: Boolean = false, // Beni engelleyen durum
)