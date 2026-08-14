package com.beem.catmap.data.repository

import android.util.Log
import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.NeighborhoodBadgeModel
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class BadgeRepository private constructor() {

    private val db = FirebaseFirestore.getInstance()

    suspend fun getUserBadges(
        userId: String
    ): Result<List<NeighborhoodBadgeModel>> {

        if (userId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("UserId boş olamaz.")
            )
        }

        return try {

            val snapshot = db.collection("users")
                .document(userId)
                .collection("neighborhoodBadges")
                .get()
                .await()

            val userNeighborhoods =
                snapshot.toObjects(
                    NeighborhoodBadgeModel::class.java
                )

            Result.success(userNeighborhoods)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun equipBadge(
        userId: String,
        badge: NeighborhoodBadgeModel
    ): Result<Unit> {

        if (userId.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "UserId boş olamaz."
                )
            )
        }

        if (!badge.isUnlocked) {
            return Result.failure(
                IllegalStateException(
                    "Kilitli bir rozet kullanılamaz."
                )
            )
        }

        if (badge.badgeId.isBlank()) {
            return Result.failure(
                IllegalArgumentException(
                    "Mahalle rozet ID'si boş olamaz."
                )
            )
        }

        return try {

            val equippedBadge = mapOf(
                "neighborhoodBadgeId" to badge.badgeId,
                "tierLevel" to badge.currentTier.level,
                "city" to badge.city,
                "district" to badge.district,
                "neighborhood" to badge.neighborhood,
                "equippedAt" to
                        com.google.firebase.firestore.FieldValue.serverTimestamp()
            )

            db.collection("users")
                .document(userId)
                .update(
                    "equippedBadge",
                    equippedBadge
                )
                .await()

            Result.success(Unit)

        } catch (e: Exception) {

            Result.failure(e)
        }
    }

    suspend fun getEquippedBadge(
        userId: String
    ): Result<EquippedBadgeModel?> {

        if (userId.isBlank()) {
            return Result.failure(
                IllegalArgumentException("UserId boş olamaz.")
            )
        }

        return try {

            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            val equippedBadge = snapshot.get(
                "equippedBadge",
                EquippedBadgeModel::class.java
            )

            Result.success(equippedBadge)

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