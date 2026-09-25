package com.beem.catmap.utils

import java.text.DecimalFormat

// 🚀 ZARİF BİR KONTROL MEKANİZMASI: Kısa mı, uzun mu?
enum class DistanceFormat {
    SHORT, // m, km
    LONG   // metre, kilometre
}

/**
 * Mesafeyi zekice hesaplayıp Değer ve Birim olarak Pair döndürür.
 * Örn (SHORT): 850 -> Pair("850", "m") | 1500 -> Pair("1.5", "km")
 * Örn (LONG):  850 -> Pair("850", "metre") | 1500 -> Pair("1.5", "kilometre")
 */
fun Int?.toSmartDistancePair(format: DistanceFormat = DistanceFormat.SHORT): Pair<String, String> {
    if (this == null) return Pair("-", "")

    return if (this < 1000) {
        val unit = if (format == DistanceFormat.LONG) "metre" else "m"
        Pair(this.toString(), unit)
    } else {
        val df = DecimalFormat("#.#")
        val kmValue = df.format(this / 1000.0)
        val unit = if (format == DistanceFormat.LONG) "kilometre" else "km"
        Pair(kmValue, unit)
    }
}

/**
 * Üstteki Pair'i kullanarak UI'da doğrudan göstermelik metin üretir.
 * Çok esnektir; istersen sonundaki "uzakta" yazısını kaldırabilir veya formatı değiştirebilirsin.
 */
fun Int?.toSmartDistanceString(
    fallback: String = "Konum doğrulanıyor...",
    format: DistanceFormat = DistanceFormat.SHORT,
    suffix: String = "uzakta"
): String {
    if (this == null) return fallback

    val (value, unit) = this.toSmartDistancePair(format)

    // Tasarım Hilesi: Kısa birimlerde (m/km) sayıya bitişik yazılır (15km), uzunlarda ayrı (15 kilometre)
    val space = if (format == DistanceFormat.LONG) " " else ""
    val suffixText = if (suffix.isNotBlank()) " $suffix" else ""

    return "$value$space$unit$suffixText"
}