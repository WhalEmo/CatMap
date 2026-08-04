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


fun Fragment.fragmentLog(durum: String) {
    val tagStr = this.tag ?: "TAG_YOK"
    val className = this::class.java.simpleName

    val argsStr = this.arguments?.let { bundle ->
        bundle.keySet().joinToString(", ") { key -> "$key=${bundle.get(key)}" }
    } ?: "Argüman Yok"

    val stateReport = buildString {
        append("Added: ${this@fragmentLog.isAdded} | ")
        append("Visible: ${this@fragmentLog.isVisible} | ")
        append("Resumed: ${this@fragmentLog.isResumed} | ")
        append("Hidden: ${this@fragmentLog.isHidden} | ")
        append("Detached: ${this@fragmentLog.isDetached}")
    }

    Log.d("NAV_BACK_DEDEKTOR", "╔═════════ 📊 FRAGMENT RAPORU ($durum) ═════════")
    Log.d("NAV_BACK_DEDEKTOR", "║ 📦 Sınıf: $className")
    Log.d("NAV_BACK_DEDEKTOR", "║ 🏷️ Tag  : $tagStr")
    Log.d("NAV_BACK_DEDEKTOR", "║ 🩺 Durum: $stateReport")
    Log.d("NAV_BACK_DEDEKTOR", "║ 🔑 Veriler: [$argsStr]")
    Log.d("NAV_BACK_DEDEKTOR", "╚════════════════════════════════════════════════")
}

fun String.extractBaseTag(): String {
    val matchingEnum = Screen.entries.firstOrNull { this.startsWith(it.tag) }
    if (matchingEnum != null) {
        return matchingEnum.tag
    }

    val lastUnderScoreIndex = this.lastIndexOf("_")
    return if (lastUnderScoreIndex != -1) {
        this.substring(0, lastUnderScoreIndex)
    } else {
        this
    }
}