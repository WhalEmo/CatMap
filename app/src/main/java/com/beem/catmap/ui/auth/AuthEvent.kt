package com.beem.catmap.ui.auth

import com.beem.catmap.data.model.UserModel

sealed class AuthEvent {
    data class ShowToast(val message: String) : AuthEvent()
    object NavigateToMap : AuthEvent()
    data class NavigateToProfileSetup(val userModel: UserModel) : AuthEvent()
}