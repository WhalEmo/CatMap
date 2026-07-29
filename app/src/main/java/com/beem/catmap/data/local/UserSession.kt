package com.beem.catmap.data.local

import com.beem.catmap.CatMapApp
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.session.CurrentUserManager

object UserSession {

    val user: Kullanici
        get() = CurrentUserManager.getInstance(CatMapApp.instance).getCurrentUser()

    val userId: String
        get() = CurrentUserManager.getInstance(CatMapApp.instance).getCurrentUserId()

    val isLoggedIn: Boolean
        get() = CurrentUserManager.getInstance(CatMapApp.instance).isUserLoggedIn()

    fun update(user: Kullanici) {
        CurrentUserManager.getInstance(CatMapApp.instance).setCurrentUser(user)
    }

    fun logout() {
        CurrentUserManager.getInstance(CatMapApp.instance).logout()
    }
}