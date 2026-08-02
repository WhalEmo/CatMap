package com.beem.catmap.ui.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object CatEventBus {
    private val _catMapEvent = MutableSharedFlow<CatMapEvent>(replay = 0, extraBufferCapacity = 64)
    val catMapEvent = _catMapEvent.asSharedFlow()

    suspend fun emitEvent(event: CatMapEvent) {
        _catMapEvent.emit(event)
    }
}