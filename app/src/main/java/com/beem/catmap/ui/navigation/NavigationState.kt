package com.beem.catmap.ui.navigation

sealed interface NavigationState {
    object Initial : NavigationState
    data class Active(val screen: Screen, val trigger: NavigationTrigger) : NavigationState
}