package com.beem.catmap.gonderi


data class ProfileState(
    val ad: String? = null,
    val soyad: String? = null,
    val kullaniciAdi: String? = null,
    val takipciSayisi: Long = 0L,
    val takipEdilenSayisi: Long = 0L,
    val gonderiSayisi: Long = 0L,
    val biyografi: String? = null,
    val fotoUrl: String? = null
)

data class UserProfileData(
    val userId: String = "",
    val kullaniciAdi: String = "",
    val ad: String = "",
    val soyad: String? = null,
    val fotoUrl: String? = null,
    val hakkinda: String = "",
    val takipciSayisi: Long = 0L,
    val takipEdilenSayisi: Long = 0L,
    val gonderiSayisi: Long = 0L
)
