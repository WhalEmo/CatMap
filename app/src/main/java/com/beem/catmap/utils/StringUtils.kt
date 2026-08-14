package com.beem.catmap.utils

import java.util.Locale

private val TURKISH_LOCALE = Locale("tr", "TR")
private const val TURKISH_VOWELS = "aeıioöuü"

fun String.withPossessiveSuffix(): String {
    if (isBlank()) return this

    val original = trim()
    val normalized = original.lowercase(TURKISH_LOCALE)

    val lastVowel = normalized
        .lastOrNull { it in TURKISH_VOWELS }
        ?: return "$original'ın"

    val suffix = when (lastVowel) {
        'a', 'ı' -> "ın"
        'e', 'i' -> "in"
        'o', 'u' -> "un"
        'ö', 'ü' -> "ün"
        else -> "ın"
    }

    val endsWithVowel =
        normalized.lastOrNull()?.let { it in TURKISH_VOWELS } == true

    return if (endsWithVowel) {
        "$original'n$suffix"
    } else {
        "$original'$suffix"
    }
}