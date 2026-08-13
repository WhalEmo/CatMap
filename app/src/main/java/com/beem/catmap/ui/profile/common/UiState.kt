package com.beem.catmap.ui.profile.common

import com.beem.catmap.data.model.UserModel

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    data class BlockedBy(val profile: UserModel? = null) : UiState<Nothing>//BU KULLANCII BENI ENGELLEDI

    data class Blocked(val profile: UserModel? = null) : UiState<Nothing>//BEN ENGELELDIYSEM
}