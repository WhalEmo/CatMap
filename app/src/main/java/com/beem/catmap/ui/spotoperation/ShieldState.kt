package com.beem.catmap.ui.spotoperation

enum class ShieldState {
    SCANNING,   // Radar dönüyor, konum aranıyor
    VERIFIED,   // Konum onaylandı (Kilitler açıldı)
    TOO_FAR,    // 50 metreden uzak
    ERROR       // GPS kapalı veya izin yok
}