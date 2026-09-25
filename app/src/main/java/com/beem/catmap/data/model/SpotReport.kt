package com.beem.catmap.data.model

data class SpotReport(
    var id: String = "",
    val reporterId: String = "",
    val reporterName: String = "",
    val reportedAt: Long = System.currentTimeMillis(),
    val reportTags: List<String> = emptyList(),
    val photoUrl: String = "",
    val note: String = "",
    val reporterLat: Double = 0.0,
    val reporterLng: Double = 0.0
)