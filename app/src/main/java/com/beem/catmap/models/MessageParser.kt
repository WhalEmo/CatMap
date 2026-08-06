package com.beem.catmap.models

import android.util.Log
import com.beem.catmap.models.ChatMessage.*
import com.google.firebase.database.DataSnapshot

fun DataSnapshot.toChatMessage(): ChatMessage? {
    val id = key ?: return null
    val typeString = child("tur").getValue(String::class.java)
    val type = MessageType.fromString(typeString)

    val senderId = child("gonderen").getValue(String::class.java)
        ?: child("gonderici").getValue(String::class.java)
        ?: return null

    val timestamp = child("zaman").getValue(Long::class.java) ?: 0L
    val isRead = child("goruldu").getValue(Boolean::class.java) ?: false

    return when (type) {
        MessageType.TEXT -> {
            val text = child("mesaj").getValue(String::class.java) ?: ""
            Text(
                id = id,
                senderId = senderId,
                timestamp = timestamp,
                isRead = isRead,
                message = text
            )
        }

        MessageType.PHOTO -> {
            val photoUrls = child("fotoUrlListesi").children.mapNotNull {
                it.getValue(String::class.java)
            }
            val clientTempId = child("clientTempId").getValue(String::class.java)

            Log.d("ChatDebug", "========================================")
            Log.d("ChatDebug", "📷 [MAPPER] Fotoğraf Mesajı Yakalandı!")
            Log.d("ChatDebug", "🔹 Realtime DB ID (Firebase Key): $id")
            Log.d("ChatDebug", "🔹 clientTempId: $clientTempId") // 👈 BURASI KRİTİK (null mı geliyor?)
            Log.d("ChatDebug", "🔹 Çekilen URL Listesi ($photoUrls.size adet): $photoUrls")
            Log.d("ChatDebug", "========================================")

            Photo(
                id = id,
                senderId = senderId,
                timestamp = timestamp,
                isRead = isRead,
                photoUrls = photoUrls,
                isUploading = false,
                clientTempId = clientTempId
            )
        }

        MessageType.REPLY -> {
            val replyText = child("mesaj").getValue(String::class.java) ?: ""
            val parentSnapshot = child("yanitlananMesaj")
            val parentMessage = if (parentSnapshot.exists()) parentSnapshot.toChatMessage() else null

            Reply(
                id = id,
                senderId = senderId,
                timestamp = timestamp,
                isRead = isRead,
                message = replyText,
                repliedMessage = parentMessage
            )
        }

        MessageType.DELETE -> {
            val deletedText = child("mesaj").getValue(String::class.java) ?: "🚫 Bu mesaj silindi"
            Deleted(
                id = id,
                senderId = senderId,
                timestamp = timestamp,
                isRead = isRead,
                message = deletedText
            )
        }
    }
}