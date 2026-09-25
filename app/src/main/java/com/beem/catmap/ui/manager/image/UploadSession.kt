package com.beem.catmap.ui.manager.image

enum class UploadSession(
    val sessionKey: String,
    val maxImageCount: Int,
    val allowGallery: Boolean
) {
    GENERAL(
        sessionKey = "GENERAL_UPLOAD",
        maxImageCount = 5,
        allowGallery = true
    ),
    REPORT(
        sessionKey = "REPORT_UPLOAD",
        maxImageCount = 1,
        allowGallery = false // Anti-troll: Rapor formunda galeri kapalı, sadece anlık çekim!
    ),
    // 🚀 GELECEK VİZYONU (Senin bahsettiğin Sohbet ve Profil altyapısı şimdiden hazır)
    CHAT(
        sessionKey = "CHAT_UPLOAD",
        maxImageCount = 10,
        allowGallery = true // Sohbette bolca resim ve galeri serbest
    ),
    PROFILE(
        sessionKey = "PROFILE_UPLOAD",
        maxImageCount = 1,
        allowGallery = true // Profil fotoğrafı için tek resim
    );

    companion object {
        fun fromKey(key: String?): UploadSession {
            return entries.firstOrNull { it.sessionKey == key || it.name == key } ?: GENERAL
        }
    }
}