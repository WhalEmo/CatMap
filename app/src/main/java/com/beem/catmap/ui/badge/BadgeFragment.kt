package com.beem.catmap.ui.badge

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.beem.catmap.databinding.FragmentBadgeBinding
import com.beem.catmap.ui.badge.model.BadgeListItem
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import kotlinx.coroutines.launch

class BadgeFragment : Fragment() {

    private var _binding: FragmentBadgeBinding? = null
    private val binding get() = _binding!!

    private val viewModel: BadgeViewModel by viewModels()
    private lateinit var badgeAdapter: BadgeAdapter

    private var expandedNeighborhoodKey: String? = null

    private var currentNeighborhoods:
            List<NeighborhoodBadgeModel> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBadgeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Geri tuşuna basılınca SmartNavigationEngine çalışsın
        handleBackPressWithEngine()

        setupRecyclerView()
        observeUiState()

        // Kullanıcının rozetlerini yüklüyoruz
        viewModel.loadUserBadges(UserSession.userId)
    }

    private fun setupRecyclerView() {
        badgeAdapter = BadgeAdapter(

            onBadgeClick = { badge ->

                BadgeDetailBottomSheet.show(
                    childFragmentManager,
                    UserSession.userId,
                    badge
                )
            },

            onNeighborhoodClick = { neighborhood ->

                toggleNeighborhood(
                    neighborhood
                )
            }
        )

        binding.rvBadges.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = badgeAdapter
        }
    }

    private fun observeUiState() {

        viewLifecycleOwner.lifecycleScope.launch {

            viewLifecycleOwner.repeatOnLifecycle(
                Lifecycle.State.STARTED
            ) {

                viewModel.uiState.collect { state ->

                    when (state) {

                        is BadgeUiState.Loading -> {

                            binding.progressBarLoading.isVisible =
                                true

                            binding.rvBadges.isVisible =
                                false

                            binding.layoutEmptyState.isVisible =
                                false
                        }

                        is BadgeUiState.Success -> {

                            binding.progressBarLoading.isVisible =
                                false

                            currentNeighborhoods =
                                state.badges

                            if (state.badges.isEmpty()) {

                                binding.layoutEmptyState.isVisible =
                                    true

                                binding.rvBadges.isVisible =
                                    false

                                return@collect
                            }

                            binding.layoutEmptyState.isVisible =
                                false

                            binding.rvBadges.isVisible =
                                true

                            // -----------------------------------------
                            // İlk açılışta hangi mahalle açık olsun?
                            // -----------------------------------------

                            if (
                                expandedNeighborhoodKey == null &&
                                state.badges.isNotEmpty()
                            ) {
                                expandedNeighborhoodKey =
                                    neighborhoodKey(
                                        state.badges.first()
                                    )
                            }

                            // -----------------------------------------
                            // UI LIST
                            // -----------------------------------------

                            val uiItems =
                                buildBadgeListItems(
                                    state.badges
                                )

                            badgeAdapter.submitList(
                                uiItems
                            )

                            // -----------------------------------------
                            // TOPLAM AÇIK ROZET
                            // -----------------------------------------

                            val unlockedCount =
                                state.badges.sumOf {
                                        neighborhood ->

                                    BadgeTier.entries.count {
                                            tier ->

                                        neighborhood.catCount >=
                                                tier.threshold
                                    }
                                }

                            binding.tvTotalBadgeCount.text =
                                getString(
                                    R.string.badge_total_unlocked_format,
                                    unlockedCount
                                )
                        }

                        is BadgeUiState.Error -> {

                            binding.progressBarLoading.isVisible =
                                false

                            binding.rvBadges.isVisible =
                                false

                            binding.layoutEmptyState.isVisible =
                                true
                        }
                    }
                }
            }
        }
    }

    private fun neighborhoodKey(
        badge: NeighborhoodBadgeModel
    ): String {
        return buildString {
            append(badge.city)
            append("|")
            append(badge.district)
            append("|")
            append(badge.neighborhood)
        }
    }

    private fun buildBadgeListItems(
        neighborhoods: List<NeighborhoodBadgeModel>
    ): List<BadgeListItem> {

        val items =
            mutableListOf<BadgeListItem>()

        /*
         * Önce ilçelere ayırıyoruz.
         */
        val groupedByDistrict =
            neighborhoods
                .sortedWith(
                    compareBy(
                        { it.district },
                        { it.neighborhood }
                    )
                )
                .groupBy {
                    it.district
                }

        groupedByDistrict.forEach {
                (district, districtNeighborhoods) ->

            // =============================================
            // DISTRICT HEADER
            // =============================================

            items +=
                BadgeListItem.DistrictHeader(
                    district = district,
                    neighborhoodCount =
                        districtNeighborhoods.size
                )

            // =============================================
            // NEIGHBORHOODS
            // =============================================

            districtNeighborhoods.forEach {
                    neighborhood ->

                val key =
                    neighborhoodKey(
                        neighborhood
                    )

                val isExpanded =
                    expandedNeighborhoodKey == key

                /*
                 * Burada overrideTier kullanmıyoruz.
                 *
                 * Mahallenin GERÇEK mevcut seviyesi
                 * catCount üzerinden bulunuyor.
                 */
                val currentTier =
                    BadgeTier.getTierForCatCount(
                        neighborhood.catCount
                    )

                val unlockedBadgeCount =
                    BadgeTier.entries.count { tier ->
                        neighborhood.catCount >=
                                tier.threshold
                    }

                // =========================================
                // NEIGHBORHOOD HEADER
                // =========================================

                items +=
                    BadgeListItem.NeighborhoodHeader(
                        city =
                            neighborhood.city,

                        district =
                            neighborhood.district,

                        neighborhood =
                            neighborhood.neighborhood,

                        catCount =
                            neighborhood.catCount,

                        unlockedBadgeCount =
                            unlockedBadgeCount,

                        currentTier =
                            currentTier,

                        isExpanded =
                            isExpanded
                    )

                // =========================================
                // EXPANDED → 8 BADGE ITEM
                // =========================================

                if (isExpanded) {

                    BadgeTier.entries.forEach {
                            tier ->

                        val isTierUnlocked =
                            neighborhood.catCount >=
                                    tier.threshold

                        val tierBadge =
                            NeighborhoodBadgeModel(

                                badgeId = neighborhood.badgeId,

                                city =
                                    neighborhood.city,

                                district =
                                    neighborhood.district,

                                neighborhood =
                                    neighborhood.neighborhood,

                                catCount =
                                    neighborhood.catCount,

                                unlockedAt =
                                    if (isTierUnlocked) {
                                        neighborhood.unlockedAt
                                    } else {
                                        null
                                    },

                                overrideTier =
                                    tier
                            )

                        items +=
                            BadgeListItem.BadgeItem(
                                badge = tierBadge
                            )
                    }
                }
            }
        }

        return items
    }


    private fun toggleNeighborhood(
        item: BadgeListItem.NeighborhoodHeader
    ) {

        val key = buildString {
            append(item.city)
            append("|")
            append(item.district)
            append("|")
            append(item.neighborhood)
        }

        expandedNeighborhoodKey =
            if (expandedNeighborhoodKey == key) {

                /*
                 * Aynı mahalleye tekrar basıldı.
                 * Kapat.
                 */
                null

            } else {

                /*
                 * Başka mahalle açılıyor.
                 *
                 * Önceki otomatik olarak kapanmış oluyor,
                 * çünkü yalnızca tek key tutuyoruz.
                 */
                key
            }

        badgeAdapter.submitList(
            buildBadgeListItems(
                currentNeighborhoods
            )
        )
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}