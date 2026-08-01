package com.beem.catmap.ui.chat

import com.beem.catmap.mesaj.Mesaj

data class ChatUiState(
    val messages: List<Mesaj> = emptyList(),
    val isLoading: Boolean = false,
    val isBlockedByMe: Boolean = false,
    val isBlockedByOther: Boolean = false,
    val isTyping: Boolean = false,
    val replyMessage: Mesaj? = null,
    val isOtherUserTyping: Boolean = false
)