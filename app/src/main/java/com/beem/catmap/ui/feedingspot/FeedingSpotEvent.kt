package com.beem.catmap.ui.feedingspot

import com.beem.catmap.data.model.FeedingSpot

sealed class FeedingSpotEvent {
    data class OpenUpdateForm(val spot: FeedingSpot) : FeedingSpotEvent()
    data class OpenCreateForm(val lat: Double, val lng: Double) : FeedingSpotEvent()
    data class ShowToast(val message: String) : FeedingSpotEvent()
    data class OpenUserProfile(val userId: String): FeedingSpotEvent()
}