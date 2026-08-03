package com.beem.catmap.gonderi

data class TargetUserFollowData(
    val followerCount: Long,
    val followingCount: Long
)
// UI'daki Butonların Anlık Durum Modeli
data class FollowUiState(
    val isSelfProfile: Boolean = false,
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
    val isLoadingFollowState: Boolean = false
)
