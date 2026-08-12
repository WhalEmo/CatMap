package com.beem.catmap.ui.message

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.MessageRepository
import com.beem.catmap.data.repository.UserRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.data.model.ChatMessage
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.message.models.BlockState
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Timer
import java.util.TimerTask

class MessageViewModel(
    private val repository: MessageRepository = MessageRepository(),
    private val userRepo: UserRepository = UserRepository(),
    private val currentUserManager: CurrentUserManager,
    private val receiverId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MessageUiState())
    val uiState: StateFlow<MessageUiState> = _uiState.asStateFlow()

    private var chatId: String? = null
    private var typingTimer: Timer? = null
    private var messagesJob: Job? = null

    private var presenceJob: Job? = null

    private var isPagingLoading = false

    private var hasMoreOlderMessages = true

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

            val (name, photoUrl) = repository.fetchReceiverProfileInfo(receiverId)
            _uiState.update {
                it.copy(receiverName = name, receiverPhotoUrl = photoUrl)
            }

            // 2. Mesaj Akışını Başlat
            observeMessages(generatedChatId)

            // 3. Yazıyor... Durumunu Dinle
            observeTypingStatus(generatedChatId)

            observeReceiverPresence()

            _uiState.update { it.copy(isLoading = false) }
        }
    }

    private fun observeMessages(chatId: String) {
        messagesJob?.cancel()
        messagesJob = viewModelScope.launch {
            repository.getMessagesFlow(chatId, limit = 20).collectLatest { incomingLatestMessages ->

                _uiState.update { currentState ->
                    val existingList = currentState.messages.toMutableList()

                    if (existingList.isEmpty()) {
                        currentState.copy(messages = incomingLatestMessages)
                    } else {
                        incomingLatestMessages.forEach { msg ->
                            if (msg is ChatMessage.Photo) {
                                val indexById = existingList.indexOfFirst { it.id == msg.id }
                                if (indexById != -1) {
                                    existingList[indexById] = msg
                                } else if (msg.clientTempId != null) {
                                    val pendingIndex = existingList.indexOfFirst { localMsg ->
                                        localMsg is ChatMessage.Photo &&
                                                localMsg.isUploading &&
                                                localMsg.clientTempId == msg.clientTempId
                                    }
                                    if (pendingIndex != -1) {
                                        existingList[pendingIndex] = msg
                                    } else if (existingList.none { it.id == msg.id }) {
                                        existingList.add(msg)
                                    }
                                } else if (existingList.none { it.id == msg.id }) {
                                    existingList.add(msg)
                                }
                            } else {
                                val index = existingList.indexOfFirst { it.id == msg.id }
                                if (index != -1) {
                                    existingList[index] = msg
                                } else {
                                    existingList.add(msg)
                                }
                            }
                        }

                        existingList.sortBy { it.timestamp }

                        currentState.copy(messages = existingList)
                    }
                }
            }
        }
    }

    fun markUnreadMessagesAsRead() {
        val activeChatId = chatId ?: run {
            Log.w("LifecycleDebug", "⚠️ markUnreadMessagesAsRead CANCELLED: chatId null!")
            return
        }

        val currentMessages = _uiState.value.messages

        val unreadIdsFromOther = currentMessages
            .filter { it.senderId != currentUserId && !it.isRead }
            .map { it.id }

        Log.d("LifecycleDebug", "🔍 Okunmamış mesaj taraması yapıldı. Bulunan Okunmamış ID'ler: $unreadIdsFromOther")

        if (unreadIdsFromOther.isNotEmpty()) {
            viewModelScope.launch {
                Log.d("LifecycleDebug", "🔥 VERİTABANINDA OKUNDU YAPILIYOR! ID'ler: $unreadIdsFromOther")
                repository.markMessagesAsReadByIds(activeChatId, unreadIdsFromOther)
            }
        }
    }

    fun unblockUserMock() {
        viewModelScope.launch {
            val newBlockState = when(_uiState.value.blockState) {
                BlockState.BlockedByMe -> BlockState.None
                BlockState.BlockedByUser -> BlockState.BlockedByUser
                BlockState.MutualBlock -> BlockState.BlockedByUser
                BlockState.None -> BlockState.None
            }
            _uiState.update { currentState ->
                currentState.copy(
                    blockState = newBlockState
                )
            }
            UiMessageManager.emitMessage(UiMessageState.Success("Kullanıcının engeli kaldırıldı."))
        }
    }

    fun blockUserMock() {
        viewModelScope.launch {
            val newBlockState = when(_uiState.value.blockState) {
                BlockState.BlockedByMe -> BlockState.BlockedByMe
                BlockState.BlockedByUser -> BlockState.MutualBlock
                BlockState.MutualBlock -> BlockState.MutualBlock
                BlockState.None -> BlockState.BlockedByMe
            }
            _uiState.update { currentState ->
                currentState.copy(
                    blockState = newBlockState
                )
            }
            UiMessageManager.emitMessage(UiMessageState.Info("Kullanıcı engellendi."))
        }
    }

    fun testBlockState(state: BlockState) {
        _uiState.update { currentState ->
            currentState.copy(
                blockState = state
            )
        }
    }

    fun sendPhotos(uris: List<android.net.Uri>) {
        val activeChatId = chatId ?: return
        val senderId = currentUserId ?: return
        if (uris.isEmpty()) return

        viewModelScope.launch {
            val tempId = java.util.UUID.randomUUID().toString()

            val tempPhotoUrls = uris.map { it.toString() }

            val tempPhotoMessage = ChatMessage.Photo(
                id = tempId,
                senderId = senderId,
                timestamp = System.currentTimeMillis(),
                isRead = false,
                photoUrls = tempPhotoUrls,
                isUploading = true,
                clientTempId = tempId
            )

            _uiState.update { currentState ->
                currentState.copy(
                    messages = currentState.messages + tempPhotoMessage,
                    replyMessage = null
                )
            }

            val replyMessage = _uiState.value.replyMessage
            val success = repository.sendPhotoMessage(
                chatId = activeChatId,
                senderId = senderId,
                imageUris = uris,
                replyTo = replyMessage,
                clientTempId = tempId
            )

            if (!success) {
                _uiState.update { currentState ->
                    currentState.copy(
                        messages = currentState.messages.filter { it.id != tempId }
                    )
                }
            }
        }
    }

    fun updateMessage(messageId: String, newText: String) {
        val activeChatId = chatId ?: return
        if (newText.trim().isEmpty()) return

        viewModelScope.launch {
            repository.updateMessage(activeChatId, messageId, newText)
        }
    }

    fun deleteMessage(messageId: String) {
        val activeChatId = chatId ?: return

        viewModelScope.launch {
            val deleted = repository.deleteMessage(activeChatId, messageId)
        }
    }

    private fun observeReceiverPresence() {
        presenceJob?.cancel()
        presenceJob = viewModelScope.launch {
            userRepo.getUserPresenceFlow(receiverId).collectLatest { status ->
                _uiState.update { it.copy(receiverStatus = status) }
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


    fun loadOlderMessages() {
        val activeChatId = chatId ?: return
        val currentMessages = _uiState.value.messages

        if (isPagingLoading || !hasMoreOlderMessages || currentMessages.isEmpty()) {
            return
        }

        val oldestMessageTimestamp = currentMessages.first().timestamp

        viewModelScope.launch {
            isPagingLoading = true

            val olderMessages = repository.getOlderMessages(
                chatId = activeChatId,
                lastMessageTimestamp = oldestMessageTimestamp,
                limit = 20
            )
            if (olderMessages.isNotEmpty()) {
                _uiState.update { state ->
                    state.copy(messages = olderMessages + state.messages)
                }

                if (olderMessages.size < 20) {
                    hasMoreOlderMessages = false
                }
            } else {
                hasMoreOlderMessages = false
            }

            isPagingLoading = false
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

    fun setReplyMessage(message: ChatMessage?) {
        _uiState.update { it.copy(replyMessage = message) }
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