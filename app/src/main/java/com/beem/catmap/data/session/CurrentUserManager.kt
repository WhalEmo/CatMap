package com.beem.catmap.data.session

import android.content.Context
import com.beem.catmap.KullaniciAuth.Kullanici
import com.google.firebase.auth.FirebaseAuth

class CurrentUserManager private constructor(context: Context) {

    private val sessionManager = UserSessionManager.getInstance(context)

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
        if (FirebaseAuth.getInstance().currentUser == null) {
            clearLocalCache()
            return Kullanici()
        }

        if (currentUserCache == null) {
            currentUserCache = sessionManager.getUserSession()
        }
        return currentUserCache ?: Kullanici()
    }

    fun getCurrentUserId(): String? {
        return getCurrentUser()?.id ?: FirebaseAuth.getInstance().uid
    }

    fun setCurrentUser(kullanici: Kullanici) {
        this.currentUserCache = kullanici
        sessionManager.saveUserSession(kullanici)
    }


    fun isUserLoggedIn(): Boolean {
        return FirebaseAuth.getInstance().currentUser != null && sessionManager.isLoggedIn()
    }

    fun logout() {
        FirebaseAuth.getInstance().signOut()

        clearLocalCache()
    }

    fun clearLocalCache() {
        currentUserCache = null
        sessionManager.clearSession()
    }
}