package com.beem.catmap.ui.navigation

import android.util.Log
import androidx.activity.OnBackPressedCallback
import androidx.fragment.app.Fragment

fun Fragment.handleBackPressWithEngine() {
    requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            Log.d("NAV_BACK_DEDEKTOR", "🚀 [Extension] Fragment: ${this@handleBackPressWithEngine::class.java.simpleName} üzerinden geri tuşu tetiklendi!")
            SmartNavigationEngine.navigateBack()
        }
    })
}

fun setupFragment(fragment: Fragment): Fragment{
    val args = SmartNavigationEngine.consumeArguments()
    args?.let {
        fragment.arguments = args
    }
    return fragment
}