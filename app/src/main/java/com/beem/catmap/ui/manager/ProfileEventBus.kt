package com.beem.catmap.ui.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ProfileEventBus {
    private val _profileEvent = MutableSharedFlow<ProfileEvent>(replay = 0, extraBufferCapacity = 10)
    val profileEvent = _profileEvent.asSharedFlow()

    suspend fun emitEvent(event: ProfileEvent) {
        _profileEvent.emit(event)
    }
}