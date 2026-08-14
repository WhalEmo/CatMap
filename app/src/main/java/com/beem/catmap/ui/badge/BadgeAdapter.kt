package com.beem.catmap.ui.badge

import android.content.res.ColorStateList
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R
import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.beem.catmap.databinding.ItemBadgeDistrictHeaderBinding
import com.beem.catmap.databinding.ItemBadgeNeighborhoodHeaderBinding
import com.beem.catmap.databinding.ItemNeighborhoodBadgeBinding
import com.beem.catmap.ui.badge.model.BadgeListItem
import com.beem.catmap.utils.withPossessiveSuffix
import java.util.Locale

class BadgeAdapter(
    private val onBadgeClick: (NeighborhoodBadgeModel) -> Unit,
    private val onNeighborhoodClick: (BadgeListItem.NeighborhoodHeader) -> Unit
) : ListAdapter<BadgeListItem, RecyclerView.ViewHolder>(
    BadgeDiffCallback()
) {

    // =========================================================
    // VIEW TYPES
    // =========================================================

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is BadgeListItem.DistrictHeader ->
                VIEW_TYPE_DISTRICT

            is BadgeListItem.NeighborhoodHeader ->
                VIEW_TYPE_NEIGHBORHOOD

            is BadgeListItem.BadgeItem ->
                VIEW_TYPE_BADGE
        }
    }

    // =========================================================
    // CREATE
    // =========================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)

        return when (viewType) {

            VIEW_TYPE_DISTRICT -> {
                val binding =
                    ItemBadgeDistrictHeaderBinding.inflate(
                        inflater,
                        parent,
                        false
                    )

                DistrictViewHolder(binding)
            }

            VIEW_TYPE_NEIGHBORHOOD -> {
                val binding =
                    ItemBadgeNeighborhoodHeaderBinding.inflate(
                        inflater,
                        parent,
                        false
                    )

                NeighborhoodViewHolder(binding)
            }

            VIEW_TYPE_BADGE -> {
                val binding =
                    ItemNeighborhoodBadgeBinding.inflate(
                        inflater,
                        parent,
                        false
                    )

                BadgeViewHolder(binding)
            }

            else -> {
                error("Bilinmeyen viewType: $viewType")
            }
        }
    }

    // =========================================================
    // BIND
    // =========================================================

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {
        when (
            val item = getItem(position)
        ) {

            is BadgeListItem.DistrictHeader -> {
                (holder as DistrictViewHolder)
                    .bind(item)
            }

            is BadgeListItem.NeighborhoodHeader -> {
                (holder as NeighborhoodViewHolder)
                    .bind(item)
            }

            is BadgeListItem.BadgeItem -> {
                (holder as BadgeViewHolder)
                    .bind(item.badge)
            }
        }
    }

    // =========================================================
    // DISTRICT
    // =========================================================

    inner class DistrictViewHolder(
        private val binding: ItemBadgeDistrictHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: BadgeListItem.DistrictHeader
        ) {
            val context = binding.root.context

            binding.apply {

                tvDistrictName.text =
                    item.district.uppercase(
                        TURKISH_LOCALE
                    )

                tvNeighborhoodCount.text =
                    context.getString(
                        R.string.badge_district_neighborhood_count,
                        item.neighborhoodCount
                    ).uppercase(
                        TURKISH_LOCALE
                    )
            }
        }
    }

    // =========================================================
    // NEIGHBORHOOD HEADER
    // =========================================================

    inner class NeighborhoodViewHolder(
        private val binding: ItemBadgeNeighborhoodHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            item: BadgeListItem.NeighborhoodHeader
        ) {
            val context = binding.root.context
            val tier = item.currentTier

            val accentColor =
                ContextCompat.getColor(
                    context,
                    tier.accentColorRes
                )

            val pillColor =
                ContextCompat.getColor(
                    context,
                    tier.pillBgColorRes
                )

            val dividerColor =
                ContextCompat.getColor(
                    context,
                    R.color.catmap_divider
                )

            val mutedColor =
                ContextCompat.getColor(
                    context,
                    R.color.catmap_text_muted
                )

            binding.apply {

                // -------------------------------------------------
                // MAHALLE
                // -------------------------------------------------

                tvNeighborhoodName.text =
                    if (item.neighborhood.isNotBlank()) {
                        "${item.neighborhood} Mahallesi"
                    } else {
                        item.district
                    }

                // -------------------------------------------------
                // CURRENT TIER ICON
                // -------------------------------------------------

                imgCurrentTier.setImageResource(
                    tier.iconResId
                )

                /*
                 * Küçük ikon yüzeyi de tier ailesinden
                 * hafif renk alsın.
                 */
                layoutCurrentTierIcon.backgroundTintList =
                    ColorStateList.valueOf(
                        pillColor
                    )

                // -------------------------------------------------
                // ÖZET
                //
                // Mahalle Gözcüsü • 18 kedi • 5/8 rozet
                // -------------------------------------------------

                val tierTitle =
                    context.getString(
                        tier.titleResId
                    )

                tvNeighborhoodSummary.text =
                    context.getString(
                        R.string.badge_neighborhood_summary_format,
                        tierTitle,
                        item.catCount,
                        item.unlockedBadgeCount,
                        item.totalBadgeCount
                    )

                // -------------------------------------------------
                // ACCORDION
                // -------------------------------------------------

                viewTierAccent.isVisible =
                    item.isExpanded

                /*
                 * RecyclerView recycled view tuttuğu için
                 * sadece animate() çağırıp bırakmıyoruz.
                 *
                 * Her bind'da kesin rotation veriyoruz.
                 */
                imgChevron.rotation =
                    if (item.isExpanded) {
                        90f
                    } else {
                        0f
                    }

                // -------------------------------------------------
                // AÇIK / KAPALI RENKLERİ
                // -------------------------------------------------

                viewTierAccent.backgroundTintList =
                    ColorStateList.valueOf(
                        accentColor
                    )

                if (item.isExpanded) {

                    /*
                     * Border'ı full accent yaparsak özellikle
                     * Gece Altını gibi tier'larda sert duruyor.
                     */
                    cardNeighborhood.strokeColor =
                        ColorUtils.setAlphaComponent(
                            accentColor,
                            110
                        )

                    imgChevron.imageTintList =
                        ColorStateList.valueOf(
                            accentColor
                        )

                } else {

                    cardNeighborhood.strokeColor =
                        dividerColor

                    imgChevron.imageTintList =
                        ColorStateList.valueOf(
                            mutedColor
                        )
                }

                // -------------------------------------------------
                // CLICK
                // -------------------------------------------------

                root.setOnClickListener {

                    /*
                     * Kullanıcı dokunduğu anda küçük bir feedback
                     * veriyoruz.
                     *
                     * Asıl state Fragment/ViewModel tarafından
                     * değiştirilecek.
                     */
                    imgChevron.animate()
                        .rotation(
                            if (item.isExpanded) {
                                0f
                            } else {
                                90f
                            }
                        )
                        .setDuration(180L)
                        .start()

                    onNeighborhoodClick(item)
                }
            }
        }
    }

    // =========================================================
    // BADGE
    // =========================================================

    inner class BadgeViewHolder(
        private val binding: ItemNeighborhoodBadgeBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(
            badge: NeighborhoodBadgeModel
        ) {
            val context = binding.root.context

            val currentTier =
                badge.currentTier

            val isUnlocked =
                badge.isUnlocked

            binding.apply {

                // -------------------------------------------------
                // LOCATION
                // -------------------------------------------------

                tvDisplayName.text = when {

                    badge.neighborhood.isNotBlank() &&
                            badge.district.isNotBlank() -> {

                        "${badge.district} • " +
                                "${badge.neighborhood} Mahallesi"
                    }

                    badge.neighborhood.isNotBlank() -> {

                        "${badge.neighborhood} Mahallesi"
                    }

                    else -> {

                        badge.displayName
                    }
                }

                // -------------------------------------------------
                // MAHALLEYE BAĞLI ROZET ÜNVANI
                // -------------------------------------------------

                val tierTitle =
                    context.getString(
                        currentTier.titleResId
                    )

                tvBadgeTitle.text =
                    if (badge.neighborhood.isNotBlank()) {

                        "${badge.neighborhood.withPossessiveSuffix()} " +
                                tierTitle

                    } else {

                        tierTitle
                    }

                // -------------------------------------------------
                // BADGE ICON
                // -------------------------------------------------

                imgBadgeIcon.setImageResource(
                    currentTier.iconResId
                )

                // -------------------------------------------------
                // LEVEL
                // -------------------------------------------------

                tvTierLevelBadge.text =
                    context.getString(
                        R.string.badge_level_format,
                        currentTier.level
                    ).uppercase(
                        TURKISH_LOCALE
                    )

                // -------------------------------------------------
                // CAT COUNT
                // -------------------------------------------------

                val targetThreshold =
                    currentTier.threshold

                tvCatCountPill.text =
                    context.getString(
                        R.string.badge_card_cat_count_format,
                        badge.catCount,
                        targetThreshold
                    )

                // -------------------------------------------------
                // PROGRESS
                // -------------------------------------------------

                val progressPercent =
                    badge.progressPercent

                progressBarBadge.progress =
                    progressPercent

                if (isUnlocked) {

                    tvProgressInfo.text =
                        context.getString(
                            R.string.badge_card_unlocked_progress
                        )

                } else {

                    tvProgressInfo.text =
                        context.getString(
                            R.string.badge_card_locked_progress,
                            badge.remainingCatCount
                        )
                }

                // -------------------------------------------------
                // UNLOCKED
                // -------------------------------------------------

                if (isUnlocked) {

                    val accentColor =
                        ContextCompat.getColor(
                            context,
                            currentTier.accentColorRes
                        )

                    val pillBgColor =
                        ContextCompat.getColor(
                            context,
                            currentTier.pillBgColorRes
                        )

                    val trackBgColor =
                        ContextCompat.getColor(
                            context,
                            currentTier.progressTrackBgRes
                        )

                    imgBadgeIcon.colorFilter = null
                    imgBadgeIcon.alpha = 1f

                    imgLockOverlay.visibility =
                        View.GONE

                    tvBadgeTitle.setTextColor(
                        accentColor
                    )

                    tvCatCountPill.setTextColor(
                        accentColor
                    )

                    tvCatCountPill.backgroundTintList =
                        ColorStateList.valueOf(
                            pillBgColor
                        )

                    tvTierLevelBadge.setTextColor(
                        accentColor
                    )

                    tvTierLevelBadge.backgroundTintList =
                        ColorStateList.valueOf(
                            pillBgColor
                        )

                    cardBadge.strokeColor =
                        accentColor

                    progressBarBadge.progressTintList =
                        ColorStateList.valueOf(
                            accentColor
                        )

                    progressBarBadge
                        .progressBackgroundTintList =
                        ColorStateList.valueOf(
                            trackBgColor
                        )

                } else {

                    val mutedColor =
                        ContextCompat.getColor(
                            context,
                            R.color.catmap_text_muted
                        )

                    val dividerColor =
                        ContextCompat.getColor(
                            context,
                            R.color.catmap_divider
                        )

                    val matrix =
                        ColorMatrix().apply {
                            setSaturation(0f)
                        }

                    imgBadgeIcon.colorFilter =
                        ColorMatrixColorFilter(
                            matrix
                        )

                    imgBadgeIcon.alpha =
                        0.35f

                    imgLockOverlay.visibility =
                        View.VISIBLE

                    tvBadgeTitle.setTextColor(
                        mutedColor
                    )

                    tvCatCountPill.setTextColor(
                        mutedColor
                    )

                    tvCatCountPill.backgroundTintList =
                        ColorStateList.valueOf(
                            dividerColor
                        )

                    tvTierLevelBadge.setTextColor(
                        mutedColor
                    )

                    tvTierLevelBadge.backgroundTintList =
                        ColorStateList.valueOf(
                            dividerColor
                        )

                    cardBadge.strokeColor =
                        dividerColor

                    progressBarBadge.progressTintList =
                        ColorStateList.valueOf(
                            mutedColor
                        )

                    progressBarBadge
                        .progressBackgroundTintList =
                        ColorStateList.valueOf(
                            dividerColor
                        )
                }

                // -------------------------------------------------
                // CLICK
                // -------------------------------------------------

                root.setOnClickListener {

                    onBadgeClick(
                        badge
                    )
                }
            }
        }
    }

    // =========================================================
    // DIFF
    // =========================================================

    class BadgeDiffCallback :
        DiffUtil.ItemCallback<BadgeListItem>() {

        override fun areItemsTheSame(
            oldItem: BadgeListItem,
            newItem: BadgeListItem
        ): Boolean {

            return oldItem.stableId ==
                    newItem.stableId
        }

        override fun areContentsTheSame(
            oldItem: BadgeListItem,
            newItem: BadgeListItem
        ): Boolean {

            return oldItem == newItem
        }
    }

    companion object {

        private const val VIEW_TYPE_DISTRICT =
            1

        private const val VIEW_TYPE_NEIGHBORHOOD =
            2

        private const val VIEW_TYPE_BADGE =
            3

        private val TURKISH_LOCALE =
            Locale("tr", "TR")
    }
}