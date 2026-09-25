package com.beem.catmap.ui.manager

import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.model.SpotReport

sealed class FeedingSpotMapEvent {
    data class Created(val spot: FeedingSpot, val report: SpotReport) : FeedingSpotMapEvent()
    data class Updated(val spot: FeedingSpot, val report: SpotReport) : FeedingSpotMapEvent()
    data class Deleted(val spotId: String) : FeedingSpotMapEvent()
}