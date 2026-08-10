package com.beem.catmap.ui.auth

import com.beem.catmap.KullaniciAuth.Kullanici

sealed class GoogleAuthResult {
    data class ExistingUser(val user: Kullanici) : GoogleAuthResult()
    data class NewUser(val user: Kullanici) : GoogleAuthResult()
}