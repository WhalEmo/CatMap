package com.beem.catmap.ui.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object FeedingSpotEventBus {
    private val _events = MutableSharedFlow<FeedingSpotMapEvent>(extraBufferCapacity = 1)
    val events = _events.asSharedFlow()

    suspend fun emitEvent(event: FeedingSpotMapEvent) {
        _events.emit(event)
    }
}