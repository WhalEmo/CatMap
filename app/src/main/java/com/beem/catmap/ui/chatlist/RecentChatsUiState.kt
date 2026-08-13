package com.beem.catmap.ui.chatlist

import com.beem.catmap.data.model.RecentChat

data class RecentChatsUiState(
    val chats: List<RecentChat> = emptyList(),
    val isLoading: Boolean = true
){
    val isEmpty: Boolean get() = !isLoading && chats.isEmpty()
}