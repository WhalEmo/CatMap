package com.beem.catmap.utils

import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.model.SpotState
import com.google.firebase.firestore.GeoPoint

object MockDataHelper {
    fun getMockSpots(): List<FeedingSpot> {
        return listOf(
            FeedingSpot(
                id = "mock_001",
                spotName = "Yazır Parkı Kedi Evi",
                currentStatus = SpotState.NEEDS_WATER.name,
                coordinates = GeoPoint(38.947400, 32.520150), // Sana çok yakın (Yaklaşık 15m)
                district = "Selçuklu",
                neighborhood = "Yazır"
            ),
            FeedingSpot(
                id = "mock_002",
                spotName = "Market Önü Su Kabı",
                currentStatus = SpotState.FULL.name,
                coordinates = GeoPoint(37.948500, 32.520118), // Biraz uzak (Yaklaşık 135m)
                district = "Selçuklu",
                neighborhood = "Yazır"
            ),
            FeedingSpot(
                id = "mock_003",
                spotName = "Çınar Altı Besleme Noktası",
                currentStatus = SpotState.NEEDS_BOTH.name,
                coordinates = GeoPoint(37.949500, 32.522000), // Oldukça uzak (Yaklaşık 300m+)
                district = "Selçuklu",
                neighborhood = "Yazır"
            )
        )
    }
}