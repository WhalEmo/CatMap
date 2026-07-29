package com.beem.catmap.gonderi

data class ProfileUiState(
    val isSelfProfile: Boolean = false,       // Kendi profilimiz mi?
    val isFollowing: Boolean = false,         // Takip ediyor muyuz?
    val isFollowed: Boolean = false,
    val isLoadingFollowState: Boolean = false // Takip et butonu yükleniyor mu?
)