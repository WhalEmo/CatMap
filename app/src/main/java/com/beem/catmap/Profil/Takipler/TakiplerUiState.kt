package com.beem.catmap.Profil.Takipler

import com.beem.catmap.KullaniciAuth.Kullanici

sealed interface TakiplerUiState {
    object Idle : TakiplerUiState
    object Loading : TakiplerUiState
    data class Success(
        val kullanicilar: List<Kullanici>,
        val isLastPage: Boolean = false,
        val isLoadingMore: Boolean = false
    ) : TakiplerUiState
    data class Error(val message: String) : TakiplerUiState
}
