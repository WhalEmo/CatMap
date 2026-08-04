package com.beem.catmap.ui.message

import com.beem.catmap.models.ChatMessage

data class MessageUiState(
    val messages: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isBlockedByOther: Boolean = false,
    val isTyping: Boolean = false,
    val replyMessage: ChatMessage? = null,
    val isOtherUserTyping: Boolean = false,

    val receiverName: String = "",
    val receiverPhotoUrl: String = "",
    val receiverStatus: String = "Çevrimdışı"
)