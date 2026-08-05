package com.beem.catmap.ui.navigation

import android.os.Bundle

data class NavBackStackEntry(
    val screen: Screen,
    val screenId: String,
    val args: Bundle = Bundle(),
    val trigger: NavigationTrigger = NavigationTrigger.FORWARD
)