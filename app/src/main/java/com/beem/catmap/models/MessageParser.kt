package com.beem.catmap.models

import android.util.Log
import com.google.firebase.database.DataSnapshot

fun DataSnapshot.toChatMessage(): ChatMessage? {
    val id = key ?: return null
    val typeString = child("tur").getValue(String::class.java)
    val type = MessageType.fromString(typeString)

    // JSON verinde hem "gonderen" hem de "gonderici" anahtarları kullanılmış.
    // İki duruma da esnek yaklaşım:
    val senderId = child("gonderen").getValue(String::class.java)
        ?: child("gonderici").getValue(String::class.java)
        ?: return null

    val timestamp = child("zaman").getValue(Long::class.java) ?: 0L
    val isRead = child("goruldu").getValue(Boolean::class.java) ?: false

    return when (type) {
        MessageType.TEXT -> {
            val text = child("mesaj").getValue(String::class.java) ?: ""
            ChatMessage.Text(
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

            Log.d("ChatDebug", "----------------------------------------")
            Log.d("ChatDebug", "📷 Fotoğraf Mesajı Bulundu! ID: $id")
            Log.d("ChatDebug", "🔹 Çekilen URL Listesi: $photoUrls")
            Log.d("ChatDebug", "----------------------------------------")

            ChatMessage.Photo(
                id = id,
                senderId = senderId,
                timestamp = timestamp,
                isRead = isRead,
                photoUrls = photoUrls
            )
        }

        MessageType.REPLY -> {
            val replyText = child("mesaj").getValue(String::class.java) ?: ""
            val parentSnapshot = child("yanitlananMesaj")
            val parentMessage = if (parentSnapshot.exists()) parentSnapshot.toChatMessage() else null

            ChatMessage.Reply(
                id = id,
                senderId = senderId,
                timestamp = timestamp,
                isRead = isRead,
                message = replyText,
                repliedMessage = parentMessage
            )
        }
    }
}