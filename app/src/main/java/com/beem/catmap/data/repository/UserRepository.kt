package com.beem.catmap.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class UserRepository(
    private val realDb: FirebaseDatabase = FirebaseDatabase.getInstance()
) {
    private val durumlarRef = realDb.getReference("durumlar")

    fun getUserPresenceFlow(userId: String): Flow<String> = callbackFlow {
        val userStatusRef = durumlarRef.child(userId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("cevrimici").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("sonGorulme").getValue(Long::class.java) ?: 0L

                val statusText = if (isOnline) {
                    "Çevrimiçi"
                } else if (lastSeen > 0) {
                    "Son görülme: ${formatLastSeen(lastSeen)}"
                } else {
                    "Çevrimdışı"
                }

                trySend(statusText)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        userStatusRef.addValueEventListener(listener)
        awaitClose { userStatusRef.removeEventListener(listener) }
    }

    private fun formatLastSeen(timestamp: Long): String {
        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}