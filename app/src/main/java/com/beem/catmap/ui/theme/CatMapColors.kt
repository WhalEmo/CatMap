package com.beem.catmap.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * CatMap Tasarım Sistemi Renk Paleti (Design Tokens).
 * XML 'colors.xml' dosyasının birebir Compose karşılığıdır.
 */
object CatMapColors {

    // =========================================================================
    // 1. TEMEL VE NÖTR RENKLER
    // =========================================================================
    val Black = Color(0xFF000000)
    val White = Color(0xFFFFFFFF)
    val Transparent = Color(0x00000000)
    val SoftGray = Color(0x26000000)

    // =========================================================================
    // 2. MARKA (BRAND) & ANA RENKLER
    // =========================================================================
    val Primary = Color(0xFF1E293B)          // Koyu Slate (Gece Modu / Marka Mavisi)
    val PrimaryDark = Color(0xFF0F172A)      // En Koyu Slate
    val Background = Color(0xFFF8FAFC)       // Genel Arka Plan Rengi

    // Marka Vurgusu: CatMap Turuncusu
    val Accent = Color(0xFFF97316)
    val AccentAlpha15 = Color(0x26F97316)
    val AccentAlpha20 = Color(0x33F97316)

    // =========================================================================
    // 3. YÜZEY (SURFACE) RENKLERİ
    // =========================================================================
    val SurfaceWhite = Color(0xFFFFFFFF)
    val SurfaceWhiteTrans80 = Color(0xCCFFFFFF)
    val SurfaceWhiteTrans70 = Color(0xB3FFFFFF)
    val SurfaceWhiteTrans60 = Color(0x99FFFFFF)
    val SurfaceTranslucent = Color(0xE6F8FAFC)
    val SurfaceBlur = Color(0xE6FFFFFF)

    // =========================================================================
    // 4. BORDER & DİVİDER
    // =========================================================================
    val Border = Color(0xFF334155)
    val Divider = Color(0xFFE2E8F0)

    // =========================================================================
    // 5. TİPOGRAFİ (METİN) RENKLERİ
    // =========================================================================
    val TextPrimary = Color(0xFF0F172A)      // Başlıklar, kullanıcı adları, ana metinler
    val TextSecondary = Color(0xFF475569)    // İkincil açıklamalar
    val TextDark = Color(0xFF0F172A)
    val TextMuted = Color(0xFF64748B)        // Pasif / gri metinler, zaman damgaları
    val TextLight = Color(0xFFFFFFFF)        // Koyu zemin üzeri beyaz metin

    // =========================================================================
    // 6. GERİ BİLDİRİM VE DURUM RENKLERİ
    // =========================================================================
    val Success = Color(0xFF10B981)          // Yeşil
    val Error = Color(0xFFEF4444)            // Kırmızı / Engel / Hata
    val IndicatorCapsule = Color(0x4DFFFFFF)

    // =========================================================================
    // 7. ROZET (BADGE) SİSTEMİ RENKLERİ
    // =========================================================================
    object Badge {
        // Ortak Rozet Renkleri
        val CardBg = Color(0xFF1E293B)
        val ProgressBg = Color(0xFF334155)
        val Ink = Color(0xFF1E293B)
        val DeepNavy = Color(0xFF0F172A)
        val CatMapOrange = Color(0xFFF97316)
        val SoftCream = Color(0xFFFFF7ED)
        val Gold = Color(0xFFF59E0B)
        val LightGold = Color(0xFFFDE68A)
        val Silver = Color(0xFF94A3B8)
        val LightSilver = Color(0xFFE2E8F0)
        val Bronze = Color(0xFFB45309)
        val BronzeLight = Color(0xFFD97706)
        val BurntOrange = Color(0xFFC2410C)

        // Tier 08 Özel Detay Kart Renkleri
        object Tier08Special {
            val CardBg = Color(0xFF0B1220)
            val CardStroke = Color(0xC8A44D)
            val Title = Color(0xFFFFF8E1)
            val Text = Color(0xE5D7A3)
            val Value = Color(0xFFFDE68A)
            val Progress = Color(0xFFF59E0B)
            val Track = Color(0xFF24324A)
            val ChipBg = Color(0xFF111A2C)
            val ChipStroke = Color(0xD4AF37)
            val ChipText = Color(0xFFFDE68A)
        }

        // Kademeli Rozet UI Renk Modeli (Progress bar ve Card arka planları için)
        @Immutable
        data class TierUiColors(
            val accent: Color,
            val surface: Color,
            val track: Color
        )

        val Tier01 = TierUiColors(
            accent = Color(0xFF64748B),
            surface = Color(0xFFF8FAFC),
            track = Color(0xFFE2E8F0)
        )

        val Tier02 = TierUiColors(
            accent = Color(0xFFB45309),
            surface = Color(0xFFFFF7ED),
            track = Color(0xFFFED7AA)
        )

        val Tier03 = TierUiColors(
            accent = Color(0xFF9A3412),
            surface = Color(0xFFFFF1E6),
            track = Color(0xFFFDBA74)
        )

        val Tier04 = TierUiColors(
            accent = Color(0xFF64748B),
            surface = Color(0xFFF1F5F9),
            track = Color(0xFFCBD5E1)
        )

        val Tier05 = TierUiColors(
            accent = Color(0xFF334155),
            surface = Color(0xFFE2E8F0),
            track = Color(0xFF94A3B8)
        )

        val Tier06 = TierUiColors(
            accent = Color(0xFFD97706),
            surface = Color(0xFFFFFBEB),
            track = Color(0xFFFDE68A)
        )

        val Tier07 = TierUiColors(
            accent = Color(0xFFF97316),
            surface = Color(0xFFFFF7ED),
            track = Color(0xFFFDBA74)
        )

        val Tier08 = TierUiColors(
            accent = Color(0xFF0F172A),
            surface = Color(0xFFFEF3C7),
            track = Color(0xFFF59E0B)
        )

        /**
         * Tier seviyesine (1-8) göre otomatik renk paketini döndürür.
         */
        fun getUiColorsForTier(tier: Int): TierUiColors {
            return when (tier) {
                1 -> Tier01
                2 -> Tier02
                3 -> Tier03
                4 -> Tier04
                5 -> Tier05
                6 -> Tier06
                7 -> Tier07
                8 -> Tier08
                else -> Tier01
            }
        }
    }
}