package com.beem.catmap.ui.feedingspot

import androidx.compose.ui.graphics.Color
import com.beem.catmap.R


data class UiBadge(
    val text: String,
    val contentColor: Color,
    val containerColor: Color,
    val borderColor: Color = contentColor.copy(alpha = 0.4f),
    val iconResId: Int = R.drawable.patidolu
)