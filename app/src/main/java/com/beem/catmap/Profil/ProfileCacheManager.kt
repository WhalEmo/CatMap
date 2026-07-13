package com.beem.catmap.Profil

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

object ProfileCacheManager {
    private const val PREF_NAME = "ProfilPrefs"
    private const val KEY_PREFIX = "profile_url_"
    const val VALUE_EMPTY = "null"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveProfileUrl(context: Context, userId: String, url: String?) {
        if (userId.isBlank()) return
        val finalUrl = if (url.isNullOrBlank()) VALUE_EMPTY else url

        getPrefs(context).edit {
            putString("$KEY_PREFIX$userId", finalUrl)
        }
    }

    fun getProfileUrl(context: Context, userId: String): String? {
        if (userId.isBlank()) return null
        val cachedValue = getPrefs(context).getString("$KEY_PREFIX$userId", null)

        if (cachedValue == VALUE_EMPTY || cachedValue.isNullOrBlank()) {
            return null
        }
        return cachedValue
    }

    fun isCacheEmpty(context: Context, userId: String): Boolean {
        return getPrefs(context).getString("$KEY_PREFIX$userId", null) == null
    }

    fun clearCache(context: Context, userId: String) {
        if (userId.isBlank()) return
        getPrefs(context).edit {
            remove("$KEY_PREFIX$userId")
        }
    }
}