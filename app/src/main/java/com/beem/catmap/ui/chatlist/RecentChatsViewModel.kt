package com.beem.catmap.ui.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.RecentChatsRepository
import com.beem.catmap.data.repository.RecentChatsRepository2
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RecentChatsViewModel(
    private val repository: RecentChatsRepository2 = RecentChatsRepository2()
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecentChatsUiState())
    val uiState: StateFlow<RecentChatsUiState> = _uiState.asStateFlow()

    init {
        observeRecentChats()
    }

    private fun observeRecentChats() {
        val currentUserId = UserSession.userId ?: return

        viewModelScope.launch {
            repository.getRecentChatsFlow(currentUserId).collectLatest { chatList ->
                val enrichedChats = chatList.map { chat ->
                    val (name, photo) = repository.fetchUserInfo(chat.otherUserId)
                    chat.copy(otherUserName = name, otherUserPhotoUrl = photo)
                }

                _uiState.update {
                    it.copy(
                        chats = enrichedChats,
                        isLoading = false
                    )
                }
            }
        }
    }
}