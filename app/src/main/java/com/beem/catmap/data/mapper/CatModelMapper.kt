package com.beem.catmap.data.mapper

import com.beem.catmap.data.model.CatModel
import com.beem.catmap.data.model.Kediler
import com.beem.catmap.ui.map.model.CatClusterItem
import java.util.Date

// ==========================================
// 1. CatModel -> Diğerleri
// ==========================================

fun CatModel.toClusterItem(): CatClusterItem {
    return CatClusterItem(
        id = this.id,
        name = this.kediAdi.ifBlank { "İsimsiz Pati" },
        description = this.kediHakkinda,
        rawLatitude = this.latitude,
        rawLongitude = this.longitude,
        photoUrl = this.mainPhotoUrl,
        photoList = this.photoUri,
        uploaderId = this.YukleyenKullaniciID,
        createdAtMillis = this.createdAt?.time ?: System.currentTimeMillis(),
        city = this.city,
        district = this.district,
        neighborhood = this.neighborhood
    )
}

fun CatModel.toLegacyKediler(): Kediler {
    return Kediler(
        this.id,
        this.kediAdi,
        this.kediHakkinda,
        this.latitude,
        this.longitude,
        this.mainPhotoUrl,
        ArrayList(this.photoUri),
        this.YukleyenKullaniciID,
        this.createdAt ?: Date(), // NullPointerException kalkanı! Asla null gitmez.
        this.city,
        this.district,
        this.neighborhood
    )
}

// ==========================================
// 2. CatClusterItem -> Diğerleri (CRASH'İ ÇÖZEN KISIM)
// ==========================================

fun CatClusterItem.toLegacyKediler(): Kediler {
    // createdAtMillis 0 veya eksikse şu anki tarihi ver, asla null bırakma
    val safeDate = if (this.createdAtMillis > 0L) Date(this.createdAtMillis) else Date()

    return Kediler(
        this.id,
        this.name,
        this.description,
        this.rawLatitude,
        this.rawLongitude,
        this.photoUrl,
        ArrayList(this.photoList),
        this.uploaderId,
        safeDate,
        this.city,
        this.district,
        this.neighborhood
    )
}

fun CatClusterItem.toCatModel(): CatModel {
    val safeDate = if (this.createdAtMillis > 0L) Date(this.createdAtMillis) else Date()

    return CatModel(
        id = this.id,
        kediAdi = this.name,
        kediHakkinda = this.description,
        latitude = this.rawLatitude,
        longitude = this.rawLongitude,
        city = this.city,
        district = this.district,
        neighborhood = this.neighborhood,
        photoUri = this.photoList,
        YukleyenKullaniciID = this.uploaderId,
        createdAt = safeDate
    )
}

// ==========================================
// 3. Kediler (Java) -> Diğerleri
// ==========================================

fun Kediler.toClusterItem(): CatClusterItem {
    return CatClusterItem(
        id = this.id ?: "",
        name = this.isim ?: "İsimsiz Pati",
        description = this.hakkindasi ?: "",
        rawLatitude = this.latitude,
        rawLongitude = this.longitude,
        photoUrl = this.url ?: "",
        photoList = this.urLler ?: emptyList(),
        uploaderId = this.yukleyenId ?: "",
        createdAtMillis = this.createdAt?.time ?: System.currentTimeMillis(),
        city = this.city ?: "",
        district = this.district ?: "",
        neighborhood = this.neighborhood ?: ""
    )
}