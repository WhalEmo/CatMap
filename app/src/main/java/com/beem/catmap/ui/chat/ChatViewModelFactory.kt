package com.beem.catmap.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beem.catmap.data.repository.ChatRepository
import com.beem.catmap.data.repository.UserRepository
import com.beem.catmap.data.session.CurrentUserManager

class ChatViewModelFactory(
    private val currentUserManager: CurrentUserManager,
    private val receiverId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChatViewModel::class.java)) {
            return ChatViewModel(
                repository = ChatRepository(),
                userRepo = UserRepository(),
                currentUserManager = currentUserManager,
                receiverId = receiverId
            ) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel Sınıfı: ${modelClass.name}")
    }
}