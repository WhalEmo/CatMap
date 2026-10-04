package com.beem.catmap.ui.manager

import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.NeighborhoodBadgeModel

data class BadgeCelebrationPayload(
    val badge: NeighborhoodBadgeModel,
    val unlockedTier: BadgeTier,
    val isFirstUnlock: Boolean
)