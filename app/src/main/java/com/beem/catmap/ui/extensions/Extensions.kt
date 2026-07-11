package com.beem.catmap.ui.extensions

import android.view.View
import androidx.core.view.isVisible
import androidx.core.view.isGone
import androidx.core.view.isInvisible

fun View.fadeIn(duration: Long = 250) {
    if (this.isVisible && this.alpha == 1f) return

    this.visibility = View.VISIBLE
    this.animate()
        .alpha(1f)
        .setDuration(duration)
        .setListener(null)
        .start()
}

fun View.fadeOut(duration: Long = 250) {
    if (this.isGone || this.isInvisible) return

    this.animate()
        .alpha(0f)
        .setDuration(duration)
        .withEndAction {
            this.visibility = View.GONE
        }
        .start()
}