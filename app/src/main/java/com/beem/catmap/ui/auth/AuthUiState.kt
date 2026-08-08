package com.beem.catmap.ui.auth

import com.beem.catmap.KullaniciAuth.Kullanici

sealed interface AuthUiState {
    object Idle : AuthUiState
    data class Loading(val message: String = "İşlem yapılıyor...") : AuthUiState
    data class Success(val user: Kullanici, val message: String) : AuthUiState
    data class Error(val errorMessage: String) : AuthUiState
}