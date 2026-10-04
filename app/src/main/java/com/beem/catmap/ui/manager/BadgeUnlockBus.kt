package com.beem.catmap.ui.manager

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object BadgeUnlockBus {
    private val _celebrationEvents = MutableSharedFlow<BadgeCelebrationPayload?>(extraBufferCapacity = 1)
    val celebrationEvents: SharedFlow<BadgeCelebrationPayload?> = _celebrationEvents.asSharedFlow()

    var isCelebrationActive: Boolean = false
        private set

    private var onDismissCallback: (() -> Unit)? = null

    fun emitCelebration(payload: BadgeCelebrationPayload) {
        isCelebrationActive = true
        _celebrationEvents.tryEmit(payload)
    }

    fun dismiss() {
        isCelebrationActive = false
        _celebrationEvents.tryEmit(null)
        onDismissCallback?.invoke()
        onDismissCallback = null
    }

    fun setOnDismissAction(action: () -> Unit) {
        if (!isCelebrationActive) {
            action()
        } else {
            onDismissCallback = action
        }
    }
}