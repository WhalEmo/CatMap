package com.beem.catmap.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.ChatRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.mesaj.Mesaj
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class ChatViewModel(
    private val repository: ChatRepository = ChatRepository(),
    private val currentUserManager: CurrentUserManager,
    private val receiverId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private var chatId: String? = null
    private var typingTimer: Timer? = null
    private var messagesJob: Job? = null

    val currentUserId: String get() = currentUserManager.getCurrentUserId()

    init {
        initializeChat()
    }

    private fun initializeChat() {
        val senderId = currentUserId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // 1. Sohbet ID'sini Al
            val generatedChatId = repository.getOrCreateChatId(senderId, receiverId)
            chatId = generatedChatId

            // 2. Mesaj Akışını Başlat
            observeMessages(generatedChatId)

            // 3. Yazıyor... Durumunu Dinle
            observeTypingStatus(generatedChatId)

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeMessages(chatId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesFlow(chatId).collectLatest { messageList ->
                _uiState.update { it.copy(messages = messageList) }
            }
        }
    }

    private fun observeTypingStatus(chatId: String) {
        viewModelScope.launch {
            repository.listenTypingStatus(chatId, receiverId).collectLatest { isTyping ->
                _uiState.update { it.copy(isOtherUserTyping = isTyping) }
            }
        }
    }

    fun sendMessage(text: String) {
        val activeChatId = chatId ?: return
        val senderId = currentUserId ?: return
        if (text.trim().isEmpty()) return

        viewModelScope.launch {
            val replyMessage = _uiState.value.replyMessage
            val success = repository.sendMessage(activeChatId, senderId, text, replyMessage)
            if (success) {
                _uiState.update { it.copy(replyMessage = null) }
            }
        }
    }

    fun onTextChanged(text: String) {
        val activeChatId = chatId ?: return
        val senderId = currentUserId ?: return

        if (text.trim().isNotEmpty()) {
            repository.setTypingStatus(activeChatId, senderId, true)
            typingTimer?.cancel()
            typingTimer = Timer().apply {
                schedule(object : TimerTask() {
                    override fun run() {
                        repository.setTypingStatus(activeChatId, senderId, false)
                    }
                }, 1000)
            }
        }
    }

    fun removeBlock() {
        viewModelScope.launch {
            //repository.removeBlock(receiverId)
            _uiState.update { it.copy(isBlockedByMe = false) }
        }
    }

    fun setReplyMessage(mesaj: Mesaj?) {
        _uiState.update { it.copy(replyMessage = mesaj) }
    }

    override fun onCleared() {
        super.onCleared()
        typingTimer?.cancel()
        chatId?.let { id ->
            currentUserId?.let { sender ->
                repository.setTypingStatus(id, sender, false)
            }
        }
    }
}