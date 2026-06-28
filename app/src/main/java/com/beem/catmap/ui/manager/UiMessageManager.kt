package com.beem.catmap.ui.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData

object UiMessageManager {
    private val _messageEvent = MutableLiveData<UiMessageState?>()
    val messageEvent: LiveData<UiMessageState?> get() = _messageEvent

    fun emitMessage(state: UiMessageState) {
        _messageEvent.value = state
    }

    fun clear() {
        _messageEvent.value = null
    }
}