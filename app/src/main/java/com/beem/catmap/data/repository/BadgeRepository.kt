package com.beem.catmap.data.repository

import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class BadgeRepository private constructor() {

    private val db = FirebaseFirestore.getInstance()

    /**
     * Kullanıcının kazandığı mahalle rozetlerini tek seferlik (get) çeker.
     * Maliyeti düşüktür ve pili yormaz.
     */
    suspend fun getUserBadges(userId: String): Result<List<NeighborhoodBadgeModel>> {
        return try {
            val snapshot = db.collection("users")
                .document(userId)
                .collection("neighborhoodBadges")
                .orderBy("unlockedAt", Query.Direction.DESCENDING)
                .get()
                .await()

            val badges = snapshot.toObjects(NeighborhoodBadgeModel::class.java)
            Result.success(badges)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: BadgeRepository? = null

        fun getInstance(): BadgeRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: BadgeRepository().also { INSTANCE = it }
            }
        }
    }
}