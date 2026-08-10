package com.beem.catmap.ui.auth.exceptions

sealed class AuthError : Exception() {
    class NetworkError : AuthError()
    class InvalidCredential : AuthError()
    class UserDisabled : AuthError()
    class Unknown : AuthError()
}