package com.beem.catmap.ui.spotoperation

sealed interface SpotUserPreference {
    object NewSpot : SpotUserPreference

    data class SpecificSpot(val spotId: String) : SpotUserPreference
}