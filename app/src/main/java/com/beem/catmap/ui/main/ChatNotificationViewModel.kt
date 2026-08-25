package com.beem.catmap.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.ChatNotificationRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatNotificationViewModel(
    private val repository: ChatNotificationRepository = ChatNotificationRepository()
) : ViewModel() {

    private val _hasUnreadMessages = MutableStateFlow(false)
    val hasUnreadMessages: StateFlow<Boolean> = _hasUnreadMessages.asStateFlow()

    private var observeJob: Job? = null
    private val authListener = FirebaseAuth.AuthStateListener { auth ->
        val user = auth.currentUser
        if (user != null) {
            startObserving(user.uid)
        } else {
            resetAndStop()
        }
    }

    init {
        FirebaseAuth.getInstance().addAuthStateListener(authListener)
    }

    private fun startObserving(userId: String) {
        observeJob?.cancel()
        _hasUnreadMessages.value = false

        observeJob = viewModelScope.launch {
            repository.getHasUnreadChatsFlow(userId).collectLatest { hasUnread ->
                _hasUnreadMessages.value = hasUnread
            }
        }
    }

    private fun resetAndStop() {
        observeJob?.cancel()
        _hasUnreadMessages.value = false
    }

    override fun onCleared() {
        super.onCleared()
        FirebaseAuth.getInstance().removeAuthStateListener(authListener)
    }

    fun observeFromJava(owner: androidx.lifecycle.LifecycleOwner, onUnreadChanged: (Boolean) -> Unit) {
        owner.lifecycleScope.launch {
            owner.repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                hasUnreadMessages.collect {
                    onUnreadChanged(it)
                }
            }
        }
    }
}