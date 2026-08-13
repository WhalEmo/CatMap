package com.beem.catmap.ui.auth

import com.beem.catmap.data.model.UserModel

sealed class GoogleAuthResult {
    data class ExistingUser(val userModel: UserModel) : GoogleAuthResult()
    data class NewUser(val userModel: UserModel) : GoogleAuthResult()
}