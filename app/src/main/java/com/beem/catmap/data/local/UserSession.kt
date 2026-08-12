package com.beem.catmap.data.local

import com.beem.catmap.CatMapApp
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.session.CurrentUserManager

object UserSession {

    val userModel: UserModel
        get() = CurrentUserManager.getInstance(CatMapApp.instance).getCurrentUser()

    val userId: String
        get() = CurrentUserManager.getInstance(CatMapApp.instance).getCurrentUserId()

    val isLoggedIn: Boolean
        get() = CurrentUserManager.getInstance(CatMapApp.instance).isUserLoggedIn()

    fun update(userModel: UserModel) {
        CurrentUserManager.getInstance(CatMapApp.instance).setCurrentUser(userModel)
    }

    fun logout() {
        CurrentUserManager.getInstance(CatMapApp.instance).logout()
    }
}