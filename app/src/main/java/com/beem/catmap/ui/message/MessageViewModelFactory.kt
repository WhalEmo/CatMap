package com.beem.catmap.ui.message

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beem.catmap.data.repository.MessageRepository
import com.beem.catmap.data.repository.UserRepository
import com.beem.catmap.data.session.CurrentUserManager

class MessageViewModelFactory(
    private val currentUserManager: CurrentUserManager,
    private val receiverId: String
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MessageViewModel::class.java)) {
            return MessageViewModel(
                repository = MessageRepository(),
                userRepo = UserRepository(),
                currentUserManager = currentUserManager,
                receiverId = receiverId
            ) as T
        }
        throw IllegalArgumentException("Bilinmeyen ViewModel Sınıfı: ${modelClass.name}")
    }
}