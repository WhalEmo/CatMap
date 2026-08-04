package com.beem.catmap.data.repository

import android.util.Log
import com.beem.catmap.models.RecentChat
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class RecentChatsRepository(
    private val realDb: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private final val messageRefKey = "mesajlar_v2"
    private val messageRef = realDb.getReference(messageRefKey)

    /**
     * Oturum açan kullanıcının dahil olduğu tüm sohbetleri canlı olarak dinler.
     */
    fun getRecentChatsFlow(currentUserId: String): Flow<List<RecentChat>> = callbackFlow {
        Log.d("RecentChatDebug", "🚀 getRecentChatsFlow başlatıldı. Akış dinleniyor... CurrentUserId: $currentUserId")

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("RecentChatDebug", "--------------------------------------------------")
                Log.d("RecentChatDebug", "📥 onDataChange tetiklendi! Toplam Sohbet Düğümü Sayısı: ${snapshot.childrenCount}")

                val chatList = mutableListOf<RecentChat>()

                for (child in snapshot.children) {
                    val chatId = child.key ?: continue
                    val parts = chatId.split("_")

                    if (parts.size < 2) {
                        Log.w("RecentChatDebug", "⚠️ Geçersiz chatId formatı atlandı: $chatId")
                        continue
                    }

                    // Oturum açan kullanıcının chatId içinde olup olmadığını kontrol et
                    val otherUserId = when (currentUserId) {
                        parts[0] -> parts[1]
                        parts[1] -> parts[0]
                        else -> null
                    }

                    if (otherUserId == null) {
                        Log.d("RecentChatDebug", "🙈 Kullanıcı bu sohbete dahil değil, atlandı: $chatId (Oturum: $currentUserId)")
                        continue
                    }

                    Log.d("RecentChatDebug", "✅ Eşleşen Sohbet Bulundu! ChatId: $chatId | Karşı Taraf: $otherUserId")

                    // Son mesajı al
                    val anaMesajSnapshot = child.child("anaMesaj")
                    val lastMessageSnapshot = anaMesajSnapshot.children.lastOrNull()

                    var lastMessageText = ""
                    var lastMessageTime = 0L

                    if (lastMessageSnapshot != null) {
                        val type = lastMessageSnapshot.child("tur").getValue(String::class.java) ?: "metin"
                        lastMessageTime = lastMessageSnapshot.child("zaman").getValue(Long::class.java) ?: 0L

                        lastMessageText = if (type == "foto") {
                            "📷 Fotoğraf"
                        } else {
                            lastMessageSnapshot.child("mesaj").getValue(String::class.java) ?: ""
                        }
                    } else {
                        Log.w("RecentChatDebug", "⚠️ 'anaMesaj' altında hiç mesaj yok: $chatId")
                    }

                    // Okunmamış mesaj sayısını hesapla
                    var unreadCount = 0
                    for (msg in anaMesajSnapshot.children) {
                        val sender = msg.child("gonderen").getValue(String::class.java)
                            ?: msg.child("gonderici").getValue(String::class.java)
                        val isRead = msg.child("goruldu").getValue(Boolean::class.java) ?: false

                        if (sender == otherUserId && !isRead) {
                            unreadCount++
                        }
                    }

                    val recentChat = RecentChat(
                        chatId = chatId,
                        otherUserId = otherUserId,
                        lastMessage = lastMessageText,
                        lastMessageTimestamp = lastMessageTime,
                        unreadCount = unreadCount
                    )

                    chatList.add(recentChat)
                    Log.d("RecentChatDebug", "➕ Listeye Eklendi: $recentChat")
                }

                // Son mesaja göre azalan sırala
                val sortedList = chatList.sortedByDescending { it.lastMessageTimestamp }
                Log.d("RecentChatDebug", "🏁 İşlem Bitti. UI'a Gönderilen Toplam Sohbet Sayısı: ${sortedList.size}")
                Log.d("RecentChatDebug", "--------------------------------------------------")

                trySend(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RecentChatDebug", "❌ Firebase Hatarı (onCancelled): ${error.message}", error.toException())
                close(error.toException())
            }
        }

        messageRef.addValueEventListener(listener)
        awaitClose {
            Log.d("RecentChatDebug", "🛑 Flow kapandı, ValueEventListener kaldırılıyor.")
            messageRef.removeEventListener(listener)
        }
    }
    fun getRecentChatsFlow_2(currentUserId: String): Flow<List<RecentChat>> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<RecentChat>()

                for (child in snapshot.children) {
                    val chatId = child.key ?: continue
                    val parts = chatId.split("_")
                    if (parts.size < 2) continue

                    // Oturum açan kullanıcının chatId içinde olup olmadığını kontrol et
                    val otherUserId = when (currentUserId) {
                        parts[0] -> parts[1]
                        parts[1] -> parts[0]
                        else -> null
                    } ?: continue

                    // Son mesajı al
                    val anaMesajSnapshot = child.child("anaMesaj")
                    val lastMessageSnapshot = anaMesajSnapshot.children.lastOrNull()

                    var lastMessageText = ""
                    var lastMessageTime = 0L

                    if (lastMessageSnapshot != null) {
                        val type = lastMessageSnapshot.child("tur").getValue(String::class.java) ?: "metin"
                        lastMessageTime = lastMessageSnapshot.child("zaman").getValue(Long::class.java) ?: 0L

                        lastMessageText = if (type == "foto") {
                            "📷 Fotoğraf"
                        } else {
                            lastMessageSnapshot.child("mesaj").getValue(String::class.java) ?: ""
                        }
                    }

                    // Okunmamış mesaj sayısını hesapla
                    var unreadCount = 0
                    for (msg in anaMesajSnapshot.children) {
                        val sender = msg.child("gonderen").getValue(String::class.java)
                            ?: msg.child("gonderici").getValue(String::class.java)
                        val isRead = msg.child("goruldu").getValue(Boolean::class.java) ?: false

                        if (sender == otherUserId && !isRead) {
                            unreadCount++
                        }
                    }

                    chatList.add(
                        RecentChat(
                            chatId = chatId,
                            otherUserId = otherUserId,
                            lastMessage = lastMessageText,
                            lastMessageTimestamp = lastMessageTime,
                            unreadCount = unreadCount
                        )
                    )
                }

                // Son mesaja göre azalan sırala (En yeni sohbet en üstte)
                val sortedList = chatList.sortedByDescending { it.lastMessageTimestamp }
                trySend(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        messageRef.addValueEventListener(listener)
        awaitClose { messageRef.removeEventListener(listener) }
    }

    /**
     * Karşı tarafın kullanıcı adını ve profil resmini Firestore'dan çeker.
     */
    suspend fun fetchUserInfo(userId: String): Pair<String, String> {
        return try {
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val name = "${doc.getString("Ad") ?: ""} ${doc.getString("Soyad") ?: ""}".trim()
                val photoUrl = doc.getString("profilFotoUrl") ?: ""
                Pair(name, photoUrl)
            } else {
                Pair("Kullanıcı", "")
            }
        } catch (e: Exception) {
            Pair("Kullanıcı", "")
        }
    }
}