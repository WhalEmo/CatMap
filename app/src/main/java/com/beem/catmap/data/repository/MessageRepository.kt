package com.beem.catmap.data.repository

import android.util.Log
import com.beem.catmap.models.ChatMessage
import com.beem.catmap.models.toChatMessage
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MessageRepository(
    private val realDb: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private final val messageRefKey = "mesajlar_v2"
    private val messageRef = realDb.getReference(messageRefKey)

    private val hasMoreOlderMessagesMap = ConcurrentHashMap<String, Boolean>()

    suspend fun getOrCreateChatId(senderId: String, receiverId: String): String = suspendCoroutine { continuation ->
        val id1 = "${receiverId}_$senderId"
        val id2 = "${senderId}_$receiverId"

        messageRef.child(id1).addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    continuation.resume(id1)
                } else {
                    messageRef.child(id2).addListenerForSingleValueEvent(object : ValueEventListener {
                        override fun onDataChange(snap2: DataSnapshot) {
                            if (snap2.exists()) {
                                continuation.resume(id2)
                            } else {
                                messageRef.child(id1).child("yaziyorMu").child(senderId).setValue(false)
                                messageRef.child(id1).child("yaziyorMu").child(receiverId).setValue(false)
                                continuation.resume(id1)
                            }
                        }

                        override fun onCancelled(error: DatabaseError) {
                            continuation.resume(id1)
                        }
                    })
                }
            }

            override fun onCancelled(error: DatabaseError) {
                continuation.resume(id1)
            }
        })
    }

    suspend fun fetchReceiverProfileInfo(receiverId: String): Pair<String, String> {
        return try {
            val document = firestore.collection("users").document(receiverId).get().await()
            if (document.exists()) {
                val name = document.getString("KullaniciAdi") ?: ""
                val photoUrl = document.getString("profilFotoUrl") ?: ""
                Pair(name, photoUrl)
            } else {
                Pair("", "")
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair("", "")
        }
    }

    suspend fun sendPhotoMessage(
        chatId: String,
        senderId: String,
        imageUris: List<android.net.Uri>,
        replyTo: ChatMessage? = null
    ): Boolean {
        return try {
            val storageRef = FirebaseStorage.getInstance().reference.child("mesaj_fotograflari")
            val uploadedUrls = mutableListOf<String>()

            for (uri in imageUris) {
                val fileName = "${System.currentTimeMillis()}_${java.util.UUID.randomUUID()}.jpg"
                val photoRef = storageRef.child(fileName)

                val uploadTask = photoRef.putFile(uri).await()
                val downloadUrl = photoRef.downloadUrl.await().toString()
                uploadedUrls.add(downloadUrl)
            }

            if (uploadedUrls.isEmpty()) return false

            val mesajKey = messageRef.child(chatId).child("anaMesaj").push().key ?: return false

            // 2. Mesaj haritasını oluşturup veritabanına yazıyoruz
            val photoMap = mutableMapOf<String, Any>(
                "gonderen" to senderId,
                "fotoUrlListesi" to uploadedUrls,
                "zaman" to System.currentTimeMillis(),
                "goruldu" to false,
                "tur" to "foto"
            )

            // Yanıtlanan mesaj varsa ekle
            if (replyTo != null) {
                photoMap["yanitlananMesaj"] = mapOf(
                    "mesajID" to replyTo.id,
                    "gonderici" to replyTo.senderId,
                    "mesaj" to when (replyTo) {
                        is ChatMessage.Photo -> "📷 Fotoğraf"
                        is ChatMessage.Text -> replyTo.message
                        is ChatMessage.Reply -> replyTo.message
                        is ChatMessage.Deleted -> replyTo.message
                    }
                )
            }

            messageRef.child(chatId).child("anaMesaj").child(mesajKey).setValue(photoMap).await()
            true
        } catch (e: Exception) {
            Log.e("ChatRepository", "Fotoğraf gönderme hatası: ${e.localizedMessage}", e)
            false
        }
    }

    /**
     * Canlı Mesaj Akışını Flow Olarak Yayınlar
     */
    fun getMessagesFlow(chatId: String, limit: Int = 20): Flow<List<ChatMessage>> = callbackFlow {
        val query = messageRef.child(chatId).child("anaMesaj")
            .orderByChild("zaman")
            .limitToLast(limit)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = snapshot.children.mapNotNull { it.toChatMessage() }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    suspend fun markMessagesAsReadByIds(chatId: String, unreadMessageIds: List<String>) {
        if (unreadMessageIds.isEmpty()) return

        try {
            val updates = mutableMapOf<String, Any>()
            for (id in unreadMessageIds) {
                updates["$id/goruldu"] = true
            }

            messageRef.child(chatId).child("anaMesaj").updateChildren(updates).await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun updateMessage(chatId: String, messageId: String, newText: String): Boolean {
        return try {
            val updates = mapOf<String, Any>(
                "mesaj" to newText,
                "duzenlendi" to true
            )
            messageRef.child(chatId).child("anaMesaj").child(messageId).updateChildren(updates).await()

            // gunMesaj düğümüne de log/takip kaydı atalım (eski veritabanı yapınla uyumlu olması için)
            val gunKey = messageRef.child(chatId).child("gunMesaj").push().key
            if (gunKey != null) {
                val gunMap = mapOf("ID" to messageId, "mesaj" to newText)
                messageRef.child(chatId).child("gunMesaj").child(gunKey).setValue(gunMap).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resetPagination(chatId: String) {
        hasMoreOlderMessagesMap[chatId] = true
    }

    suspend fun getOlderMessages(
        chatId: String,
        lastMessageTimestamp: Long,
        limit: Int = 20
    ): List<ChatMessage> {
        if (hasMoreOlderMessagesMap[chatId] == false) {
            Log.d("ChatRepository", "🛑 [KİLİTLİ] $chatId için daha eski mesaj yok. İstek engellendi.")
            return emptyList()
        }

        return try {
            val targetTimestamp = (lastMessageTimestamp - 1).toDouble()

            val query = messageRef.child(chatId).child("anaMesaj")
                .orderByChild("zaman")
                .endAt(targetTimestamp)
                .limitToLast(limit)

            val snapshot = query.get().await()

            if (!snapshot.exists() || snapshot.childrenCount == 0L) {
                hasMoreOlderMessagesMap[chatId] = false
                Log.d("ChatRepository", "🔒 Veri kalmadı, sayfalama kilitlendi (chatId: $chatId).")
                return emptyList()
            }

            val olderList = snapshot.children.mapNotNull { child ->
                child.toChatMessage()
            }

            if (olderList.size < limit) {
                hasMoreOlderMessagesMap[chatId] = false
                Log.d("ChatRepository", "🔒 Son sayfa yüklendi (Gelen: ${olderList.size} < Limit: $limit). Sayfalama kilitlendi.")
            }

            olderList
        } catch (e: Exception) {
            Log.e("ChatRepository", "getOlderMessages Hata: ${e.localizedMessage}", e)
            emptyList()
        }
    }
    /***
    suspend fun deleteMessage(chatId: String, messageId: String): Boolean {
        return try {
            messageRef.child(chatId).child("anaMesaj").child(messageId).removeValue().await()

            val silKey = messageRef.child(chatId).child("silMesaj").push().key
            if (silKey != null) {
                messageRef.child(chatId).child("silMesaj").child(silKey).setValue(messageId).await()
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    */

    /**
     * Mesaj Gönderme
     */
    suspend fun sendMessage(chatId: String, senderId: String, text: String, replyTo: ChatMessage? = null): Boolean {
        return try {
            val mesajKey = messageRef.child(chatId).child("anaMesaj").push().key ?: return false

            if (replyTo != null) {
                val parentSummary = when (replyTo) {
                    is ChatMessage.Text -> replyTo.message
                    is ChatMessage.Photo -> "📷 Fotoğraf"
                    is ChatMessage.Reply -> replyTo.message
                    is ChatMessage.Deleted -> replyTo.message
                }

                val yanitMap = mapOf(
                    "gonderici" to senderId,
                    "mesaj" to text,
                    "zaman" to System.currentTimeMillis(),
                    "goruldu" to false,
                    "tur" to "yanit",
                    "mesajID" to mesajKey,
                    "yanitlananMesaj" to mapOf(
                        "mesajID" to replyTo.id,
                        "gonderici" to replyTo.senderId,
                        "mesaj" to parentSummary,
                        "zaman" to replyTo.timestamp,
                        "goruldu" to replyTo.isRead,
                        "tur" to when (replyTo) {
                            is ChatMessage.Photo -> "foto"
                            is ChatMessage.Reply -> "yanit"
                            is ChatMessage.Text -> "metin"
                            is ChatMessage.Deleted -> "delete"
                        }
                    )
                )
                messageRef.child(chatId).child("anaMesaj").child(mesajKey).setValue(yanitMap).await()
            } else {
                val map = mapOf(
                    "gonderen" to senderId,
                    "mesaj" to text,
                    "zaman" to System.currentTimeMillis(),
                    "goruldu" to false,
                    "tur" to "metin"
                )
                messageRef.child(chatId).child("anaMesaj").child(mesajKey).setValue(map).await()
            }

            messageRef.child(chatId).child("yaziyorMu").child(senderId).setValue(false).await()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }


    suspend fun deleteMessage(chatId: String, messageId: String): Boolean {
        return try {
            val updates = mapOf<String, Any>(
                "mesaj" to "🚫 Bu mesaj silindi",
                "tur" to "delete"
            )

            messageRef.child(chatId)
                .child("anaMesaj")
                .child(messageId)
                .updateChildren(updates)
                .await()

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    /**
     * Karşı Tarafın "Yazıyor..." Durumunu Flow İle Dinleme
     */
    fun listenTypingStatus(chatId: String, receiverId: String): Flow<Boolean> = callbackFlow {
        val ref = messageRef.child(chatId).child("yaziyorMu").child(receiverId)
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isTyping = snapshot.getValue(Boolean::class.java) ?: false
                trySend(isTyping)
            }

            override fun onCancelled(error: DatabaseError) {}
        }
        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    /**
     * Yazıyor Durumunu Güncelleme
     */
    fun setTypingStatus(chatId: String, senderId: String, isTyping: Boolean) {
        messageRef.child(chatId).child("yaziyorMu").child(senderId).setValue(isTyping)
    }

}