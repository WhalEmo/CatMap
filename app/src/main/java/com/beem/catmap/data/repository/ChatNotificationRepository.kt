package com.beem.catmap.data.repository

import android.util.Log
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class ChatNotificationRepository {

    fun getHasUnreadChatsFlow(userId: String): Flow<Boolean> = callbackFlow {
        val ref = FirebaseDatabase.getInstance().getReference("recent_chats").child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var hasUnread = false
                for (child in snapshot.children) {
                    val unreadCount = child.child("unreadCount").getValue(Int::class.java) ?: 0
                    if (unreadCount > 0) {
                        hasUnread = true
                        break
                    }
                }
                trySend(hasUnread)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.w("ChatNotificationRepo", "DB Listener iptal edildi veya yetki yok: ${error.message}")
                trySend(false)
                close() // Çökmeyi engellemek için exception fırlatmadan güvenle kapat
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }
}