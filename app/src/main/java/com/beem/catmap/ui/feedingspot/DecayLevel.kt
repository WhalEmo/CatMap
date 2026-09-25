package com.beem.catmap.ui.feedingspot

enum class DecayLevel(val maxMinutes: Long) {
    FRESH(6 * 60),
    WARNING(18 * 60),
    CRITICAL(Long.MAX_VALUE)
}