package com.beem.catmap.data.repository

import android.content.Context
import androidx.collection.LruCache
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.gonderi.TargetUserFollowData
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max
class FollowRepository(private val context: Context) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userManager = CurrentUserManager.getInstance(context)

    private val targetUserCache = LruCache<String, TargetUserFollowData>(10)

    private val currentUserId: String?
        get() = UserSession.userId

    suspend fun isFollowing(targetUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(Exception("Oturum açık değil"))
        return@withContext try {
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

    suspend fun isFollowedBy(targetUserId: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val userId = currentUserId ?: return@withContext Result.failure(Exception("Oturum açık değil"))

        return@withContext try {
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

    suspend fun fetchAndCacheFollowCounts(
        userId: String,
        isMyProfile: Boolean,
        forceRefresh: Boolean = false
    ): Result<FollowCounts> = withContext(Dispatchers.IO) {

        if (isMyProfile && !forceRefresh) {
            val followerCount = userManager.profileState.value.takipciSayisi
            val followingCount = userManager.profileState.value.takipEdilenSayisi
            return@withContext Result.success(FollowCounts(followingCount, followerCount))
        }

        if (!isMyProfile && !forceRefresh) {
            val cachedData = targetUserCache.get(userId)
            if (cachedData != null) {
                return@withContext Result.success(FollowCounts(cachedData.followingCount, cachedData.followerCount))
            }
        }

        try {
            val snapshot = db.collection("users")
                .document(userId)
                .get()
                .await()

            if (snapshot.exists()) {
                val followingCount = snapshot.getLong("TakipEdilenSayisi") ?: 0L
                val followerCount = snapshot.getLong("takipciSayisi") ?: 0L

                if (isMyProfile) {
                    userManager.updateFollowCounts(followerCount, followingCount)
                } else {
                    targetUserCache.put(userId, TargetUserFollowData(followerCount, followingCount))
                }

                Result.success(FollowCounts(followingCount, followerCount))
            } else {
                Result.success(FollowCounts(0L, 0L))
            }
        } catch (e: Exception) {
            if (!isMyProfile) {
                val cached = targetUserCache.get(userId)
                if (cached != null) {
                    return@withContext Result.success(FollowCounts(cached.followingCount, cached.followerCount))
                }
            }
            Result.failure(e)
        }
    }

    suspend fun takipet(
        currentUserId: String,
        targetUserId: String,
        myBlockedList: List<String>?,
        blockedMeList: List<String>?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if ((myBlockedList?.contains(targetUserId) == true) ||
            (blockedMeList?.contains(currentUserId) == true)
        ) {
            return@withContext Result.failure(IllegalStateException("Engelleme durumundan dolayı takip edilemez."))
        }

        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)

        return@withContext try {
            db.runTransaction { transaction ->
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

                null
            }.await()

            // Target Cache güncelleme
            val currentCache = targetUserCache.get(targetUserId)
            val newFollowerCount = (currentCache?.followerCount ?: 0L) + 1
            val newFollowingCount = currentCache?.followingCount ?: 0L
            targetUserCache.put(targetUserId, TargetUserFollowData(newFollowerCount, newFollowingCount))

            // Kendi profil sayılarını güncelleme
            val myCurrentFollower = userManager.profileState.value.takipciSayisi
            val myCurrentFollowing = userManager.profileState.value.takipEdilenSayisi
            userManager.updateFollowCounts(myCurrentFollower, myCurrentFollowing + 1)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(currentUserId: String, targetUserId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUserRef = db.collection("users").document(currentUserId)
        val targetUserRef = db.collection("users").document(targetUserId)

        return@withContext try {
            db.runTransaction { transaction ->
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

                null
            }.await()

            // Target Cache güncelleme
            val currentCache = targetUserCache.get(targetUserId)
            if (currentCache != null) {
                val newFollowerCount = max(currentCache.followerCount - 1, 0L)
                targetUserCache.put(targetUserId, TargetUserFollowData(newFollowerCount, currentCache.followingCount))
            }

            // Kendi profil sayılarını güncelleme
            val myCurrentFollower = userManager.profileState.value.takipciSayisi
            val myCurrentFollowing = max(userManager.profileState.value.takipEdilenSayisi - 1, 0L)
            userManager.updateFollowCounts(myCurrentFollower, myCurrentFollowing)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun removeFollower(currentUserId: String, followerId: String): Result<Unit> = withContext(Dispatchers.IO) {
        val currentUserRef = db.collection("users").document(currentUserId)
        val followerRef = db.collection("users").document(followerId)

        return@withContext try {
            db.runTransaction { transaction ->
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

                null
            }.await()

            val myCurrentFollower = max(userManager.profileState.value.takipciSayisi - 1, 0L)
            val myCurrentFollowing = userManager.profileState.value.takipEdilenSayisi
            userManager.updateFollowCounts(myCurrentFollower, myCurrentFollowing)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getCachedTargetUserData(userId: String): TargetUserFollowData? {
        return targetUserCache.get(userId)
    }
}