package com.beem.catmap.main

import com.beem.catmap.data.model.NeighborhoodBadgeModel

sealed class AppShellIntent {
    data class EquipBadge(val badgeModel: NeighborhoodBadgeModel) : AppShellIntent()
    object SignOut : AppShellIntent()
}