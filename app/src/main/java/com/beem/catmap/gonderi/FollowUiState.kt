package com.beem.catmap.gonderi


// UI'daki Butonların Anlık Durum Modeli
data class FollowUiState(
    val isSelfProfile: Boolean? = null,
    val isFollowing: Boolean = false,
    val isFollowed: Boolean = false,
    val isLoadingFollowState: Boolean = false,
    val isBlocked: Boolean = false, // Benim engellediğim durum
    val isBlockedBy: Boolean = false, // Beni engelleyen durum
)
