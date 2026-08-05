package com.beem.catmap.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.beem.catmap.KullaniciAuth.Kullanici

class UserSessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "KullaniciKayit"

        // Key Tanımlamaları
        private const val KEY_ID = "ID"
        private const val KEY_AD = "Ad"
        private const val KEY_SOYAD = "Soyad"
        private const val KEY_EMAIL = "Email"
        private const val KEY_USERNAME = "KullaniciAdi"
        private const val KEY_PASSWORD = "Sifre"
        private const val KEY_IS_LOGGED_IN = "GirisYapildi"
        private const val KEY_FOTO_URL = "FotoUrl"

        private const val KEY_TAKIPCI_SAYISI = "TakipciSayisi"
        private const val KEY_TAKIP_EDILEN_SAYISI = "TakipEdilenSayisi"
        private const val KEY_BIYOGRAFI = "Biyografi"
        private const val KEY_POST_COUNT = "PostCount"

        @Volatile
        private var INSTANCE: UserSessionManager? = null

        fun getInstance(context: Context): UserSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Kullanıcıyı yerel disk belleğine mühürler
     */
    fun saveUserSession(kullanici: Kullanici) {
        prefs.edit().apply {
            putString(KEY_ID, kullanici.id)
            putString(KEY_AD, kullanici.ad)
            putString(KEY_SOYAD, kullanici.soyad)
            putString(KEY_EMAIL, kullanici.email)
            putString(KEY_USERNAME, kullanici.kullaniciAdi)
            putString(KEY_PASSWORD, kullanici.sifre)
            putString(KEY_FOTO_URL, kullanici.fotoUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            kullanici.gonderiSayisi?.let { putLong(KEY_POST_COUNT, it) }
            kullanici.takipciSayisi?.let { putLong(KEY_TAKIPCI_SAYISI,it) }
            kullanici.takipEdilenSayisi?.let { putLong(KEY_TAKIP_EDILEN_SAYISI,it) }
            putString(KEY_BIYOGRAFI,kullanici.biyografi)
            apply() // Disk yazımını arka planda asenkron yapar
        }
    }


    /**
     * Diskten kayıtlı kullanıcıyı çekip nesneye dönüştürür
     */
    fun getUserSession(): Kullanici? {
        if (!isLoggedIn()) return null

        return Kullanici().apply {
            id = prefs.getString(KEY_ID, "") ?: ""
            ad = prefs.getString(KEY_AD, "") ?: ""
            soyad = prefs.getString(KEY_SOYAD, "") ?: ""
            email = prefs.getString(KEY_EMAIL, "") ?: ""
            kullaniciAdi = prefs.getString(KEY_USERNAME, "") ?: ""
            sifre = prefs.getString(KEY_PASSWORD, "") ?: ""
            fotoUrl = prefs.getString(KEY_FOTO_URL, "") ?: ""
            biyografi = prefs.getString(KEY_BIYOGRAFI,"") ?: ""
            takipciSayisi = prefs.getLong(KEY_TAKIPCI_SAYISI,0)
            takipEdilenSayisi = prefs.getLong(KEY_TAKIP_EDILEN_SAYISI,0)
            gonderiSayisi = prefs.getLong(KEY_POST_COUNT,0)
        }
    }

    /**
     * Kullanıcı oturum açmış mı kontrolü
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Çıkış Yap (Logout) - Yerel veriyi sıfırlar
     */
    fun clearSession() {
        prefs.edit { clear() }
    }
}