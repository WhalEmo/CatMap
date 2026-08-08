package com.beem.catmap.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.LruCache
import com.beem.catmap.data.local.UserSession
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

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
    private val isFollowingCache = LruCache<String, Boolean>(100)
    private val isFollowedByCache = LruCache<String, Boolean>(100)

    private val currentUserId: String?
        get() = UserSession.userId

    suspend fun isFollowing(targetUserId: String, forceRefresh: Boolean = false): Result<Boolean> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(Exception("Oturum açık değil"))

        if (!forceRefresh) {
            val cachedState = isFollowingCache.get(targetUserId)
            if (cachedState != null) {
                return@withContext Result.success(cachedState)
            }
        }

        return@withContext try {
            val documentSnapshot = db.collection("users")
                .document(userId)
                .collection("takipEdilenler")
                .document(targetUserId)
                .get()
                .await()

            val isFollowing = documentSnapshot.exists()
            // RAM Cache'e kaydet
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
            if (cachedState != null) {
                return@withContext Result.success(cachedState)
            }
        }

        return@withContext try {
            val documentSnapshot = db.collection("users")
                .document(userId)
                .collection("takipciler")
                .document(targetUserId)
                .get()
                .await()

            val isFollowed = documentSnapshot.exists()
            // RAM Cache'e kaydet
            isFollowedByCache.put(targetUserId, isFollowed)

            Result.success(isFollowed)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun takipEt(
        currentUserId: String,
        targetUserId: String,
        myBlockedList: List<String>?,
        blockedMeList: List<String>?
    ): Result<FollowResult> = withContext(Dispatchers.IO) {
        if ((myBlockedList?.contains(targetUserId) == true) ||
            (blockedMeList?.contains(currentUserId) == true)
        ) {
            return@withContext Result.failure(IllegalStateException("Engelleme durumundan dolayı takip edilemez."))
        }

        isFollowingCache.put(targetUserId, true)

        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)

        return@withContext try {
            val result = db.runTransaction { transaction ->
                val currentSnap = transaction.get(currentUserRef)
                val targetSnap = transaction.get(targetUserRef)

                var followingCount = currentSnap.getLong("TakipEdilenSayisi") ?: 0L
                var followerCount = targetSnap.getLong("takipciSayisi") ?: 0L

                val followingSubCol = currentUserRef.collection("takipEdilenler")
                val followersSubCol = targetUserRef.collection("takipciler")

                val followingDocRef = followingSubCol.document(targetUserId)
                val followerDocRef = followersSubCol.document(currentUserId)

                val followingDocSnap = transaction.get(followingDocRef)
                val followerDocSnap = transaction.get(followerDocRef)

                var isFollowAdded = false

                if (!followingDocSnap.exists()) {
                    val followingData = hashMapOf(
                        "followedAt" to FieldValue.serverTimestamp(),
                        "KullaniciAdi" to targetSnap.getString("KullaniciAdi"),
                        "profilFotoUrl" to targetSnap.getString("profilFotoUrl"),
                        "ID" to targetSnap.id
                    )
                    transaction.set(followingDocRef, followingData)
                    followingCount += 1
                    isFollowAdded = true
                }

                if (!followerDocSnap.exists()) {
                    val followerData = hashMapOf(
                        "followedAt" to FieldValue.serverTimestamp(),
                        "KullaniciAdi" to currentSnap.getString("KullaniciAdi"),
                        "profilFotoUrl" to currentSnap.getString("profilFotoUrl"),
                        "ID" to currentSnap.id
                    )
                    transaction.set(followerDocRef, followerData)
                    if (isFollowAdded) {
                        followerCount += 1
                    }
                }

                transaction.update(currentUserRef, "TakipEdilenSayisi", followingCount)
                transaction.update(targetUserRef, "takipciSayisi", followerCount)

                FollowResult(
                    currentFollowingCount = followingCount,
                    targetFollowerCount = followerCount
                )
            }.await()

            Result.success(result)
        } catch (e: Exception) {
            // İşlem başarısız olursa Cache'i geri al (Rollback)
            isFollowingCache.put(targetUserId, false)
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(
        currentUserId: String,
        targetUserId: String
    ): Result<FollowResult> = withContext(Dispatchers.IO) {
        isFollowingCache.put(targetUserId, false)

        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)

        return@withContext try {
            val result = db.runTransaction { transaction ->
                val currentSnap = transaction.get(currentUserRef)
                val targetSnap = transaction.get(targetUserRef)

                var followingCount = currentSnap.getLong("TakipEdilenSayisi") ?: 0L
                var followerCount = targetSnap.getLong("takipciSayisi") ?: 0L

                val followingDocRef = currentUserRef.collection("takipEdilenler").document(targetUserId)
                val followerDocRef = targetUserRef.collection("takipciler").document(currentUserId)

                val followingDocSnap = transaction.get(followingDocRef)
                val followerDocSnap = transaction.get(followerDocRef)

                var isUnfollowed = false

                if (followingDocSnap.exists()) {
                    transaction.delete(followingDocRef)
                    followingCount = max(followingCount - 1, 0L)
                    isUnfollowed = true
                }

                if (followerDocSnap.exists()) {
                    transaction.delete(followerDocRef)
                    if (isUnfollowed) {
                        followerCount = max(followerCount - 1, 0L)
                    }
                }

                transaction.update(currentUserRef, "TakipEdilenSayisi", followingCount)
                transaction.update(targetUserRef, "takipciSayisi", followerCount)

                FollowResult(
                    currentFollowingCount = followingCount,
                    targetFollowerCount = followerCount
                )
            }.await()

            Result.success(result)
        } catch (e: Exception) {
            // İşlem başarısız olursa Cache'i geri al (Rollback)
            isFollowingCache.put(targetUserId, true)
            Result.failure(e)
        }
    }

    suspend fun removeFollower(
        currentUserId: String,
        followerId: String
    ): Result<RemoveFollowerResult> = withContext(Dispatchers.IO) {
        // Optimistic Cache Güncellemesi
        isFollowedByCache.put(followerId, false)

        val currentUserRef = db.collection("users").document(currentUserId)
        val followerRef = db.collection("users").document(followerId)

        return@withContext try {
            val result = db.runTransaction { transaction ->
                val currentSnap = transaction.get(currentUserRef)
                val followerSnap = transaction.get(followerRef)

                var followerCount = currentSnap.getLong("takipciSayisi") ?: 0L
                var followingCount = followerSnap.getLong("TakipEdilenSayisi") ?: 0L

                val followerDocRef = currentUserRef.collection("takipciler").document(followerId)
                val followingDocRef = followerRef.collection("takipEdilenler").document(currentUserId)

                val followerDocSnap = transaction.get(followerDocRef)
                val followingDocSnap = transaction.get(followingDocRef)

                var isFollowerRemoved = false

                if (followerDocSnap.exists()) {
                    transaction.delete(followerDocRef)
                    followerCount = max(followerCount - 1, 0L)
                    isFollowerRemoved = true
                }

                if (followingDocSnap.exists()) {
                    transaction.delete(followingDocRef)
                    if (isFollowerRemoved) {
                        followingCount = max(followingCount - 1, 0L)
                    }
                }

                transaction.update(currentUserRef, "takipciSayisi", followerCount)
                transaction.update(followerRef, "TakipEdilenSayisi", followingCount)

                RemoveFollowerResult(
                    currentFollowerCount = followerCount,
                    followerFollowingCount = followingCount
                )
            }.await()

            Result.success(result)
        } catch (e: Exception) {
            isFollowedByCache.put(followerId, true)
            Result.failure(e)
        }
    }


}