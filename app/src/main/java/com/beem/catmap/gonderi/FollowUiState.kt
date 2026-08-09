package com.beem.catmap.gonderi


// UI'daki Butonların Anlık Durum Modeli
data class FollowUiState(
    val isSelfProfile: Boolean? = null,
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
    val isLoadingFollowState: Boolean = false,
)
