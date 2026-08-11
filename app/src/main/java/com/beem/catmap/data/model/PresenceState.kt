package com.beem.catmap.data.model

sealed interface PresenceState {
    data class Success(val isOnline: Boolean, val lastSeenText: String) : PresenceState
    object Blocked : PresenceState
    object Offline : PresenceState
    data class Error(val exception: Throwable) : PresenceState
}