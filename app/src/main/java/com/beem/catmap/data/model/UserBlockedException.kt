package com.beem.catmap.data.model

import com.beem.catmap.KullaniciAuth.Kullanici

class UserBlockedException(
    message: String = "Engellediğiniz kullanıcı",
    val profile: Kullanici? = null
) : Exception(message)