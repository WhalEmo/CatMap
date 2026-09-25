package com.beem.catmap.engine.location

enum class LocationAccessState {
    READY,
    GPS_DISABLED,
    PERMISSION_RATIONALE,
    PERMISSION_PERMANENT,
    CHECKING
}