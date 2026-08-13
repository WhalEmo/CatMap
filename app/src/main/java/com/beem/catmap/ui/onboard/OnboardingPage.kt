package com.beem.catmap.ui.onboard
import androidx.annotation.RawRes
data class OnboardingPage(
    val title: String,
    val description: String,
    @RawRes val animationResId1: Int,
    @RawRes val animationResId2: Int
)