package com.beem.catmap.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class ProfileSessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "ProfilPrefs"

        private const val KEY_TAKIPCI_SAYISI = "cache_takipci"
        private const val KEY_TAKIP_EDILEN_SAYISI = "cache_takip"
        private const val KEY_GONDERI_SAYISI = "cache_gonderi"
        private const val KEY_BIYOGRAFI = "cache_biyografi"

        @Volatile
        private var INSTANCE: ProfileSessionManager? = null

        fun getInstance(context: Context): ProfileSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ProfileSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Tüm profil detaylarını tek seferde ProfilPrefs dosyasına kaydeder
     */
    fun saveProfileDetails(
        takipciSayisi: Long,
        takipEdilenSayisi: Long,
        gonderiSayisi: Long = 0L,
        biyografi: String? = null
    ) {
        prefs.edit {
            putLong(KEY_TAKIPCI_SAYISI, takipciSayisi)
            putLong(KEY_TAKIP_EDILEN_SAYISI, takipEdilenSayisi)
            putLong(KEY_GONDERI_SAYISI, gonderiSayisi)
            biyografi?.let { putString(KEY_BIYOGRAFI, it) }
        }
    }

    /**
     * Sadece Takip ve Takipçi sayılarını günceller
     */
    fun saveFollowCounts(takipciSayisi: Long, takipEdilenSayisi: Long) {
        prefs.edit {
            putLong(KEY_TAKIPCI_SAYISI, takipciSayisi)
            putLong(KEY_TAKIP_EDILEN_SAYISI, takipEdilenSayisi)
        }
    }

    /**
     * Sadece Gönderi sayısını günceller
     */
    fun saveGonderiSayisi(gonderiSayisi: Long) {
        prefs.edit {
            putLong(KEY_GONDERI_SAYISI, gonderiSayisi)
        }
    }

    /**
     * Sadece Biyografi bilgisini günceller
     */
    fun saveBiyografi(biyografi: String) {
        prefs.edit {
            putString(KEY_BIYOGRAFI, biyografi)
        }
    }


    fun getTakipciSayisi(): Long = prefs.getLong(KEY_TAKIPCI_SAYISI, 0L)

    fun getTakipEdilenSayisi(): Long = prefs.getLong(KEY_TAKIP_EDILEN_SAYISI, 0L)

    fun getGonderiSayisi(): Long = prefs.getLong(KEY_GONDERI_SAYISI, 0L)

    fun getBiyografi(): String = prefs.getString(KEY_BIYOGRAFI, "") ?: ""

    /**
     * Çıkış yapıldığında sadece profil cache'ini temizler
     */
    fun clearProfileCache() {
        prefs.edit { clear() }
    }
}