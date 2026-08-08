package com.beem.catmap.ui.auth

sealed interface AuthEvent {
    data class ShowToast(val message: String) : AuthEvent
    object NavigateToMap : AuthEvent
}