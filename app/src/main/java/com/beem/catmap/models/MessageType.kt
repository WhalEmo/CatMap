package com.beem.catmap.models

import com.google.firebase.database.PropertyName

enum class MessageType(val value: String) {
    @PropertyName("metin")
    TEXT("metin"),

    @PropertyName("foto")
    PHOTO("foto"),

    @PropertyName("yanit")
    REPLY("yanit"),

    @PropertyName("delete")
    DELETE("delete");

    companion object {
        fun fromString(type: String?): MessageType {
            return entries.find { it.value.equals(type, ignoreCase = true) } ?: TEXT
        }
    }
}