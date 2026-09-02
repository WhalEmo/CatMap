package com.beem.catmap.data.model.exception

class UserBlockedByException(
    message: String = "Bu kullanıcıya erişim engellendi.",
    cause: Throwable? = null
) : Exception(message, cause)