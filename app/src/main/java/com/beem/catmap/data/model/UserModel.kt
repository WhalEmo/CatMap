package com.beem.catmap.data.model

import android.graphics.Bitmap
import java.io.Serializable

data class UserModel(
    @JvmField var id: String = "",
    @JvmField var name: String = "",
    @JvmField var surname: String = "",
    @JvmField var email: String = "",
    @JvmField var username: String = "",
    @JvmField var password: String = "",
    @JvmField var latitude: Double = 0.0,
    @JvmField var longitude: Double = 0.0,
    @JvmField var loginSuccess: Boolean = false,
    @JvmField var photoUrl: String = "",
    @Transient @JvmField var photoBitmap: Bitmap? = null,
    @JvmField var lastSeen: Long = 0L,
    @JvmField var isOnline: Boolean = false,
    @JvmField var isFollowing: Int = 0,
    @JvmField var isFollowers: Int = 0,
    @JvmField var followersCount: Long? = 0L,
    @JvmField var followingCount: Long? = 0L,
    @JvmField var postCount: Long? = 0L,
    @JvmField var bio: String = "",
    @JvmField var isProfileLoaded: Boolean = false,
    @Transient @JvmField var equippedBadge: EquippedBadgeModel? = null
): Serializable {
    fun KullaniciData(): Map<String, Any?> {
        return mapOf(
            "Ad" to name,
            "Soyad" to surname,
            "Email" to email,
            "KullaniciAdi" to username,
            "Hakkinda" to bio,
            "TakipEdilenSayisi" to (followingCount ?: 0L),
            "takipciSayisi" to (followersCount ?: 0L),
            "gonderiSayisi" to (postCount ?: 0L),
            "profilFotoUrl" to (photoUrl.ifEmpty { "" }),
        )
    }
}