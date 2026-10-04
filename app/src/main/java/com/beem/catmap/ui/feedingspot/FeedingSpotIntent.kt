package com.beem.catmap.ui.feedingspot

sealed class FeedingSpotIntent {
    data class OpenUserProfile(val userId: String): FeedingSpotIntent()
}