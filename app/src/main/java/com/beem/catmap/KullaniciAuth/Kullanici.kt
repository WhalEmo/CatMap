package com.beem.catmap.KullaniciAuth

import android.graphics.Bitmap

data class Kullanici(
    @JvmField var id: String = "",
    @JvmField var ad: String = "",
    @JvmField var soyad: String = "",
    @JvmField var email: String = "",
    @JvmField var kullaniciAdi: String = "",
    @JvmField var sifre: String = "",
    @JvmField var latitude: Double = 0.0,
    @JvmField var longitude: Double = 0.0,
    @JvmField var girisBasarili: Boolean = false,
    @JvmField var fotoUrl: String = "",
    @JvmField var fotoBitmap: Bitmap? = null,
    @JvmField var sonGorulme: Long = 0L,
    @JvmField var isCevrimiciMi: Boolean = false,
    @JvmField var takipEdiyorMuyum: Int = 0,
    @JvmField var takipciMi: Int = 0,
    @JvmField var takipciSayisi: Long? = 0L,
    @JvmField var takipEdilenSayisi: Long? = 0L,
    @JvmField var gonderiSayisi: Long? = 0L,
    @JvmField var biyografi: String = "",
    @JvmField var isProfileLoaded: Boolean = false
) {
    fun KullaniciData(): Map<String, Any?> {
        return mapOf(
            "Ad" to ad,
            "Soyad" to soyad,
            "Email" to email,
            "KullaniciAdi" to kullaniciAdi,
            "Hakkinda" to biyografi,
            "TakipEdilenSayisi" to (takipEdilenSayisi ?: 0L),
            "takipciSayisi" to (takipciSayisi ?: 0L),
            "gonderiSayisi" to (gonderiSayisi ?: 0L),
            "profilFotoUrl" to (fotoUrl.ifEmpty { "" }),
            "latitude" to latitude,
            "longitude" to longitude,
            "isCevrimiciMi" to isCevrimiciMi,
            "sonGorulme" to sonGorulme
        )
    }
}