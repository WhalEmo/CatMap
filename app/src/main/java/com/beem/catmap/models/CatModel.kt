package com.beem.catmap.models

data class CatModel(
    val id: String = "",
    val kediAdi: String = "",
    val kediHakkinda: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val photoUri: List<String> = emptyList(),
    val YukleyenKullaniciID: String = "",
    val geohash: String = ""
) {
    val mainPhotoUrl: String
        get() = photoUri.firstOrNull() ?: ""
}