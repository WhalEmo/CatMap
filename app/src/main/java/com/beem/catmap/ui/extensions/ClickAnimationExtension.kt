package com.beem.catmap.ui.extensions

import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator

fun View.bounceAndHaptic() {
    performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)

    animate()
        .scaleX(0.90f)
        .scaleY(0.90f)
        .setDuration(70)
        .withEndAction {
            animate()
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f)
                .setDuration(120)
                .start()
        }
        .start()
}


fun View.fadeInSmooth(duration: Long = 100) {
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
fun View.fadeOutSmooth(duration: Long = 100) {
    if (this.visibility == View.GONE) return

    this.animate().cancel()
    this.animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            this.visibility = View.GONE
        }
}