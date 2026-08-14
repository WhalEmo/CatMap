package com.beem.catmap.ui.badge

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.graphics.ColorUtils
import com.beem.catmap.R
import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.databinding.BottomSheetBadgeStoryBinding
import com.beem.catmap.utils.withPossessiveSuffix
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import androidx.fragment.app.FragmentManager
import java.util.Locale

class BadgeStoryBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBadgeStoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var tier: BadgeTier

    private val city: String
        get() = requireArguments()
            .getString(ARG_CITY)
            .orEmpty()

    private val district: String
        get() = requireArguments()
            .getString(ARG_DISTRICT)
            .orEmpty()

    private val neighborhood: String
        get() = requireArguments()
            .getString(ARG_NEIGHBORHOOD)
            .orEmpty()

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)

        val tierLevel = requireArguments()
            .getInt(ARG_TIER_LEVEL)

        tier = BadgeTier.entries.firstOrNull {
            it.level == tierLevel
        } ?: BadgeTier.TIER_01
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        _binding =
            BottomSheetBadgeStoryBinding.inflate(
                inflater,
                container,
                false
            )

        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?
    ) {
        super.onViewCreated(
            view,
            savedInstanceState
        )

        bindStory()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    // =========================================================
    // BIND
    // =========================================================

    private fun bindStory() {

        val context = requireContext()

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

        val trackColor =
            ContextCompat.getColor(
                context,
                tier.progressTrackBgRes
            )

        binding.apply {

            // -------------------------------------------------
            // BADGE
            // -------------------------------------------------

            imgBadge.setImageResource(
                tier.iconResId
            )

            // -------------------------------------------------
            // LOCATION
            // -------------------------------------------------

            tvBadgeLocation.text =
                buildLocationText()

            // -------------------------------------------------
            // TITLE
            // -------------------------------------------------

            val tierTitle =
                getString(
                    tier.titleResId
                )

            tvBadgeTitle.text =
                if (neighborhood.isNotBlank()) {

                    "${neighborhood.withPossessiveSuffix()} $tierTitle"

                } else {

                    tierTitle
                }

            // -------------------------------------------------
            // ELEMENT
            // -------------------------------------------------

            val element =
                getString(
                    tier.elementResId
                )

            tvBadgeElement.text =
                getString(
                    R.string.badge_element_format,
                    element
                ).uppercase(
                    TURKISH_LOCALE
                )

            // -------------------------------------------------
            // LEVEL
            // -------------------------------------------------

            tvBadgeLevel.text =
                getString(
                    R.string.badge_level_format,
                    tier.level
                ).uppercase(
                    TURKISH_LOCALE
                )

            // -------------------------------------------------
            // DESCRIPTION
            // -------------------------------------------------

            tvShortDescription.setText(
                tier.shortDescriptionResId
            )

            // -------------------------------------------------
            // STORY
            // -------------------------------------------------

            tvBadgeStory.setText(
                tier.storyResId
            )

            // -------------------------------------------------
            // COLORS
            // -------------------------------------------------

            tvBadgeElement.setTextColor(
                accentColor
            )

            tvBadgeLevel.setTextColor(
                accentColor
            )

            tvBadgeElement.backgroundTintList =
                ColorStateList.valueOf(
                    pillColor
                )

            tvBadgeLevel.backgroundTintList =
                ColorStateList.valueOf(
                    pillColor
                )

            viewStoryDivider.backgroundTintList =
                ColorStateList.valueOf(
                    accentColor
                )

            val glowColor =
                ColorUtils.setAlphaComponent(
                    accentColor,
                    28
                )

            viewBadgeGlow.backgroundTintList =
                ColorStateList.valueOf(
                    glowColor
                )

            // -------------------------------------------------
            // TAGS
            // -------------------------------------------------

            bindTags(
                accentColor = accentColor,
                pillColor = pillColor,
                trackColor = trackColor
            )
        }
    }

    // =========================================================
    // TAGS
    // =========================================================

    private fun bindTags(
        accentColor: Int,
        pillColor: Int,
        trackColor: Int
    ) {

        val tags =
            resources.getStringArray(
                tier.tagsResId
            )

        binding.chipGroupTags.removeAllViews()

        val isTier8 =
            tier == BadgeTier.TIER_08

        val resolvedTextColor =
            if (isTier8) {

                requireContext().getColor(
                    R.color.badge_tier_08_detail_chip_text
                )

            } else {

                accentColor
            }

        val resolvedBackgroundColor =
            if (isTier8) {

                requireContext().getColor(
                    R.color.badge_tier_08_detail_chip_bg
                )

            } else {

                pillColor
            }

        val resolvedStrokeColor =
            if (isTier8) {

                requireContext().getColor(
                    R.color.badge_tier_08_detail_chip_stroke
                )

            } else {

                trackColor
            }

        tags.forEach { tag ->

            val chip =
                Chip(requireContext()).apply {

                    text = tag

                    isClickable = false
                    isCheckable = false
                    isFocusable = false

                    textSize = 11f

                    setTextColor(
                        resolvedTextColor
                    )

                    chipBackgroundColor =
                        ColorStateList.valueOf(
                            resolvedBackgroundColor
                        )

                    this.chipStrokeColor =
                        ColorStateList.valueOf(
                            resolvedStrokeColor
                        )

                    chipStrokeWidth =
                        1.dpFloat()

                    chipCornerRadius =
                        50.dpFloat()

                    minHeight =
                        32.dp()

                    chipStartPadding =
                        10.dpFloat()

                    chipEndPadding =
                        10.dpFloat()

                    textStartPadding = 0f
                    textEndPadding = 0f
                }

            binding.chipGroupTags.addView(
                chip
            )
        }
    }

    // =========================================================
    // LOCATION
    // =========================================================

    private fun buildLocationText(): String {

        return when {

            neighborhood.isNotBlank() &&
                    district.isNotBlank() -> {

                "$district • $neighborhood Mahallesi"
            }

            neighborhood.isNotBlank() -> {

                "$neighborhood Mahallesi"
            }

            district.isNotBlank() -> {

                district
            }

            else -> {

                city
            }
        }
    }

    // =========================================================
    // DP
    // =========================================================

    private fun Int.dp(): Int {
        return (
                this *
                        resources.displayMetrics.density
                ).toInt()
    }

    private fun Int.dpFloat(): Float {
        return this *
                resources.displayMetrics.density
    }

    companion object {

        const val TAG =
            "BadgeStoryBottomSheet"

        private const val ARG_TIER_LEVEL =
            "tier_level"

        private const val ARG_CITY =
            "city"

        private const val ARG_DISTRICT =
            "district"

        private const val ARG_NEIGHBORHOOD =
            "neighborhood"

        private val TURKISH_LOCALE =
            Locale("tr", "TR")

        fun newInstance(
            equippedBadge: EquippedBadgeModel
        ): BadgeStoryBottomSheet {

            return BadgeStoryBottomSheet().apply {

                arguments = Bundle().apply {

                    putInt(
                        ARG_TIER_LEVEL,
                        equippedBadge.tierLevel
                    )

                    putString(
                        ARG_CITY,
                        equippedBadge.city
                    )

                    putString(
                        ARG_DISTRICT,
                        equippedBadge.district
                    )

                    putString(
                        ARG_NEIGHBORHOOD,
                        equippedBadge.neighborhood
                    )
                }
            }
        }

        fun show(
            fragmentManager: FragmentManager,
            equippedBadge: EquippedBadgeModel
        ) {

            if (
                fragmentManager.findFragmentByTag(TAG)
                != null
            ) {
                return
            }

            newInstance(
                equippedBadge
            ).show(
                fragmentManager,
                TAG
            )
        }
    }
}