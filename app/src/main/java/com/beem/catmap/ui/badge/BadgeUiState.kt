package com.beem.catmap.ui.badge

import com.beem.catmap.data.model.NeighborhoodBadgeModel

sealed interface BadgeUiState {
    data object Loading : BadgeUiState
    data class Success(val badges: List<NeighborhoodBadgeModel>) : BadgeUiState
    data class Error(val message: String) : BadgeUiState
}