package com.beem.catmap.engine.speedengine

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

object SpeedEngine {

    private const val START_MOVING_THRESHOLD = 1.2f
    private const val STOP_MOVING_THRESHOLD = 0.6f

    private val _motionState = MutableStateFlow(MotionState.STATIC)
    val motionState: StateFlow<MotionState> get() = _motionState

    private var lastCalculatedSpeed = 0f

    suspend fun processLocation(newLocation: Location) = withContext(Dispatchers.Default) {
        val rawSpeed = if (newLocation.hasSpeed()) {
            newLocation.speed
        } else {
            0f
        }

        lastCalculatedSpeed = lastCalculatedSpeed * 0.7f + rawSpeed * 0.3f

        val currentState = _motionState.value
        if (currentState == MotionState.STATIC && lastCalculatedSpeed > START_MOVING_THRESHOLD) {
            _motionState.value = MotionState.MOVING
        } else if (currentState == MotionState.MOVING && lastCalculatedSpeed < STOP_MOVING_THRESHOLD) {
            _motionState.value = MotionState.STATIC
        }
    }
}