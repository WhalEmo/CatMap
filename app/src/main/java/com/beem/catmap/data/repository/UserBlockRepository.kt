package com.beem.catmap.data.repository

import android.util.Log
import android.util.LruCache
import com.beem.catmap.CatMapApp
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.session.CurrentUserManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class UserBlockRepository private constructor(
    private val currentUserManager: CurrentUserManager
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val userModelCache = LruCache<String, UserModel>(20)

    companion object {
        @Volatile
        private var INSTANCE: UserBlockRepository? = null

        fun getInstance(): UserBlockRepository {
            return INSTANCE ?: synchronized(this) {
                val appInstance = CatMapApp.Companion.instance
                val userManager = CurrentUserManager.Companion.getInstance(appInstance)
                INSTANCE ?: UserBlockRepository(userManager).also { INSTANCE = it }
            }
        }
    }

    suspend fun getInitialBlockedUsers(
        userId: String,
        limit: Long = 20
    ): Pair<List<UserModel>, DocumentSnapshot?> {
        val (networkUsers, lastDoc) = getBlockedUsersPageFromNetwork(userId, limit, null)

        val newIds = networkUsers.mapNotNull { it.id }

        val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableSet()
        currentIds.addAll(newIds)
        currentUserManager.updateBenimEngellediklerim(currentIds.toList())

        return Pair(networkUsers, lastDoc)
    }

    suspend fun getBlockedUsersPageFromNetwork(
        userId: String,
        limit: Long = 20,
        lastDocumentSnapshot: DocumentSnapshot? = null
    ): Pair<List<UserModel>, DocumentSnapshot?> {

        var query = db.collection("users")
            .document(userId)
            .collection("blockedUsers")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .limit(limit)

        if (lastDocumentSnapshot != null) {
            query = query.startAfter(lastDocumentSnapshot)
        }

        val snapshot = query.get().await()
        val newLastDoc = snapshot.documents.lastOrNull()

        val liste = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val kullaniciAdi = doc.getString("kullaniciAdi") ?: ""
            val fotoUrl = doc.getString("fotoUrl") ?: ""

            UserModel().apply {
                this.id = id
                this.username = kullaniciAdi
                this.photoUrl = fotoUrl
            }.also { user ->
                userModelCache.put(id, user)
            }
        }

        return Pair(liste, newLastDoc)
    }

    suspend fun blockUser(
        userId: String,
        blockUserModel: UserModel
    ) {
        val targetId = blockUserModel.id ?: return
        val kullaniciAdi = blockUserModel.username ?: ""
        val fotoUrl = blockUserModel.photoUrl ?: ""

        val myFollowingDoc = try {
            db.collection("users")
            .document(userId)
            .collection("takipEdilenler")
            .document(targetId)
            .get()
            .await()
            .exists()
        } catch (e: Exception) {
            false
        }

        val myFollowerDoc = try {
            db.collection("users")
            .document(userId)
            .collection("takipciler")
            .document(targetId)
            .get()
            .await()
            .exists()
        } catch (e: Exception) {
            false
        }

        val isIWasFollowing = myFollowingDoc
        val isHeWasFollowing = myFollowerDoc

        // 1. Firestore İşlemi (Engellenenler koleksiyonuna yaz)
        db.collection("users")
            .document(userId)
            .collection("blockedUsers")
            .document(targetId)
            .set(
                mapOf(
                    "blockedAt" to FieldValue.serverTimestamp(),
                    "kullaniciAdi" to kullaniciAdi,
                    "fotoUrl" to fotoUrl
                )
            )
            .await()

        // 2. LRU Cache Güncellemesi
        userModelCache.put(targetId, blockUserModel)

        // 3. Engellenenler ID Listesini Güncelle
        val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (!currentIds.contains(targetId)) {
            currentIds.add(0, targetId)
            currentUserManager.updateBenimEngellediklerim(currentIds)
        }

        // 4. LOKAL SAYAÇLARI GÜNCELLE (Eğer takipleşiyorsak sayıları düş)
        val currentUser = currentUserManager.getCurrentUser()
        var currentFollowingCount = currentUser.followingCount ?: 0L
        var currentFollowerCount = currentUser.followersCount ?: 0L

        if (isIWasFollowing) {
            currentFollowingCount = max(0L, currentFollowingCount - 1)
        }
        if (isHeWasFollowing) {
            currentFollowerCount = max(0L, currentFollowerCount - 1)
        }

        if (isIWasFollowing || isHeWasFollowing) {
            currentUserManager.updateFollowCounts(
                takipciSayisi = currentFollowerCount,
                takipEdilenSayisi = currentFollowingCount
            )
        }
    }

    suspend fun unblockUser(
        kisiId: String,
        engelliKullaniciId: String
    ) {
        // 1. Firestore Silme
        db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .document(engelliKullaniciId)
            .delete()
            .await()

        // 2. LRU Cache'ten Kaldırma
        userModelCache.remove(engelliKullaniciId)

        // 3. Lokal ID Listesi Güncellemesi
        val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (currentIds.contains(engelliKullaniciId)) {
            currentIds.remove(engelliKullaniciId)
            currentUserManager.updateBenimEngellediklerim(currentIds)
        }
    }

    suspend fun isUserBlocked(kisiId: String, targetUserId: String): Boolean {
        if (targetUserId.isBlank() || kisiId.isBlank()) return false

        if (userModelCache.get(targetUserId) != null) {
            Log.d("LRU", "CHCDEN GELDI")
            return true
        }

        Log.d("LRU", "CHCDEN GELMEDI")
        val cachedIds = currentUserManager.benimEngellediklerimState.value
        if (cachedIds.contains(targetUserId)) {
            Log.d("LRU", "CHCDEN GELMEDI2")
            return true
        }

        return try {
            val doc = db.collection("users")
                .document(kisiId)
                .collection("blockedUsers")
                .document(targetUserId)
                .get()
                .await()

            doc.exists()
        } catch (e: Exception) {
            false
        }
    }

    fun clearCache() {
        userModelCache.evictAll()
    }
}