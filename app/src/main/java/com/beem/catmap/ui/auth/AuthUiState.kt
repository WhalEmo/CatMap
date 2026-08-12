package com.beem.catmap.ui.auth

import com.beem.catmap.data.model.UserModel

sealed interface AuthUiState {
    object Idle : AuthUiState
    data class Loading(val message: String = "İşlem yapılıyor...") : AuthUiState
    data class Success(val userModel: UserModel, val message: String) : AuthUiState
    data class Error(val errorMessage: String) : AuthUiState
}