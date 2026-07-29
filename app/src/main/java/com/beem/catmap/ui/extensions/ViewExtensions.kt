package com.beem.catmap.ui.extensions

import android.view.animation.Animation
import android.view.animation.ScaleAnimation
import android.widget.ImageView

fun ImageView.kalpAnimasyonuYap() {
    val buyutKucult = ScaleAnimation(
        0.7f, 1.2f,
        0.7f, 1.2f,
        Animation.RELATIVE_TO_SELF, 0.5f,
        Animation.RELATIVE_TO_SELF, 0.5f
    ).apply {
        duration = 200
        repeatCount = 1
        repeatMode = Animation.REVERSE
    }

    this.startAnimation(buyutKucult)
}