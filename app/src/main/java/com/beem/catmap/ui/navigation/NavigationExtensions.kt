package com.beem.catmap.ui.navigation

import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

fun Fragment.handleBackPressWithEngine() {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            SmartNavigationEngine.navigateBack()
        }
    })
}