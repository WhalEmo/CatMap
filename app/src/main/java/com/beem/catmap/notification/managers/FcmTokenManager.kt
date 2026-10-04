package com.beem.catmap.notification.managers

import android.os.Build
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await

class FcmTokenManager(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val database: FirebaseDatabase = FirebaseDatabase.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val messaging: FirebaseMessaging = FirebaseMessaging.getInstance()
) {

    companion object {
        private const val TAG = "FcmTokenManager"
    }

    // Uygulama açılışında veya login sonrasında çağrılır
    suspend fun syncCurrentToken() {
        val currentUser = auth.currentUser ?: return
        try {
            val token = messaging.token.await()
            saveTokenParallel(currentUser.uid, token)
        } catch (e: Exception) {
            Log.e(TAG, "❌ syncCurrentToken başarısız oldu: ${e.message}", e)
        }
    }

    // onNewToken tetiklendiğinde çağrılır
    suspend fun onNewTokenReceived(newToken: String) {
        val currentUser = auth.currentUser ?: return
        try {
            saveTokenParallel(currentUser.uid, newToken)
        } catch (e: Exception) {
            Log.e(TAG, "❌ onNewTokenReceived başarısız oldu: ${e.message}", e)
        }
    }

    private suspend fun saveTokenParallel(userId: String, token: String) = coroutineScope {
        val firestoreDeferred = async {
            try {
                saveTokenToFirestore(userId, token)
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Firestore token kaydı hatası: ${e.message}")
            }
        }
        val realTimeDeferred = async {
            try {
                saveTokenToRealtimeDB(userId, token)
            } catch (e: Exception) {
                Log.e(TAG, "⚠️ Realtime Database token kaydı hatası: ${e.message}")
            }
        }

        firestoreDeferred.await()
        realTimeDeferred.await()
    }

    private suspend fun saveTokenToRealtimeDB(userId: String, token: String) {
        val safeTokenKey = sanitizeToken(token)

        val realTimeDBData = hashMapOf(
            "token" to token,
            "deviceModel" to Build.MODEL,
            "updatedAt" to ServerValue.TIMESTAMP,
            "platform" to "android"
        )

        database.getReference("userSessionPrivate")
            .child(userId)
            .child("fcmTokens")
            .child(safeTokenKey)
            .setValue(realTimeDBData)
            .await()

        Log.d(TAG, "✅ Token Realtime Database'e yazıldı.")
    }

    private suspend fun saveTokenToFirestore(userId: String, token: String) {
        val firestoreData = hashMapOf(
            "token" to token,
            "deviceModel" to Build.MODEL,
            "updatedAt" to FieldValue.serverTimestamp(),
            "platform" to "android"
        )

        firestore.collection("users")
            .document(userId)
            .collection("fcm_tokens")
            .document(token)
            .set(firestoreData)
            .await()

        Log.d(TAG, "✅ Token Firestore'a yazıldı.")
    }

    private fun sanitizeToken(token: String): String {
        return token.replace(".", "_")
            .replace("#", "_")
            .replace("$", "_")
            .replace("[", "_")
            .replace("]", "_")
            .replace("/", "_")
    }

    suspend fun deleteTokenOnLogout() = coroutineScope {
        val currentUser = auth.currentUser ?: return@coroutineScope
        try {
            val token = messaging.token.await()
            val safeTokenKey = sanitizeToken(token)

            val firestoreDelete = async {
                try {
                    firestore.collection("users")
                        .document(currentUser.uid)
                        .collection("fcm_tokens")
                        .document(token)
                        .delete()
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "Firestore logout silme hatası: ${e.message}")
                }
            }

            val rtdbDelete = async {
                try {
                    database.getReference("userSessionPrivate")
                        .child(currentUser.uid)
                        .child("fcmTokens")
                        .child(safeTokenKey)
                        .removeValue()
                        .await()
                } catch (e: Exception) {
                    Log.e(TAG, "RTDB logout silme hatası: ${e.message}")
                }
            }

            firestoreDelete.await()
            rtdbDelete.await()

            messaging.deleteToken().await()
            Log.d(TAG, "🧹 Token her iki veritabanından ve cihazdan başarıyla silindi.")
        } catch (e: Exception) {
            Log.e(TAG, "❌ deleteTokenOnLogout hatası: ${e.message}", e)
        }
    }
}