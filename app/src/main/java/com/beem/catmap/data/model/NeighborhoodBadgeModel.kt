package com.beem.catmap.data.model

import com.google.firebase.Timestamp

data class NeighborhoodBadgeModel(
    val badgeId: String = "",
    val city: String = "",
    val district: String = "",
    val neighborhood: String = "",
    val catCount: Long = 0,
    val unlockedAt: Timestamp? = null,
    val overrideTier: BadgeTier? = null // Repository'den 8 tier basarken kullanacağız
) {
    /**
     * Eğer özel bir Tier verilmişse onu kullanır, yoksa kedi sayısına göre otomatik bulur
     */
    val currentTier: BadgeTier
        get() = overrideTier ?: BadgeTier.getTierForCatCount(catCount)

    val titleResId: Int get() = currentTier.titleResId
    val shortDescriptionResId: Int get() = currentTier.shortDescriptionResId
    val storyResId: Int get() = currentTier.storyResId
    val tagsResId: Int get() = currentTier.tagsResId
    val iconResId: Int get() = currentTier.iconResId

    val displayName: String
        get() = listOf(district, neighborhood.ifBlank { city })
            .filter { it.isNotBlank() }
            .joinToString(" / ")

    /**
     * Bu spesifik kartın kilidinin açık olup olmadığını kontrol eder:
     * Kullanıcının kedi sayısı bu kartın temsil ettiği Tier eşiğini geçmiş mi?
     */
    val isUnlocked: Boolean
        get() = catCount >= currentTier.threshold

    val nextTier: BadgeTier?
        get() = BadgeTier.getNextTier(currentTier)

    val nextThreshold: Long?
        get() = currentTier.threshold

    val remainingCatCount: Long
        get() = (currentTier.threshold - catCount).coerceAtLeast(0L)

    /**
     * Progress bar için yüzde oranını (0 - 100) hesaplar.
     */
    val progressPercent: Int
        get() {
            if (currentTier.threshold <= 0L) return 100
            val percent = (catCount.toFloat() / currentTier.threshold.toFloat()) * 100
            return percent.toInt().coerceIn(0, 100)
        }

    val elementResId: Int
        get() = currentTier.elementResId
}