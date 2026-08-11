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

class RecentChatsRepository2(
    private val realDb: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    /**
     * SADECE kullanıcının kendi son sohbetler listesini canlı dinler.
     * Güvenlik açığı oluşmaz ve Permission Denied hatası vermez!
     */
    fun getRecentChatsFlow(currentUserId: String): Flow<List<RecentChat>> = callbackFlow {
        Log.d("RecentChatDebug", "🚀 getRecentChatsFlow başlatıldı. UserId: $currentUserId")

        val userRecentRef = realDb.getReference("recent_chats").child(currentUserId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val chatList = mutableListOf<RecentChat>()

                for (child in snapshot.children) {
                    val chatId = child.child("chatId").getValue(String::class.java) ?: continue
                    val otherUserId = child.child("otherUserId").getValue(String::class.java) ?: continue
                    val lastMessage = child.child("lastMessage").getValue(String::class.java) ?: ""
                    val lastMessageTimestamp = child.child("lastMessageTimestamp").getValue(Long::class.java) ?: 0L
                    val unreadCount = child.child("unreadCount").getValue(Int::class.java) ?: 0

                    val recentChat = RecentChat(
                        chatId = chatId,
                        otherUserId = otherUserId,
                        lastMessage = lastMessage,
                        lastMessageTimestamp = lastMessageTimestamp,
                        unreadCount = unreadCount
                    )
                    chatList.add(recentChat)
                }

                val sortedList = chatList.sortedByDescending { it.lastMessageTimestamp }
                trySend(sortedList)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("RecentChatDebug", "❌ Firebase Hatası (onCancelled): ${error.message}", error.toException())
                close(error.toException())
            }
        }

        userRecentRef.addValueEventListener(listener)
        awaitClose {
            userRecentRef.removeEventListener(listener)
        }
    }

    /**
     * Kullanıcı sohbeti açtığında okunmamış mesaj sayısını sıfırlar
     */
    suspend fun clearUnreadCount(currentUserId: String, otherUserId: String) {
        try {
            realDb.getReference("recent_chats")
                .child(currentUserId)
                .child(otherUserId)
                .child("unreadCount")
                .setValue(0)
                .await()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

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