package com.beem.catmap.gonderi

data class ProfileUiState(
    val isSelfProfile: Boolean = false,       // Kendi profilimiz mi?
    val isFollowing: Boolean = false,         // Takip ediyor muyuz?
    val isFollowed: Boolean = false,
    val isLoadingFollowState: Boolean = false // Takip et butonu yükleniyor mu?
)

data class ProfileState(
    val takipciSayisi: Long = 0L,
    val takipEdilenSayisi: Long = 0L,
    val gonderiSayisi: Long = 0L,
    val biyografi: String = ""
)

data class UserProfileData(
    val userId: String,
    val kullaniciAdi: String = "",
    val fotoUrl: String? = null,
    val hakkinda: String = ""
)
