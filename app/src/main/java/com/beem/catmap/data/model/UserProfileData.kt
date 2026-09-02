package com.beem.catmap.data.model

import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class UserProfileData(
    @DocumentId
    val id: String = "",

    @get:PropertyName("Ad")
    @set:PropertyName("Ad")
    var name: String = "",

    @get:PropertyName("Soyad")
    @set:PropertyName("Soyad")
    var surname: String = "",

    @get:PropertyName("Email")
    @set:PropertyName("Email")
    var email: String = "",

    @get:PropertyName("KullaniciAdi")
    @set:PropertyName("KullaniciAdi")
    var username: String = "",

    @get:PropertyName("Hakkinda")
    @set:PropertyName("Hakkinda")
    var bio: String = "",

    @get:PropertyName("profilFotoUrl")
    @set:PropertyName("profilFotoUrl")
    var photoUrl: String = "",

    @get:PropertyName("takipciSayisi")
    @set:PropertyName("takipciSayisi")
    var followersCount: Long = 0L,

    @get:PropertyName("TakipEdilenSayisi")
    @set:PropertyName("TakipEdilenSayisi")
    var followingCount: Long = 0L,

    @get:PropertyName("gonderiSayisi")
    @set:PropertyName("gonderiSayisi")
    var postCount: Long = 0L,

    @get:PropertyName("begendigiGonderiler")
    @set:PropertyName("begendigiGonderiler")
    var likedPosts: List<String> = emptyList(),

    @get:PropertyName("equippedBadge")
    @set:PropertyName("equippedBadge")
    var equippedBadge: EquippedBadgeModel? = null
) {
    val fullName: String
        get() = "$name $surname".trim()

    /**
     * Firestore güncelleme (update) işlemlerinde direkt map olarak göndermek için.
     */
    fun toFirestoreMap(): Map<String, Any?> {
        return mapOf(
            "Ad" to name,
            "Soyad" to surname,
            "Email" to email,
            "KullaniciAdi" to username,
            "Hakkinda" to bio,
            "TakipEdilenSayisi" to followingCount,
            "takipciSayisi" to followersCount,
            "gonderiSayisi" to postCount,
            "profilFotoUrl" to photoUrl
        )
    }
}