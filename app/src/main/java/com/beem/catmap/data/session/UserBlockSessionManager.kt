package com.beem.catmap.data.session

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class BlockSessionManager private constructor(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    companion object {
        private const val PREF_NAME = "BlockPrefs"
        private const val KEY_BENIM_ENGELLEDIKLERIM = "cache_benim_engellenenler"

        @Volatile
        private var INSTANCE: BlockSessionManager? = null

        fun getInstance(context: Context): BlockSessionManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BlockSessionManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    /**
     * Engellenenler listesini SharedPreferences'a kaydeder (Virgülle birleştirerek saklar)
     */
    fun saveBenimEngellediklerim(liste: List<String>) {
        val joinedString = liste.joinToString(separator = ",")
        prefs.edit {
            putString(KEY_BENIM_ENGELLEDIKLERIM, joinedString)
        }
    }

    /**
     * SharedPreferences'tan engellenenler listesini okur ve List<String> olarak döndürür
     */
    fun getBenimEngellediklerim(): List<String> {
        val savedString = prefs.getString(KEY_BENIM_ENGELLEDIKLERIM, "") ?: ""
        if (savedString.isBlank()) return emptyList()
        return savedString.split(",")
    }

    /**
     * Cache'i temizler
     */
    fun clearBlockCache() {
        prefs.edit { clear() }
    }
}