package com.beem.catmap.data.repository;
import com.beem.catmap.data.local.UserSession
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FollowRepository(
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val currentUserId: String?
        get() = UserSession.userId

    suspend fun isFollowing(targetUserId: String): Result<Boolean> {
        val userId = currentUserId ?: return Result.failure(Exception("Oturum açık değil"))

        return try {
            val documentSnapshot = db.collection("users")
                .document(userId)
                .collection("takipEdilenler")
                .document(targetUserId)
                .get()
                .await()

            Result.success(documentSnapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    suspend fun isFollowedBy(targetUserId: String): Result<Boolean> {
        val userId = currentUserId ?: return Result.failure(Exception("Oturum açık değil"))

        return try {
            val documentSnapshot = db.collection("users")
                .document(userId)
                .collection("takipciler")
                .document(targetUserId)
                .get()
                .await()

            Result.success(documentSnapshot.exists())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}