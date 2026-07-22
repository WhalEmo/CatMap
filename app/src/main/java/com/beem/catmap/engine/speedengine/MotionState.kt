package com.beem.catmap.engine.speedengine

import android.location.Location
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext

enum class MotionState(val tilt: Float, val zoom: Float) {
    STATIC(0f, 17.5f),
    MOVING(45f, 18.5f)
}