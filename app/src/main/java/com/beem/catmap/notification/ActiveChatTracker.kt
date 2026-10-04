package com.beem.catmap.notification

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase

object ActiveChatTracker {

    private const val TAG = "ActiveChatTracker"

    @Volatile
    var activeChatRoomId: String? = null

    // Uygulama ön planda mı?
    @Volatile
    var isAppInForeground: Boolean = false

    private val auth get() = FirebaseAuth.getInstance()
    private val database get() = FirebaseDatabase.getInstance()

    private fun getCurrentChatRef(): DatabaseReference? {
        val uid = auth.currentUser?.uid ?: return null
        return database.getReference("userSessionPrivate")
            .child(uid)
            .child("currentChat")
    }

    fun enterChat(chatId: String) {
        activeChatRoomId = chatId
        Log.d(TAG, "🟢 Sohbete girildi: $chatId")

        getCurrentChatRef()?.let { ref ->
            ref.setValue(chatId)
            ref.onDisconnect().removeValue()
        }
    }

    fun exitChat(chatId: String) {
        if (activeChatRoomId == chatId) {
            activeChatRoomId = null
            Log.d(TAG, "🔴 Sohbetten çıkıldı: $chatId")

            getCurrentChatRef()?.let { ref ->
                ref.removeValue()
                ref.onDisconnect().cancel()
            }
        }
    }

    fun isChatActive(incomingRoomId: String?): Boolean {
        if (incomingRoomId.isNullOrEmpty()) return false
        return isAppInForeground && (activeChatRoomId == incomingRoomId)
    }
}