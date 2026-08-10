package com.beem.catmap.ui.auth

import com.beem.catmap.KullaniciAuth.Kullanici

sealed class AuthEvent {
    data class ShowToast(val message: String) : AuthEvent()
    object NavigateToMap : AuthEvent()
    data class NavigateToProfileSetup(val user: Kullanici) : AuthEvent()
}