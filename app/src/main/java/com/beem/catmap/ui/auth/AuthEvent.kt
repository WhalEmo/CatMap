package com.beem.catmap.ui.auth

import com.beem.catmap.data.model.UserModel

sealed class AuthEvent {
    data class ShowToast(val message: String) : AuthEvent()
    data class NavigateToMap(val isNewRegister: Boolean = false) : AuthEvent()
    data class NavigateToProfileSetup(val userModel: UserModel) : AuthEvent()
}