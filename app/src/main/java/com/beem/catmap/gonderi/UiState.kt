package com.beem.catmap.gonderi

import com.beem.catmap.KullaniciAuth.Kullanici

sealed interface UiState<out T> {
    object Idle : UiState<Nothing>
    object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
    object BlockedBy : UiState<Nothing>//BU KULLANCII BENI ENGELLEDI

    data class Blocked(val profile: Kullanici? = null) : UiState<Nothing>//BEN ENGELELDIYSEM
}