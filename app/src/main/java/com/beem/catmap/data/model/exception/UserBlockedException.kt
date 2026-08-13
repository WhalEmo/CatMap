package com.beem.catmap.data.model.exception

import com.beem.catmap.data.model.UserModel

class UserBlockedException(
    message: String = "Engellediğiniz kullanıcı",
    val profile: UserModel? = null
) : Exception(message)