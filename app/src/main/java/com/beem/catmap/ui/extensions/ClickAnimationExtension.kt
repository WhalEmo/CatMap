package com.beem.catmap.ui.extensions

import android.view.HapticFeedbackConstants
import android.view.View

fun View.bounceAndHaptic() {
    this.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    this.animate()
        .scaleX(0.92f)
        .scaleY(0.92f)
        .setDuration(80)
        .withEndAction {
            this.animate()
                .scaleX(1.0f)
                .scaleY(1.0f)
                .setDuration(100)
                .start()
        }
        .start()
}

/**
 * Görünümü yumuşakça saydamlaştırarak belirginleştirir (Fade In).
 */
fun View.fadeInSmooth(duration: Long = 200) {
    if (this.visibility == View.VISIBLE && this.alpha == 1f) return

    this.animate().cancel()
    this.visibility = View.VISIBLE
    this.animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
}

/**
 * Görünümü yumuşakça saydamlaştırarak gizler (Fade Out).
 */
fun View.fadeOutSmooth(duration: Long = 200) {
    if (this.visibility == View.GONE) return

    this.animate().cancel()
    this.animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            this.visibility = View.GONE
        }
}