package com.beem.catmap.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.collection.LruCache
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.FollowResult
import com.beem.catmap.data.model.PaginatedResult
import com.beem.catmap.data.model.RemoveFollowerResult
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.auth.exceptions.IsBlockedByException
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import com.google.firebase.functions.FirebaseFunctionsException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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

    private val userManager = CurrentUserManager.getInstance(context)

    // Önbellek Tanımlamaları
    private val isFollowingCache = LruCache<String, Boolean>(100)
    private val isFollowedByCache = LruCache<String, Boolean>(100)
    private val takipcilerCache = LruCache<String, PaginatedResult<UserModel>>(10)
    private val takipEdilenlerCache = LruCache<String, PaginatedResult<UserModel>>(10)

    private val currentUserId: String?
        get() = UserSession.userId

    // =========================================================================
    // 1. SORGULAMA VE LİSTELEME İŞLEMLERİ (FIRESTORE)
    // =========================================================================

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

    suspend fun getTakipciler(
        userId: String,
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null,
        forceRefresh: Boolean = false
    ): Result<PaginatedResult<UserModel>> = runCatching {
        if (forceRefresh) {
            clearUserCache(userId)
        } else if (lastDocument == null) {
            takipcilerCache.get(userId)?.let { cachedResult ->
                return@runCatching cachedResult
            }
        }

        var query = db.collection("users")
            .document(userId)
            .collection("takipciler")
            .limit(limit)

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            UserModel().apply {
                id = doc.getString("ID") ?: doc.id
                username = doc.getString("KullaniciAdi") ?: ""
                photoUrl = doc.getString("profilFotoUrl") ?: ""
                isFollowers = 2
            }
        }

        val result = PaginatedResult(
            items = items,
            lastDocument = snapshot.documents.lastOrNull(),
            isLastPage = items.size < limit
        )

        if (lastDocument == null) {
            takipcilerCache.put(userId, result)
        }

        result
    }

    suspend fun getTakipEdilenler(
        userId: String,
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null,
        forceRefresh: Boolean = false
    ): Result<PaginatedResult<UserModel>> = runCatching {
        if (forceRefresh) {
            clearUserCache(userId)
        } else if (lastDocument == null) {
            takipEdilenlerCache.get(userId)?.let { cachedResult ->
                return@runCatching cachedResult
            }
        }

        var query = db.collection("users")
            .document(userId)
            .collection("takipEdilenler")
            .limit(limit)

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            UserModel().apply {
                username = doc.getString("KullaniciAdi") ?: ""
                photoUrl = doc.getString("profilFotoUrl") ?: ""
                id = doc.getString("ID") ?: ""
                isFollowing = 2
            }
        }

        val result = PaginatedResult(
            items = items,
            lastDocument = snapshot.documents.lastOrNull(),
            isLastPage = items.size < limit
        )

        if (lastDocument == null) {
            takipEdilenlerCache.put(userId, result)
        }

        result
    }

    // =========================================================================
    // 2. YAZMA VE MUTASYON İŞLEMLERİ (CLOUD FUNCTIONS)
    // =========================================================================

    suspend fun takipEt(targetUserId: String): Result<FollowResult> = withContext(Dispatchers.IO) {
        isFollowingCache.put(targetUserId, true)
        clearUserCache(targetUserId)
        currentUserId?.let { clearUserCache(it) }

        val data = hashMapOf("targetUserId" to targetUserId)

        return@withContext try {
            Log.d("FOLLOW_DEBUG", "1. Repository: Cloud Function çağrısı başlatılıyor... targetUserId: $targetUserId")
            val httpsResult = functions
                .getHttpsCallable("followUser")
                .call(data)
                .await()

            val resultMap = httpsResult.data as? Map<*, *>
            val followingCount = (resultMap?.get("currentFollowingCount") as? Number)?.toLong() ?: 0L
            val followerCount = (resultMap?.get("targetFollowerCount") as? Number)?.toLong() ?: 0L

            val currentProfile = userManager.profileState.value
            userManager.updateFollowCounts(
                takipciSayisi = currentProfile.followersCount,
                takipEdilenSayisi = followingCount
            )

            Result.success(FollowResult(followingCount, followerCount))
        } catch (e: FirebaseFunctionsException) {
            isFollowingCache.put(targetUserId, false) // Rollback

            if (e.code == FirebaseFunctionsException.Code.PERMISSION_DENIED) {
                Result.failure(IsBlockedByException())
            } else {
                Result.failure(e)
            }
        } catch (e: Exception) {
            isFollowingCache.put(targetUserId, false) // Rollback
            Result.failure(e)
        }
    }

    suspend fun unfollowUser(targetUserId: String): Result<FollowResult> = withContext(Dispatchers.IO) {
        isFollowingCache.put(targetUserId, false)
        clearUserCache(targetUserId)
        currentUserId?.let { clearUserCache(it) }

        val data = hashMapOf("targetUserId" to targetUserId)

        return@withContext try {
            val httpsResult = functions
                .getHttpsCallable("unfollowUser")
                .call(data)
                .await()

            val resultMap = httpsResult.data as? Map<*, *>
            val followingCount = (resultMap?.get("currentFollowingCount") as? Number)?.toLong() ?: 0L
            val followerCount = (resultMap?.get("targetFollowerCount") as? Number)?.toLong() ?: 0L

            val currentProfile = userManager.profileState.value
            userManager.updateFollowCounts(
                takipciSayisi = currentProfile.followersCount,
                takipEdilenSayisi = followingCount
            )

            Result.success(FollowResult(followingCount, followerCount))
        } catch (e: Exception) {
            isFollowingCache.put(targetUserId, true) // Rollback
            Result.failure(e)
        }
    }

    suspend fun removeFollower(followerId: String): Result<RemoveFollowerResult> = withContext(Dispatchers.IO) {
        isFollowedByCache.put(followerId, false)
        clearUserCache(followerId)
        currentUserId?.let { clearUserCache(it) }

        val data = hashMapOf("followerId" to followerId)

        return@withContext try {
            val httpsResult = functions
                .getHttpsCallable("removeFollower")
                .call(data)
                .await()

            val resultMap = httpsResult.data as? Map<*, *>
            val followerCount = (resultMap?.get("currentFollowerCount") as? Number)?.toLong() ?: 0L
            val followingCount = (resultMap?.get("followerFollowingCount") as? Number)?.toLong() ?: 0L

            val currentProfile = userManager.profileState.value
            userManager.updateFollowCounts(
                takipciSayisi = followerCount,
                takipEdilenSayisi = currentProfile.followingCount
            )

            Result.success(RemoveFollowerResult(followerCount, followingCount))
        } catch (e: Exception) {
            isFollowedByCache.put(followerId, true) // Rollback
            Result.failure(e)
        }
    }


    fun clearUserCache(userId: String) {
        isFollowingCache.remove(userId)
        isFollowedByCache.remove(userId)
        takipcilerCache.remove(userId)
        takipEdilenlerCache.remove(userId)
    }

    fun clearAllCache() {
        isFollowingCache.evictAll()
        isFollowedByCache.evictAll()
        takipcilerCache.evictAll()
        takipEdilenlerCache.evictAll()
    }
}