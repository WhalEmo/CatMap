package com.beem.catmap.ui.feedingspot

sealed class FeedingSpotUiState {
    object Idle : FeedingSpotUiState()
    data class Loading(val message: String) : FeedingSpotUiState()
    object Success : FeedingSpotUiState()
    data class Error(val message: String) : FeedingSpotUiState()
}