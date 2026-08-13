package com.beem.catmap.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.beem.catmap.data.model.UserModel

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

        // TTL Zaman Damgası Key'i
        private const val KEY_LAST_STATS_FETCH_TIME = "LastStatsFetchTime"

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
    fun saveUserSession(userModel: UserModel) {
        prefs.edit().apply {
            putString(KEY_ID, userModel.id)
            putString(KEY_AD, userModel.name)
            putString(KEY_SOYAD, userModel.surname)
            putString(KEY_EMAIL, userModel.email)
            putString(KEY_USERNAME, userModel.username)
            putString(KEY_PASSWORD, userModel.password)
            putString(KEY_FOTO_URL, userModel.photoUrl)
            putBoolean(KEY_IS_LOGGED_IN, true)
            userModel.postCount?.let { putLong(KEY_POST_COUNT, it) }
            userModel.followersCount?.let { putLong(KEY_TAKIPCI_SAYISI, it) }
            userModel.followingCount?.let { putLong(KEY_TAKIP_EDILEN_SAYISI, it) }
            putString(KEY_BIYOGRAFI, userModel.bio)
            apply() // Disk yazımını arka planda asenkron yapar
        }
    }

    /**
     * Diskten kayıtlı kullanıcıyı çekip nesneye dönüştürür
     */
    fun getUserSession(): UserModel? {
        if (!isLoggedIn()) return null

        return UserModel().apply {
            id = prefs.getString(KEY_ID, "") ?: ""
            name = prefs.getString(KEY_AD, "") ?: ""
            surname = prefs.getString(KEY_SOYAD, "") ?: ""
            email = prefs.getString(KEY_EMAIL, "") ?: ""
            username = prefs.getString(KEY_USERNAME, "") ?: ""
            password = prefs.getString(KEY_PASSWORD, "") ?: ""
            photoUrl = prefs.getString(KEY_FOTO_URL, "") ?: ""
            bio = prefs.getString(KEY_BIYOGRAFI, "") ?: ""
            followersCount = prefs.getLong(KEY_TAKIPCI_SAYISI, 0)
            followingCount = prefs.getLong(KEY_TAKIP_EDILEN_SAYISI, 0)
            postCount = prefs.getLong(KEY_POST_COUNT, 0)
        }
    }

    /**
     * Sayaçların son çekilme zamanını kaydeder
     */
    fun saveLastStatsFetchTime(time: Long) {
        prefs.edit { putLong(KEY_LAST_STATS_FETCH_TIME, time) }
    }

    /**
     * Sayaçların son çekilme zamanını okur (varsayılan: 0)
     */
    fun getLastStatsFetchTime(): Long {
        return prefs.getLong(KEY_LAST_STATS_FETCH_TIME, 0L)
    }

    /**
     * Kullanıcı oturum açmış mı kontrolü
     */
    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    }

    /**
     * Çıkış Yap (Logout) - Yerel veriyi ve zaman damgasını sıfırlar
     */
    fun clearSession() {
        prefs.edit { clear() }
    }
}