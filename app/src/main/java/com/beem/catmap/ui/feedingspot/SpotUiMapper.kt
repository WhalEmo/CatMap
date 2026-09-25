package com.beem.catmap.ui.feedingspot

import androidx.compose.ui.graphics.Color
import com.beem.catmap.R
import com.beem.catmap.data.model.ReportAction
import com.beem.catmap.data.model.SpotState
import com.beem.catmap.ui.theme.CatMapColors


// ==========================================
// 1. ANA DURUM (SPOT STATE) MAPPER
// ==========================================
fun SpotState.toUiBadge(): UiBadge {
    return when (this) {
        SpotState.FULL -> UiBadge(
            text = "Kaplar Dolu",
            contentColor = CatMapColors.Success,
            containerColor = CatMapColors.Success.copy(alpha = 0.12f),
            iconResId = R.drawable.ic_cat_food
        )

        SpotState.NEEDS_FOOD -> UiBadge(
            text = "Mama Bekliyor",
            contentColor = CatMapColors.Badge.Gold,
            containerColor = CatMapColors.Badge.Gold.copy(alpha = 0.15f),
            iconResId = R.drawable.ic_cat_food
        )

        SpotState.NEEDS_WATER -> UiBadge(
            text = "Su Bekliyor",
            contentColor = CatMapColors.Water,
            containerColor = CatMapColors.Water.copy(alpha = 0.12f),
            iconResId = R.drawable.ic_water_drop
        )

        SpotState.NEEDS_BOTH -> UiBadge(
            text = "Mama ve Su Bitmiş",
            contentColor = CatMapColors.Error,
            containerColor = CatMapColors.Error.copy(alpha = 0.12f),
            iconResId = R.drawable.ic_empty_bowl
        )

        SpotState.NEEDS_MAINTENANCE -> UiBadge(
            text = "Temizlik ve Bakım Lazım",
            contentColor = CatMapColors.MaintenanceOrange,
            containerColor = CatMapColors.MaintenanceOrange.copy(alpha = 0.15f),
            iconResId = R.drawable.ic_clean_sparkle
        )

        SpotState.OUT_OF_SERVICE -> UiBadge(
            text = "Nokta Aktif Değil",
            contentColor = CatMapColors.TextMuted,
            containerColor = CatMapColors.TextMuted.copy(alpha = 0.1f),
            iconResId = R.drawable.ic_broken_bowl
        )

        SpotState.UNKNOWN -> UiBadge(
            text = "Keşif Bekliyor",
            contentColor = CatMapColors.TextMuted,
            containerColor = CatMapColors.TextMuted.copy(alpha = 0.1f),
            iconResId = R.drawable.ic_clipboard
        )
    }
}

// ==========================================
// 2. AKSİYON / GÖZLEM (REPORT ACTION) MAPPER
// ==========================================
fun ReportAction.toUiBadge(): UiBadge {
    val water = R.drawable.ic_water_drop
    val food = R.drawable.ic_cat_food

    return when (this) {
        // İKMALLER (Yeşil ve Mavi Tonları)
        ReportAction.ACTION_ADDED_BOTH -> UiBadge(
            text = "Mama ve Su Eklendi",
            contentColor = CatMapColors.Success,
            containerColor = CatMapColors.Success.copy(alpha = 0.1f)
        )
        ReportAction.ACTION_ADDED_FOOD -> UiBadge(
            text = "Sadece Mama Eklendi",
            contentColor = CatMapColors.Success,
            containerColor = CatMapColors.Success.copy(alpha = 0.1f),
            iconResId = food
        )
        ReportAction.ACTION_ADDED_WATER -> UiBadge(
            text = "Sadece Su Eklendi",
            contentColor = CatMapColors.Water,
            containerColor = CatMapColors.Water.copy(alpha = 0.1f),
            iconResId = water
        )

        // GÖZLEMLER: DOLU (İkmal gerekmemiş - Yeşil/Mavi)
        ReportAction.OBSERVED_FOOD_FULL -> UiBadge(
            text = "Mama Kabı Dolu",
            contentColor = CatMapColors.Success,
            containerColor = CatMapColors.Success.copy(alpha = 0.1f),
            iconResId = food
        )
        ReportAction.OBSERVED_WATER_FULL -> UiBadge(
            text = "Su Kabı Dolu",
            contentColor = CatMapColors.Water,
            containerColor = CatMapColors.Water.copy(alpha = 0.1f),
            iconResId = water
        )

        // GÖZLEMLER: BOŞ (Uyarı/Hata Tonları)
        ReportAction.OBSERVED_FOOD_EMPTY -> UiBadge(
            text = "Mama Kabı Boş",
            contentColor = CatMapColors.Error,
            containerColor = CatMapColors.Error.copy(alpha = 0.1f),
            iconResId = food
        )
        ReportAction.OBSERVED_WATER_EMPTY -> UiBadge(
            text = "Su Kabı Boş",
            contentColor = CatMapColors.Error,
            containerColor = CatMapColors.Error.copy(alpha = 0.1f),
            iconResId = water
        )

        // BAKIM / ONARIM (Nötr / Marka Rengi)
        ReportAction.ACTION_CLEANED -> UiBadge(
            text = "Kaplar Temizlendi",
            contentColor = CatMapColors.CleanTeal,
            containerColor = CatMapColors.CleanTeal.copy(alpha = 0.1f),
            iconResId = R.drawable.ic_clean_sparkle
        )
        ReportAction.ACTION_REPAIRED -> UiBadge(
            text = "Kaplar Onarıldı",
            contentColor = CatMapColors.CleanTeal,
            containerColor = CatMapColors.CleanTeal.copy(alpha = 0.1f),
            iconResId = R.drawable.ic_repair_tools
        )

        // HASAR (Kritik)
        ReportAction.OBSERVED_DIRTY -> UiBadge(
            text = "Kaplar Kirli",
            contentColor = CatMapColors.MaintenanceOrange,
            containerColor = CatMapColors.MaintenanceOrange.copy(alpha = 0.15f),
            iconResId = R.drawable.ic_clean_sparkle
        )
        ReportAction.OBSERVED_DAMAGED -> UiBadge(
            text = "Kaplar Zarar Görmüş",
            contentColor = CatMapColors.Error,
            containerColor = CatMapColors.Error.copy(alpha = 0.1f),
            iconResId = R.drawable.ic_broken_bowl
        )
        ReportAction.UNKNOWN -> UiBadge(
            text = "İşlem Kaydı",
            contentColor = CatMapColors.TextMuted,
            containerColor = CatMapColors.TextMuted.copy(alpha = 0.1f),
            iconResId = R.drawable.ic_clipboard
        )
    }
}

fun String.getCustomSpotMarker(): Pair<Int, Int> {
    val state = SpotState.safeValueOf(this)

    // Senin colors.xml'deki marka renklerine ve UiBadge ikonlarına göre eşleştirme:
    return when (state) {
        SpotState.FULL -> Pair(R.color.catmap_success, R.drawable.ic_cat_food)
        SpotState.NEEDS_FOOD -> Pair(R.color.badge_gold, R.drawable.ic_cat_food)
        SpotState.NEEDS_WATER -> Pair(R.color.catmap_water, R.drawable.ic_water_drop) // 🚨 XML'e eklenecek
        SpotState.NEEDS_BOTH -> Pair(R.color.catmap_error, R.drawable.ic_empty_bowl)
        SpotState.NEEDS_MAINTENANCE -> Pair(R.color.badge_catmap_orange, R.drawable.ic_clean_sparkle)
        SpotState.OUT_OF_SERVICE -> Pair(R.color.catmap_text_muted, R.drawable.ic_broken_bowl)
        else -> Pair(R.color.catmap_text_muted, R.drawable.ic_clipboard) // UNKNOWN
    }
}


fun getDecayLevel(minutesAgo: Long): DecayLevel {
    return DecayLevel.entries.first { minutesAgo <= it.maxMinutes }
}

fun DecayLevel.toUiBadge(): UiBadge {
    return when (this) {
        DecayLevel.FRESH -> UiBadge("Ortam Ferah", CatMapColors.Success, CatMapColors.Success.copy(alpha = 0.12f))
        DecayLevel.WARNING -> UiBadge("Kontrol Vakti", CatMapColors.Badge.Gold, CatMapColors.Badge.Gold.copy(alpha = 0.15f))
        DecayLevel.CRITICAL -> UiBadge("Acil Müdahale", CatMapColors.Error, CatMapColors.Error.copy(alpha = 0.12f))
    }
}