package com.beem.catmap.ui.auth.exceptions

sealed class GoogleAuthException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause) {

    class UnsupportedCredential(
        val credentialType: String
    ) : GoogleAuthException(
        "Desteklenmeyen credential tipi: $credentialType"
    )

    class InvalidGoogleCredential(
        cause: Throwable
    ) : GoogleAuthException(
        "Google credential parse edilemedi.",
        cause
    )

    class EmptyIdToken(): GoogleAuthException("Google ID token boş döndü.")
}