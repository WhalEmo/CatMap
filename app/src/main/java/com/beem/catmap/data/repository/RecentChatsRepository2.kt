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
        if (currentUserId.isBlank()) {
            trySend(emptyList())
            return@callbackFlow
        }
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
                // 🟢 Çıkış yapıldığında veya yetki gittiğinde uygulamayı patlatmak yerine akışı güvenle kapatıyoruz
                if (error.code == DatabaseError.PERMISSION_DENIED) {
                    Log.d("RecentChats", "Oturum kapandı veya yetki bitti, dinleyici güvenle sonlandırılıyor.")

                    // Kanala boş liste gönderip akışı çökme olmadan kapatabilirsin
                    trySend(emptyList())
                    close()
                } else {
                    // Diğer beklenmeyen gerçek hataları logla
                    Log.e("RecentChats", "Realtime DB Hatası: ${error.message}")
                }
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
        if (userId.isBlank()) return Pair("CatMap Kullanıcısı", "")

        return try {
            // 1. KADEME: 'users' ana koleksiyonundan çekmeyi dene
            val doc = firestore.collection("users").document(userId).get().await()
            if (doc.exists()) {
                val name = "${doc.getString("Ad") ?: ""} ${doc.getString("Soyad") ?: ""}".trim()
                val photoUrl = doc.getString("profilFotoUrl") ?: ""

                // İsim boşsa fallback'e gitmesin diye kontrol
                val finalName = if (name.isNotBlank()) name else "CatMap Kullanıcısı"
                Pair(finalName, photoUrl)
            } else {
                // Document yoksa 2. Kademe (publicUsers) dene
                fetchFromPublicUsers(userId)
            }
        } catch (e: Exception) {
            // 2. KADEME: Permission Denied veya herhangi bir Firestore hatasında publicUsers'a düş
            Log.w("RecentChatsRepo", "⚠️ 'users' koleksiyonuna erişilemedi (${e.localizedMessage}). 'publicUsers' deneniyor... UserId: $userId")
            fetchFromPublicUsers(userId)
        }
    }

    /**
     * 2. Kademe: Engellenme veya 'users' içinde bulunamama durumunda çalışan yedek fonksiyon
     */
    private suspend fun fetchFromPublicUsers(userId: String): Pair<String, String> {
        return try {
            val publicDoc = firestore.collection("publicUsers").document(userId).get().await()
            if (publicDoc.exists()) {
                // Farklı key isimleri ihtimaline karşı fallback alanları
                val name = publicDoc.getString("KullaniciAdi")
                    ?: publicDoc.getString("kullaniciAdi")
                    ?: publicDoc.getString("Ad")
                    ?: "CatMap Kullanıcısı"

                val photoUrl = publicDoc.getString("profilFotoUrl") ?: ""
                Pair(name, photoUrl)
            } else {
                // 3. KADEME: publicUsers içinde de yoksa varsayılan
                Pair("CatMap Kullanıcısı", "")
            }
        } catch (e: Exception) {
            // 3. KADEME: publicUsers çekerken de hata alındıysa son çare varsayılan
            Log.e("RecentChatsRepo", "❌ 'publicUsers' çekilirken hata oluştu: ${e.localizedMessage}")
            Pair("CatMap Kullanıcısı", "")
        }
    }
}