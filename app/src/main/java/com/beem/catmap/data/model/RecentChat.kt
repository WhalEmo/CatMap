package com.beem.catmap.data.model


data class RecentChat(
    val chatId: String,
    val otherUserId: String,
    val otherUserName: String = "",
    val otherUserPhotoUrl: String = "",
    val lastMessage: String = "",
    val lastMessageTimestamp: Long = 0L,
    val unreadCount: Int = 0,
    val isBlocked: Boolean = false
)