package com.beem.catmap.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.LruCache
import com.beem.catmap.data.local.UserSession
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class FollowResult(
    val currentFollowingCount: Long,
    val targetFollowerCount: Long
)

data class RemoveFollowerResult(
    val currentFollowerCount: Long,
    val followerFollowingCount: Long
)

class FollowRepository private constructor(context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: FollowRepository? = null

        fun getInstance(context: Context): FollowRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FollowRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val functions: FirebaseFunctions = FirebaseFunctions.getInstance()

    private val isFollowingCache = LruCache<String, Boolean>(100)
    private val isFollowedByCache = LruCache<String, Boolean>(100)

    private val currentUserId: String?
        get() = UserSession.userId

    // Okuma işlemlerini hafif olduğu için doğrudan Firestore'dan çekmeye devam edebiliriz
    suspend fun isFollowing(targetUserId: String, forceRefresh: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(Exception("Oturum açık değil"))

        if (!forceRefresh) {
            val cachedState = isFollowingCache.get(targetUserId)
            if (cachedState != null) return@withContext Result.success(cachedState)
        }

        return@withContext try {
            val documentSnapshot = db.collection("users")
                .document(userId)
                .collection("takipEdilenler")
                .document(targetUserId)
                .get()
                .await()

            val isFollowing = documentSnapshot.exists()
            isFollowingCache.put(targetUserId, isFollowing)
            Result.success(isFollowing)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isFollowedBy(targetUserId: String, forceRefresh: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(Exception("Oturum açık değil"))

        if (!forceRefresh) {
            val cachedState = isFollowedByCache.get(targetUserId)
            if (cachedState != null) return@withContext Result.success(cachedState)
        }

        return@withContext try {
            val documentSnapshot = db.collection("users")
                .document(userId)
                .collection("takipciler")
                .document(targetUserId)
                .get()
                .await()

            val isFollowed = documentSnapshot.exists()
            isFollowedByCache.put(targetUserId, isFollowed)
            Result.success(isFollowed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // --- CLOUD FUNCTIONS ÇAĞRILARI ---

    suspend fun takipEt(targetUserId: String): Result<FollowResult> = withContext(Dispatchers.IO) {
        isFollowingCache.put(targetUserId, true)

        val data = hashMapOf("targetUserId" to targetUserId)

        return@withContext try {
            val httpsResult = functions
                .getHttpsCallable("followUser")
                .call(data)
                .await()

            val resultMap = httpsResult.data as? Map<*, *>
            val followingCount = (resultMap?.get("currentFollowingCount") as? Number)?.toLong() ?: 0L
            val followerCount = (resultMap?.get("targetFollowerCount") as? Number)?.toLong() ?: 0L

            Result.success(FollowResult(followingCount, followerCount))
        } catch (e: Exception) {
            isFollowingCache.put(targetUserId, false) // Rollback
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUserId: String): Result<FollowResult> = withContext(Dispatchers.IO) {
        isFollowingCache.put(targetUserId, false)

        val data = hashMapOf("targetUserId" to targetUserId)

        return@withContext try {
            val httpsResult = functions
                .getHttpsCallable("unfollowUser")
                .call(data)
                .await()

            val resultMap = httpsResult.data as? Map<*, *>
            val followingCount = (resultMap?.get("currentFollowingCount") as? Number)?.toLong() ?: 0L
            val followerCount = (resultMap?.get("targetFollowerCount") as? Number)?.toLong() ?: 0L

            Result.success(FollowResult(followingCount, followerCount))
        } catch (e: Exception) {
            isFollowingCache.put(targetUserId, true) // Rollback
            Result.failure(e)
        }
    }

    suspend fun removeFollower(followerId: String): Result<RemoveFollowerResult> = withContext(Dispatchers.IO) {
        isFollowedByCache.put(followerId, false)

        val data = hashMapOf("followerId" to followerId)

        return@withContext try {
            val httpsResult = functions
                .getHttpsCallable("removeFollower")
                .call(data)
                .await()

            val resultMap = httpsResult.data as? Map<*, *>
            val followerCount = (resultMap?.get("currentFollowerCount") as? Number)?.toLong() ?: 0L
            val followingCount = (resultMap?.get("followerFollowingCount") as? Number)?.toLong() ?: 0L

            Result.success(RemoveFollowerResult(followerCount, followingCount))
        } catch (e: Exception) {
            isFollowedByCache.put(followerId, true) // Rollback
            Result.failure(e)
        }
    }
}