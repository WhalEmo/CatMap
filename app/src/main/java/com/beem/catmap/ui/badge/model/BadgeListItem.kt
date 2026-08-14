package com.beem.catmap.ui.badge.model

import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.NeighborhoodBadgeModel

sealed interface BadgeListItem {

    val stableId: String

    data class DistrictHeader(
        val district: String,
        val neighborhoodCount: Int
    ) : BadgeListItem {

        override val stableId: String
            get() = "district_$district"
    }

    data class NeighborhoodHeader(
        val city: String,
        val district: String,
        val neighborhood: String,
        val catCount: Long,
        val unlockedBadgeCount: Int,
        val currentTier: BadgeTier,
        val isExpanded: Boolean
    ) : BadgeListItem {

        override val stableId: String
            get() = "neighborhood_${city}_${district}_${neighborhood}"

        val totalBadgeCount: Int
            get() = BadgeTier.entries.size
    }

    data class BadgeItem(
        val badge: NeighborhoodBadgeModel
    ) : BadgeListItem {

        override val stableId: String
            get() = "badge_${badge.badgeId}_${badge.currentTier.level}"
    }
}