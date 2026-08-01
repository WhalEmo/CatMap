package com.beem.catmap.models

sealed class ChatMessage {
    abstract val id: String
    abstract val senderId: String
    abstract val timestamp: Long
    abstract val isRead: Boolean
    abstract val type: MessageType

    data class Text(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val message: String
    ) : ChatMessage() {
        override val type = MessageType.TEXT
    }

    data class Photo(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val photoUrls: List<String>
    ) : ChatMessage() {
        override val type = MessageType.PHOTO
    }

    data class Reply(
        override val id: String,
        override val senderId: String,
        override val timestamp: Long,
        override val isRead: Boolean,
        val message: String,
        val repliedMessage: ChatMessage?
    ) : ChatMessage() {
        override val type = MessageType.REPLY
    }
}