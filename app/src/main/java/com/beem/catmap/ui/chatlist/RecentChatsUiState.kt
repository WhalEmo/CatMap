package com.beem.catmap.ui.chatlist

import com.beem.catmap.models.RecentChat

data class RecentChatsUiState(
    val chats: List<RecentChat> = emptyList(),
    val isLoading: Boolean = true
)