package com.beem.catmap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.beem.catmap.R

// Klasik Android sistem fontu (Roboto / Varsayılan Sistem Fontu)
val DefaultSystemFont = FontFamily.Default

// Tüm ekranlarda "fontFamily = PlusJakartaSans" yazan yerler
// artık tek satırla standart Android fontuna döner:
val PlusJakartaSans = DefaultSystemFont

val Montserrat = DefaultSystemFont

val Rubik = FontFamily(
    Font(R.font.rubik_medium, FontWeight.Medium)
)

// 2. Material3 Typography Eşlemesi
val CatMapTypography = Typography(
    titleLarge = TextStyle(
        fontFamily = DefaultSystemFont,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = CatMapColors.TextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = DefaultSystemFont,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        color = CatMapColors.TextPrimary
    ),
    headlineSmall = TextStyle(
        fontFamily = DefaultSystemFont,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        color = CatMapColors.TextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = DefaultSystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = CatMapColors.TextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = DefaultSystemFont,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = CatMapColors.TextSecondary
    ),
    labelLarge = TextStyle(
        fontFamily = DefaultSystemFont,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontFamily = DefaultSystemFont,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        color = CatMapColors.TextMuted
    )
)