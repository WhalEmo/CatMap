package com.beem.catmap.ui.badge

import com.beem.catmap.data.model.NeighborhoodBadgeModel

sealed interface EquipBadgeState {

    data object Idle : EquipBadgeState

    data object Loading : EquipBadgeState

    data class Success(
        val badge: NeighborhoodBadgeModel
    ) : EquipBadgeState

    data class Error(
        val message: String
    ) : EquipBadgeState
}