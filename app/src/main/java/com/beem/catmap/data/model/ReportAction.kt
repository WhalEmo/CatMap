package com.beem.catmap.data.model

enum class ReportAction {
    ACTION_ADDED_BOTH,
    ACTION_ADDED_FOOD,
    ACTION_ADDED_WATER,

    OBSERVED_FOOD_FULL,
    OBSERVED_FOOD_EMPTY,
    OBSERVED_WATER_FULL,
    OBSERVED_WATER_EMPTY,

    ACTION_CLEANED,
    ACTION_REPAIRED,
    OBSERVED_DIRTY,
    OBSERVED_DAMAGED,
    UNKNOWN;

    companion object {
        fun safeValueOf(value: String): ReportAction {
            return try {
                valueOf(value)
            } catch (e: IllegalArgumentException) {
                UNKNOWN
            }
        }
    }
}