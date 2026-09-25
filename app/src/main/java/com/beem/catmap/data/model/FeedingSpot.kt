package com.beem.catmap.data.model

import com.google.firebase.firestore.GeoPoint

data class FeedingSpot(
    var id: String = "", // Firestore Document ID
    val spotName: String = "",
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",
    val geohash: String = "",
    val coordinates: GeoPoint? = null,

    // Durum ve Zaman Takibi
    val currentStatus: String = SpotState.NEEDS_BOTH.name,
    val lastUpdatedAt: Long = System.currentTimeMillis(),
    val lastUpdatedBy: String = "",

    // Güvenlik ve Yaratıcı Bilgileri
    val creatorId: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isActive: Boolean = true, // Soft delete için
    val verificationCount: Int = 0,
    val reportAsFakeCount: Int = 0
)