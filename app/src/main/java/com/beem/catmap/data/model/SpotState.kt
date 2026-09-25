package com.beem.catmap.data.model

enum class SpotState {
    FULL,
    NEEDS_FOOD,
    NEEDS_WATER,
    NEEDS_BOTH,
    NEEDS_MAINTENANCE,
    OUT_OF_SERVICE,
    UNKNOWN;

    companion object {
        fun safeValueOf(value: String): SpotState {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}