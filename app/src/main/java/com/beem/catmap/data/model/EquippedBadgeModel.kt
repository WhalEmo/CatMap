package com.beem.catmap.data.model

import com.google.firebase.Timestamp

data class EquippedBadgeModel(
    val neighborhoodBadgeId: String = "",
    val tierLevel: Int = 0,

    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",

    val equippedAt: Timestamp? = null
) {

    val tier: BadgeTier?
        get() = BadgeTier.entries.firstOrNull {
            it.level == tierLevel
        }

    val isValid: Boolean
        get() = neighborhoodBadgeId.isNotBlank() &&
                tierLevel > 0
}