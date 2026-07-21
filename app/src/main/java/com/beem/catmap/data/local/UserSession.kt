package com.beem.catmap.data.local

import com.beem.catmap.CatMapApp
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.repository.UserRepository

object UserSession {

    val user: Kullanici
        get() = UserRepository.getInstance(CatMapApp.instance).getCurrentUser()

    val userId: String
        get() = UserRepository.getInstance(CatMapApp.instance).getCurrentUserId()

    val isLoggedIn: Boolean
        get() = UserRepository.getInstance(CatMapApp.instance).isUserLoggedIn()

    fun update(user: Kullanici) {
        UserRepository.getInstance(CatMapApp.instance).setCurrentUser(user)
    }

    fun logout() {
        UserRepository.getInstance(CatMapApp.instance).logout()
    }
}