package com.beem.catmap.data.session

import android.content.Context
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.session.UserSessionManager

class CurrentUserManager private constructor(context: Context) {

    private val sessionManager = UserSessionManager.Companion.getInstance(context)

    private var currentUserCache: Kullanici? = null

    companion object {
        @Volatile
        private var INSTANCE: CurrentUserManager? = null

        fun getInstance(context: Context): CurrentUserManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CurrentUserManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    fun getCurrentUser(): Kullanici {
        if (currentUserCache == null) {
            currentUserCache = sessionManager.getUserSession() ?: Kullanici()
        }
        return currentUserCache!!
    }

    fun getCurrentUserId(): String {
        return getCurrentUser().id
    }

    /**
     * Yeni giriş veya kayıt sonrasında kullanıcıyı günceller ve kaydeder
     */
    fun setCurrentUser(kullanici: Kullanici) {
        this.currentUserCache = kullanici
        sessionManager.saveUserSession(kullanici)
    }

    /**
     * Oturum durumu kontrolü
     */
    fun isUserLoggedIn(): Boolean {
        return sessionManager.isLoggedIn()
    }

    /**
     * Çıkış Yap (Logout)
     */
    fun logout() {
        currentUserCache = null
        sessionManager.clearSession()
    }

}