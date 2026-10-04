package com.beem.catmap.notification.models

enum class NotificationType(val rawValue: String) {
    CHAT_MESSAGE("CHAT_MESSAGE"),
    ADMIN_BROADCAST("ADMIN_BROADCAST"),
    REMINDER("REMINDER"),
    FOLLOW("FOLLOW");

    companion object {
        fun fromRaw(value: String?): NotificationType {
            return entries.firstOrNull { it.rawValue.equals(value, ignoreCase = true) }
                ?: ADMIN_BROADCAST
        }
    }
}