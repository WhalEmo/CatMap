package com.beem.catmap.data.repository

import com.beem.catmap.mesaj.Mesaj
import com.beem.catmap.mesaj.YanitMesaj
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class ChatRepository(
    private val realDb: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private final val messageRefKey = "mesajlar_v2"
    private val messageRef = realDb.getReference(messageRefKey)


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

    /**
     * Canlı Mesaj Akışını Flow Olarak Yayınlar
     */
    fun getMessagesFlow(chatId: String, limit: Int = 20): Flow<List<Mesaj>> = callbackFlow {
        val query = messageRef.child(chatId).child("anaMesaj")
            .orderByChild("zaman")
            .limitToLast(limit)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<Mesaj>()
                for (snap in snapshot.children) {
                    val mesaj = parseMesaj(snap)
                    if (mesaj != null) list.add(mesaj)
                }
                trySend(list)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        query.addValueEventListener(listener)
        awaitClose { query.removeEventListener(listener) }
    }

    /**
     * Mesaj Gönderme
     */
    suspend fun sendMessage(chatId: String, senderId: String, text: String, replyTo: Mesaj? = null): Boolean {
        return try {
            val mesajKey = messageRef.child(chatId).child("anaMesaj").push().key ?: return false

            if (replyTo != null) {
                val yanit = YanitMesaj(senderId, text, System.currentTimeMillis(), mesajKey, false, replyTo)
                if (replyTo.tur == "foto") replyTo.mesaj = "📷 Fotoğraf"
                messageRef.child(chatId).child("anaMesaj").child(mesajKey).setValue(yanit).await()
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

            // Yazıyor durumunu kapat
            messageRef.child(chatId).child("yaziyorMu").child(senderId).setValue(false).await()
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

    private fun parseMesaj(snapshot: DataSnapshot): Mesaj? {
        val tur = snapshot.child("tur").getValue(String::class.java) ?: "metin"
        return when (tur) {
            "metin" -> {
                val id = snapshot.key ?: ""
                val gonderen = snapshot.child("gonderen").getValue(String::class.java) ?: ""
                val zaman = snapshot.child("zaman").getValue(Long::class.java) ?: 0L
                val mesaj = snapshot.child("mesaj").getValue(String::class.java) ?: ""
                val goruldu = snapshot.child("goruldu").getValue(Boolean::class.java) ?: false
                Mesaj(gonderen, mesaj, zaman, id, goruldu).apply {
                    this.tur = "metin"
                }
            }
            "yanit" -> snapshot.getValue(YanitMesaj::class.java)
            else -> null
        }
    }
}