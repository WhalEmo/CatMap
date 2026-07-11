package com.beem.catmap.ui.manager

sealed class UiMessageState {
    data class Success(val message: String, val durationMs: Int = 3000) : UiMessageState()

    data class Error(val message: String, val durationMs: Int = 3500) : UiMessageState()

    data class Info(val message: String) : UiMessageState()
}