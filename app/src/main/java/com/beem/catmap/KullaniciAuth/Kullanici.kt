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
    //BURASI YANLIS
    fun KullaniciData(): Map<String, Any?> {
        return mapOf(
            "ID" to id,
            "Ad" to ad,
            "Soyad" to soyad,
            "Email" to email,
            "KullaniciAdi" to kullaniciAdi,
            "latitude" to latitude,
            "longitude" to longitude,
            "girisBasarili" to girisBasarili,
            "profilFotoUrl" to fotoUrl,
            "sonGorulme" to sonGorulme,
            "isCevrimiciMi" to isCevrimiciMi,
            "TakipEdiyorMuyum" to takipEdiyorMuyum,
            "TakipciMi" to takipciMi,
            "takipciSayisi" to takipciSayisi,
            "TakipEdilenSayisi" to takipEdilenSayisi,
            "gonderiSayisi" to gonderiSayisi,
            "Hakkinda" to biyografi
        )
    }
}