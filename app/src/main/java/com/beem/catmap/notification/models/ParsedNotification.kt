package com.beem.catmap.notification.models

sealed class ParsedNotification {
    data class Chat(
        val senderId: String,
        val chatId: String,
        val title: String,
        val body: String,
        val photoUrl: String?
    ) : ParsedNotification()

    data class Broadcast(
        val title: String,
        val body: String,
        val targetRoute: String?,
        val imageUrl: String? = null
    ) : ParsedNotification()

    data class Follow(
        val senderId: String,
        val title: String,
        val body: String,
        val photoUrl: String?
    ) : ParsedNotification()
}