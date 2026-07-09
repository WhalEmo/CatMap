package com.beem.catmap.ui.navigation

import android.os.Bundle

sealed interface NavigationState {
    object Initial : NavigationState
    data class Active(
        val screen: Screen,
        val trigger: NavigationTrigger,
        val args: Bundle,
        val screenId: String
    ) : NavigationState
}