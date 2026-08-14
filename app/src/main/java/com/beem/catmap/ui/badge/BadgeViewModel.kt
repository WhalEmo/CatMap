package com.beem.catmap.ui.badge

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.beem.catmap.data.repository.BadgeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class BadgeViewModel : ViewModel() {

    private val repository = BadgeRepository.getInstance()

    private val _uiState = MutableStateFlow<BadgeUiState>(BadgeUiState.Loading)
    val uiState: StateFlow<BadgeUiState> = _uiState.asStateFlow()

    private val _equippedBadge =
        MutableStateFlow<EquippedBadgeModel?>(null)

    val equippedBadge: StateFlow<EquippedBadgeModel?> =
        _equippedBadge.asStateFlow()

    private val _equipBadgeState =
        MutableStateFlow<EquipBadgeState>(
            EquipBadgeState.Idle
        )

    val equipBadgeState:
            StateFlow<EquipBadgeState> =
        _equipBadgeState.asStateFlow()

    fun loadUserBadges(
        userId: String
    ) {
        viewModelScope.launch {

            _uiState.value =
                BadgeUiState.Loading

            val badgesResult =
                repository.getUserBadges(userId)

            val equippedBadgeResult =
                repository.getEquippedBadge(userId)

            badgesResult
                .onSuccess { badges ->

                    _equippedBadge.value =
                        equippedBadgeResult.getOrNull()

                    _uiState.value =
                        BadgeUiState.Success(
                            badges
                        )
                }
                .onFailure { exception ->

                    _uiState.value =
                        BadgeUiState.Error(
                            exception.message
                                ?: "Rozetler yüklenemedi."
                        )
                }
        }
    }

    fun equipBadge(
        userId: String,
        badge: NeighborhoodBadgeModel
    ) {

        if (
            _equipBadgeState.value
                    is EquipBadgeState.Loading
        ) {
            return
        }

        viewModelScope.launch {

            _equipBadgeState.value =
                EquipBadgeState.Loading

            repository.equipBadge(
                userId = userId,
                badge = badge
            )
                .onSuccess {

                    _equippedBadge.value =
                        EquippedBadgeModel(
                            neighborhoodBadgeId =
                                badge.badgeId,

                            tierLevel =
                                badge.currentTier.level,

                            city =
                                badge.city,

                            district =
                                badge.district,

                            neighborhood =
                                badge.neighborhood
                        )

                    _equipBadgeState.value =
                        EquipBadgeState.Success(
                            badge
                        )
                }
                .onFailure { exception ->

                    _equipBadgeState.value =
                        EquipBadgeState.Error(
                            exception.message
                                ?: "Rozet kullanılamadı."
                        )
                }
        }
    }

    fun isBadgeEquipped(
        badge: NeighborhoodBadgeModel
    ): Boolean {

        val equipped =
            _equippedBadge.value
                ?: return false

        return equipped.neighborhoodBadgeId ==
                badge.badgeId &&
                equipped.tierLevel ==
                badge.currentTier.level
    }


    fun resetEquipBadgeState() {
        _equipBadgeState.value =
            EquipBadgeState.Idle
    }
}