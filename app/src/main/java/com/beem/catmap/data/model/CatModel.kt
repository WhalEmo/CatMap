package com.beem.catmap.data.model

import java.util.Date

data class CatModel(
    val id: String = "",
    val kediAdi: String = "",
    val kediHakkinda: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",
    val photoUri: List<String> = emptyList(),
    val YukleyenKullaniciID: String = "",
    val geohash: String = "",
    val createdAt: Date? = null

) {
    val mainPhotoUrl: String
        get() = photoUri.firstOrNull() ?: ""
}