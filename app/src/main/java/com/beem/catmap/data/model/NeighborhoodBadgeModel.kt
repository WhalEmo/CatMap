package com.beem.catmap.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class NeighborhoodBadgeModel(
    @DocumentId
    val badgeId: String = "",
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",
    val catCount: Long = 0,
    val unlockedAt: Timestamp? = null
) {

    val title: String
        get() = when {
            catCount >= CAT_GUARDIAN_THRESHOLD -> "Mahalle Pati Koruyucusu"
            catCount >= CAT_FRIEND_THRESHOLD -> "Mahalle Kedi Dostu"
            catCount >= CAT_EXPLORER_THRESHOLD -> "Mahalle Pati Kaşifi"
            else -> "Henüz Rozet Yok"
        }

    val displayName: String
        get() = listOf(district, neighborhood.ifBlank { city })
            .filter { it.isNotBlank() }
            .joinToString(" / ")

    val isUnlocked: Boolean
        get() = unlockedAt != null

    val nextThreshold: Long?
        get() = when {
            catCount < CAT_EXPLORER_THRESHOLD -> CAT_EXPLORER_THRESHOLD
            catCount < CAT_FRIEND_THRESHOLD -> CAT_FRIEND_THRESHOLD
            catCount < CAT_GUARDIAN_THRESHOLD -> CAT_GUARDIAN_THRESHOLD
            else -> null
        }

    companion object {
        const val CAT_EXPLORER_THRESHOLD = 1L
        const val CAT_FRIEND_THRESHOLD = 3L
        const val CAT_GUARDIAN_THRESHOLD = 5L
    }
}