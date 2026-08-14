package com.beem.catmap.ui.badge

import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.R
import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.beem.catmap.databinding.BottomSheetBadgeDetailBinding
import com.beem.catmap.utils.withPossessiveSuffix
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.chip.Chip
import kotlinx.coroutines.launch
import java.util.Locale

class BadgeDetailBottomSheet : BottomSheetDialogFragment() {

    private var _binding: BottomSheetBadgeDetailBinding? = null
    private val binding get() = _binding!!


    private val viewModel: BadgeViewModel by viewModels(
        ownerProducer = { requireParentFragment() }
    )

    private var currentEquippedBadge:
            EquippedBadgeModel? = null

    private lateinit var badge: NeighborhoodBadgeModel

    private val userId: String
        get() = requireArguments()
            .getString(ARG_USER_ID)
            .orEmpty()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        badge = createBadgeFromArguments()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetBadgeDetailBinding.inflate(
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
        super.onViewCreated(view, savedInstanceState)

        bindBadge()

        observeEquippedBadge()
        observeEquipBadgeState()
    }

    override fun onStart() {
        super.onStart()

        configureBottomSheet()
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }



    private fun observeEquipBadgeState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.equipBadgeState.collect { state ->

                    when (state) {

                        is EquipBadgeState.Idle -> {

                            renderBadgeActionState()
                        }

                        is EquipBadgeState.Loading -> {

                            showEquipLoading()
                        }

                        is EquipBadgeState.Success -> {

                            /*
                             * _equippedBadge zaten ViewModel'de
                             * güncellendi.
                             *
                             * StateFlow observer render edecek.
                             */
                            renderBadgeActionState()
                        }

                        is EquipBadgeState.Error -> {

                            showEquipError(
                                state.message
                            )
                        }
                    }
                }
            }
        }
    }

    private fun observeEquippedBadge() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.equippedBadge.collect {
                        equippedBadge ->

                    currentEquippedBadge =
                        equippedBadge

                    renderBadgeActionState()
                }
            }
        }
    }

    private fun renderBadgeActionState() {

        if (!badge.isUnlocked) {

            binding.layoutBadgeAction.isVisible =
                false

            return
        }

        binding.layoutBadgeAction.isVisible =
            true

        if (isCurrentBadgeEquipped()) {

            showEquippedState()

        } else {

            showAvailableState()
        }
    }
    private fun showAvailableState() {

        binding.btnUseBadge.apply {

            isEnabled = true

            alpha = 1f

            text = getString(
                R.string.badge_use_action
            )
        }
    }
    private fun showEquippedState() {

        binding.btnUseBadge.apply {

            isEnabled = false

            alpha = 0.88f

            text = getString(
                R.string.badge_using_action
            )
        }
    }

    private fun showEquipIdle() {

        binding.btnUseBadge.apply {

            isEnabled = true

            text = getString(
                R.string.badge_use_action
            )
        }
    }
    private fun showEquipLoading() {

        binding.btnUseBadge.apply {

            isEnabled = false

            alpha = 0.8f

            text = getString(
                R.string.badge_use_loading
            )
        }
    }
    private fun showEquipSuccess() {

        binding.btnUseBadge.apply {

            isEnabled = false

            text = getString(
                R.string.badge_using_action
            )

            alpha = 0.92f
        }
    }
    private fun showEquipError(
        message: String
    ) {

        renderBadgeActionState()

        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_SHORT
        ).show()

        viewModel.resetEquipBadgeState()
    }


    private fun isCurrentBadgeEquipped(): Boolean {

        val equipped =
            currentEquippedBadge
                ?: return false

        return equipped.neighborhoodBadgeId ==
                badge.badgeId &&
                equipped.tierLevel ==
                badge.currentTier.level
    }

    // =========================================================
    // BIND
    // =========================================================

    private fun bindBadge() {
        val tier = badge.currentTier
        val isUnlocked = badge.isUnlocked

        val accentColor = requireContext()
            .getColor(tier.accentColorRes)

        val pillColor = requireContext()
            .getColor(tier.pillBgColorRes)

        val trackColor = requireContext()
            .getColor(tier.progressTrackBgRes)

        bindHero(
            tier = tier,
            isUnlocked = isUnlocked,
            accentColor = accentColor,
            pillColor = pillColor
        )

        bindTexts(tier)

        bindTags(
            tier = tier,
            accentColor = accentColor,
            pillColor = pillColor,
            trackColor = trackColor
        )

        bindProgress(
            tier = tier,
            isUnlocked = isUnlocked,
            accentColor = accentColor,
            pillColor = pillColor,
            trackColor = trackColor
        )

        bindUnlockedStatus(
            isUnlocked = isUnlocked
        )

        bindBadgeAction(
            tier = tier,
            isUnlocked = isUnlocked,
            accentColor = accentColor
        )
    }

    // =========================================================
    // HERO
    // =========================================================

    private fun bindHero(
        tier: BadgeTier,
        isUnlocked: Boolean,
        accentColor: Int,
        pillColor: Int
    ) {
        binding.apply {

            imgBadge.setImageResource(tier.iconResId)

            /*
             * Kilitliyken rozeti tamamen grayscale yapmıyoruz.
             *
             * Kullanıcı kazanacağı gerçek rozeti görebilsin.
             */
            imgBadge.alpha = if (isUnlocked) {
                1f
            } else {
                0.55f
            }

            imgBadgeLock.isVisible = !isUnlocked

            /*
             * Rozetin arkasındaki halo.
             *
             * Tier'ın kendi accent renginin çok hafif hali.
             */
            val glowColor = ColorUtils.setAlphaComponent(
                accentColor,
                28
            )

            viewBadgeGlow.backgroundTintList =
                ColorStateList.valueOf(glowColor)

            /*
             * Element + Seviye pill.
             */
            tvBadgeElement.setTextColor(accentColor)
            tvBadgeLevel.setTextColor(accentColor)

            tvBadgeElement.backgroundTintList =
                ColorStateList.valueOf(pillColor)

            tvBadgeLevel.backgroundTintList =
                ColorStateList.valueOf(pillColor)
        }
    }

    // =========================================================
    // TEXTS
    // =========================================================

    private fun bindTexts(
        tier: BadgeTier
    ) {
        binding.apply {

            /*
             * Selçuklu • Yazır Mahallesi
             */
            tvBadgeLocation.text = buildLocationText()

            /*
             * Yazır'ın Mahalle Öncüsü
             */
            tvBadgeTitle.text = buildContextualTitle(
                tier = tier
            )

            /*
             * ALTIN ROZETİ
             */
            val element = getString(
                tier.elementResId
            )

            tvBadgeElement.text = getString(
                R.string.badge_element_format,
                element
            ).uppercase(TURKISH_LOCALE)

            /*
             * SEVİYE 6
             */
            tvBadgeLevel.text = getString(
                R.string.badge_level_format,
                tier.level
            ).uppercase(TURKISH_LOCALE)

            /*
             * Artık haritayı takip edenlerden değil...
             */
            tvShortDescription.setText(
                tier.shortDescriptionResId
            )

            /*
             * Rozetin uzun CatMap hikâyesi.
             */
            tvBadgeStory.setText(
                tier.storyResId
            )
        }
    }

    // =========================================================
    // TAGS
    // =========================================================

    private fun bindTags(
        tier: BadgeTier,
        accentColor: Int,
        pillColor: Int,
        trackColor: Int
    ) {
        val tags = resources.getStringArray(tier.tagsResId)

        binding.chipGroupTags.removeAllViews()

        val isTier8 = tier == BadgeTier.TIER_08

        val resolvedTextColor = if (isTier8) {
            requireContext().getColor(
                R.color.badge_tier_08_detail_chip_text
            )
        } else {
            accentColor
        }

        val resolvedBackgroundColor = if (isTier8) {
            requireContext().getColor(
                R.color.badge_tier_08_detail_chip_bg
            )
        } else {
            pillColor
        }

        val resolvedStrokeColor = if (isTier8) {
            requireContext().getColor(
                R.color.badge_tier_08_detail_chip_stroke
            )
        } else {
            trackColor
        }

        tags.forEach { tag ->

            val chip = Chip(requireContext()).apply {

                text = tag

                isClickable = false
                isCheckable = false
                isFocusable = false

                textSize = 11f

                setTextColor(resolvedTextColor)

                chipBackgroundColor =
                    ColorStateList.valueOf(resolvedBackgroundColor)

                this.chipStrokeColor =
                    ColorStateList.valueOf(resolvedStrokeColor)

                chipStrokeWidth = 1.dpFloat()

                chipCornerRadius = 50.dpFloat()

                minHeight = 32.dp()

                chipStartPadding = 10.dpFloat()
                chipEndPadding = 10.dpFloat()

                textStartPadding = 0f
                textEndPadding = 0f
            }

            binding.chipGroupTags.addView(chip)
        }
    }

    // =========================================================
    // PROGRESS
    // =========================================================


    private fun bindProgress(
        tier: BadgeTier,
        isUnlocked: Boolean,
        accentColor: Int,
        pillColor: Int,
        trackColor: Int
    ) {
        binding.apply {

            tvProgressTitle.setText(
                R.string.badge_progress_title
            )

            val displayedCatCount = badge.catCount
                .coerceAtMost(tier.threshold)

            tvCatProgress.text = getString(
                R.string.badge_progress_count_format,
                displayedCatCount,
                tier.threshold
            )

            progressBadge.progress =
                if (isUnlocked) {
                    100
                } else {
                    badge.progressPercent
                }

            // -----------------------------------------------------
            // TIER 08 · GECE ALTINI ÖZEL PRESTİJ TEMASI
            // -----------------------------------------------------

            if (tier == BadgeTier.TIER_08) {

                val cardBgColor = requireContext().getColor(
                    R.color.badge_tier_08_detail_card_bg
                )

                val cardStrokeColor = requireContext().getColor(
                    R.color.badge_tier_08_detail_card_stroke
                )

                val titleColor = requireContext().getColor(
                    R.color.badge_tier_08_detail_title
                )

                val descriptionColor = requireContext().getColor(
                    R.color.badge_tier_08_detail_text
                )

                val valueColor = requireContext().getColor(
                    R.color.badge_tier_08_detail_value
                )

                val progressColor = requireContext().getColor(
                    R.color.badge_tier_08_detail_progress
                )

                val progressTrackColor = requireContext().getColor(
                    R.color.badge_tier_08_detail_track
                )

                cardBadgeProgress.setCardBackgroundColor(
                    cardBgColor
                )

                cardBadgeProgress.strokeColor =
                    cardStrokeColor

                tvProgressTitle.setTextColor(
                    titleColor
                )

                tvCatProgress.setTextColor(
                    valueColor
                )

                tvProgressDescription.setTextColor(
                    descriptionColor
                )

                progressBadge.progressTintList =
                    ColorStateList.valueOf(progressColor)

                progressBadge.progressBackgroundTintList =
                    ColorStateList.valueOf(progressTrackColor)

            } else {

                // -------------------------------------------------
                // NORMAL TIER TEMASI
                // -------------------------------------------------

                tvCatProgress.setTextColor(
                    accentColor
                )

                progressBadge.progressTintList =
                    ColorStateList.valueOf(accentColor)

                progressBadge.progressBackgroundTintList =
                    ColorStateList.valueOf(trackColor)

                val cardSurface = ColorUtils.blendARGB(
                    Color.WHITE,
                    pillColor,
                    0.55f
                )

                cardBadgeProgress.setCardBackgroundColor(
                    cardSurface
                )

                cardBadgeProgress.strokeColor =
                    ColorUtils.setAlphaComponent(
                        accentColor,
                        80
                    )

                tvProgressTitle.setTextColor(
                    requireContext().getColor(
                        R.color.catmap_text_primary
                    )
                )

                tvProgressDescription.setTextColor(
                    requireContext().getColor(
                        R.color.catmap_text_secondary
                    )
                )
            }

            // -----------------------------------------------------
            // PROGRESS AÇIKLAMASI
            // -----------------------------------------------------

            if (isUnlocked) {

                tvProgressDescription.setText(
                    R.string.badge_progress_unlocked
                )

            } else {

                val tierTitle = getString(
                    tier.titleResId
                )

                tvProgressDescription.text = getString(
                    R.string.badge_progress_locked_format,
                    tierTitle,
                    badge.remainingCatCount
                )
            }
        }
    }

    // =========================================================
    // UNLOCKED STATUS
    // =========================================================

    private fun bindUnlockedStatus(
        isUnlocked: Boolean
    ) {
        binding.layoutUnlockedStatus.isVisible =
            isUnlocked

        if (!isUnlocked) return

        val locationName = when {

            badge.neighborhood.isNotBlank() ->
                badge.neighborhood

            badge.district.isNotBlank() ->
                badge.district

            else ->
                badge.city
        }

        binding.tvUnlockedDescription.text =
            getString(
                R.string.badge_unlocked_description_format,
                locationName
            )
    }

    private fun bindBadgeAction(
        tier: BadgeTier,
        isUnlocked: Boolean,
        accentColor: Int
    ) {
        binding.apply {

            layoutBadgeAction.isVisible =
                isUnlocked

            if (!isUnlocked) {
                return
            }

            btnUseBadge.backgroundTintList =
                ColorStateList.valueOf(
                    accentColor
                )

            val buttonTextColor =
                if (tier == BadgeTier.TIER_08) {

                    requireContext().getColor(
                        R.color.badge_tier_08_detail_value
                    )

                } else {

                    Color.WHITE
                }

            btnUseBadge.setTextColor(
                buttonTextColor
            )

            btnUseBadge.setOnClickListener {

                onUseBadgeClicked()
            }
        }
    }

    private fun onUseBadgeClicked() {

        if (!badge.isUnlocked) {
            return
        }

        if (userId.isBlank()) {
            return
        }

        viewModel.equipBadge(
            userId = userId,
            badge = badge
        )
    }

    // =========================================================
    // LOCATION
    // =========================================================

    private fun buildLocationText(): String {
        return when {

            badge.neighborhood.isNotBlank() &&
                    badge.district.isNotBlank() -> {

                "${badge.district} • ${badge.neighborhood} Mahallesi"
            }

            badge.neighborhood.isNotBlank() -> {

                "${badge.neighborhood} Mahallesi"
            }

            badge.district.isNotBlank() -> {

                badge.district
            }

            else -> {

                badge.city
            }
        }
    }

    // =========================================================
    // CONTEXTUAL TITLE
    // =========================================================

    private fun buildContextualTitle(
        tier: BadgeTier
    ): String {

        val tierTitle = getString(
            tier.titleResId
        )

        if (badge.neighborhood.isBlank()) {
            return tierTitle
        }

        /*
         * Yazır'ın Mahalle Öncüsü
         * Bosna Hersek'in Pati Rehberi
         * Sancak'ın Mahalle Gözcüsü
         */
        return buildString {

            append(
                badge.neighborhood.withPossessiveSuffix()
            )

            append(" ")

            append(tierTitle)
        }
    }

    // =========================================================
    // BOTTOM SHEET BEHAVIOR
    // =========================================================

    private fun configureBottomSheet() {
        val bottomSheetDialog =
            dialog as? BottomSheetDialog ?: return

        val bottomSheet = bottomSheetDialog
            .findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )
            ?: return

        // BottomSheet ekran yüksekliğini kullanabilsin
        bottomSheet.layoutParams = bottomSheet.layoutParams.apply {
            height = ViewGroup.LayoutParams.MATCH_PARENT
        }

        val behavior = BottomSheetBehavior.from(bottomSheet)

        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isFitToContents = false

            expandedOffset = 0
        }
    }

    // =========================================================
    // ARGUMENTS
    // =========================================================

    private fun createBadgeFromArguments():
            NeighborhoodBadgeModel {

        val args = requireArguments()

        val tierName = args.getString(
            ARG_TIER
        )

        val tier = tierName
            ?.let { name ->
                BadgeTier.entries.firstOrNull {
                    it.name == name
                }
            }

        return NeighborhoodBadgeModel(
            badgeId = args.getString(ARG_BADGE_ID)
                .orEmpty(),

            city = args.getString(ARG_CITY)
                .orEmpty(),

            district = args.getString(ARG_DISTRICT)
                .orEmpty(),

            neighborhood = args
                .getString(ARG_NEIGHBORHOOD)
                .orEmpty(),

            catCount = args.getLong(
                ARG_CAT_COUNT
            ),

            overrideTier = tier
        )
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
            "BadgeDetailBottomSheet"

        private const val ARG_USER_ID = "user_id"

        private const val ARG_BADGE_ID =
            "badge_id"

        private const val ARG_CITY =
            "city"

        private const val ARG_DISTRICT =
            "district"

        private const val ARG_NEIGHBORHOOD =
            "neighborhood"

        private const val ARG_CAT_COUNT =
            "cat_count"

        private const val ARG_TIER =
            "tier"

        private val TURKISH_LOCALE =
            Locale("tr", "TR")

        private const val TURKISH_VOWELS =
            "aeıioöuü"

        fun newInstance(
            userId: String,
            badge: NeighborhoodBadgeModel
        ): BadgeDetailBottomSheet {

            return BadgeDetailBottomSheet().apply {

                arguments = bundleOf(

                    ARG_USER_ID to userId,

                    ARG_BADGE_ID to
                            badge.badgeId,

                    ARG_CITY to
                            badge.city,

                    ARG_DISTRICT to
                            badge.district,

                    ARG_NEIGHBORHOOD to
                            badge.neighborhood,

                    ARG_CAT_COUNT to
                            badge.catCount,

                    /*
                     * overrideTier çok önemli.
                     *
                     * RecyclerView'da hangi kart tıklandıysa
                     * BottomSheet aynı Tier'ı göstermeli.
                     */
                    ARG_TIER to
                            badge.currentTier.name
                )
            }
        }

        fun show(
            fragmentManager: FragmentManager,
            userId: String,
            badge: NeighborhoodBadgeModel
        ) {
            newInstance(
                userId = userId,
                badge = badge
            ).show(
                fragmentManager,
                TAG
            )
        }
    }
}