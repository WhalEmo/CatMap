package com.beem.catmap.ui.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object ProfileEventBus {
    private val _profileEvent = MutableSharedFlow<ProfileEvent>(
        replay = 0,
        extraBufferCapacity = 64
    )
    val profileEvent = _profileEvent.asSharedFlow()

    fun emitEvent(event: ProfileEvent) {
        _profileEvent.tryEmit(event)
    }
}