package com.beem.catmap.data.repository

import android.util.Log
import com.beem.catmap.data.local.CacheHelperPostLikeV2
import com.beem.catmap.data.model.PresenceAndBlockResult
import com.beem.catmap.data.model.PresenceState
import com.beem.catmap.ui.message.models.BlockState
import com.beem.catmap.utils.BlockUtils
import com.beem.catmap.utils.toFormattedLastSeen
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val realDb: FirebaseDatabase = FirebaseDatabase.getInstance()
) {

    companion object {
        @Volatile
        private var INSTANCE: UserRepository? = null

        @JvmStatic
        fun getInstance(): UserRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserRepository().also { INSTANCE = it }
            }
        }
    }

    private val durumlarRef = realDb.getReference("durumlar")
    private val blockRelationsRef = realDb.getReference("block_relations")

    // 1. Ortak Engel Düğümünü Dinleyen Flow
    private fun observeBlockRelation(currentUserId: String, receiverId: String): Flow<BlockState> = callbackFlow {
        val relationKey = BlockUtils.generateRelationKey(currentUserId, receiverId)
        val ref = blockRelationsRef.child(relationKey)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val amIBlocking = snapshot.child(currentUserId).getValue(Boolean::class.java) ?: false
                val isOtherBlocking = snapshot.child(receiverId).getValue(Boolean::class.java) ?: false

                val blockState = when {
                    amIBlocking && isOtherBlocking -> BlockState.MutualBlock
                    amIBlocking -> BlockState.BlockedByMe
                    isOtherBlocking -> BlockState.BlockedByUser
                    else -> BlockState.None
                }

                trySend(blockState)
            }

            override fun onCancelled(error: DatabaseError) {
                trySend(BlockState.None)
            }
        }

        ref.addValueEventListener(listener)
        awaitClose { ref.removeEventListener(listener) }
    }

    // 2. Presence Dinleyen Flow
    private fun observePresence(receiverId: String): Flow<PresenceState> = callbackFlow {
        val userStatusRef = durumlarRef.child(receiverId)

        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val isOnline = snapshot.child("cevrimici").getValue(Boolean::class.java) ?: false
                val lastSeen = snapshot.child("sonGorulme").getValue(Long::class.java) ?: 0L

                val statusText = if (isOnline) {
                    "Çevrimiçi"
                } else if (lastSeen > 0) {
                    "Son görülme: ${lastSeen.toFormattedLastSeen()}"
                } else {
                    "Çevrimdışı"
                }

                trySend(PresenceState.Success(isOnline = isOnline, lastSeenText = statusText))
            }

            override fun onCancelled(error: DatabaseError) {
                // PERMISSION_DENIED gelse bile Error state fırlatıyoruz
                trySend(PresenceState.Error(error.toException()))
            }
        }

        userStatusRef.addValueEventListener(listener)
        awaitClose { userStatusRef.removeEventListener(listener) }
    }

    // 🟢 Birleşik Akış
    fun getUserPresenceAndBlockFlow(currentUserId: String, receiverId: String): Flow<PresenceAndBlockResult> {
        return observeBlockRelation(currentUserId, receiverId).flatMapLatest { blockState ->
            if (blockState != BlockState.None) {
                // 🛑 ENGEL VAR: Presence dinleyicisini HİÇ BAŞLATMA / VARSANI İPTAL ET!
                // Böylece PERMISSION_DENIED hatası alıp dinleyiciyi öldürmeyiz.
                flowOf(
                    PresenceAndBlockResult(
                        presenceState = PresenceState.Blocked,
                        blockState = blockState
                    )
                )
            } else {
                // 🔓 ENGEL YOK: Presence dinleyicisini Güvenle Çalıştır!
                observePresence(receiverId).map { presence ->
                    PresenceAndBlockResult(
                        presenceState = presence,
                        blockState = BlockState.None
                    )
                }
            }
        }
    }

    suspend fun syncUserLikes(userId: String) = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext

        try {
            val documentSnapshot = Firebase.firestore
                .collection("users")
                .document(userId)
                .get()
                .await()

            if (documentSnapshot.exists()) {
                val rawList = documentSnapshot.get("begendigiGonderiler") as? List<*>
                val stringSet = rawList?.filterIsInstance<String>()?.toSet() ?: emptySet()
                CacheHelperPostLikeV2.setLikesList(stringSet)
            }
        } catch (e: Exception) {
            Log.e("UserRepository", "Kullanıcı beğenileri senkronize edilemedi: ${e.message}", e)
        }
    }

    fun syncUserLikesAsync(userId: String, scope: CoroutineScope) {
        scope.launch(Dispatchers.IO) {
            syncUserLikes(userId)
        }
    }
}