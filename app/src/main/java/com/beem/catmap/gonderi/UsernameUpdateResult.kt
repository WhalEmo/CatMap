package com.beem.catmap.gonderi

sealed class UsernameUpdateResult {
    object Success : UsernameUpdateResult()
    object AlreadyTaken : UsernameUpdateResult()
    data class Error(val exception: Exception) : UsernameUpdateResult()
}