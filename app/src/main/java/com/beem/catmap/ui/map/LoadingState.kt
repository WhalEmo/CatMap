package com.beem.catmap.ui.map

sealed interface LoadingState {
    object Idle : LoadingState

    data class Loading(
        val message: String,
        val type: LoadingType = LoadingType.GENERAL
    ) : LoadingState
}