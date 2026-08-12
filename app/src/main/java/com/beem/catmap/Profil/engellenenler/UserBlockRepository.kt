package com.beem.catmap.data.repository

import android.util.Log
import android.util.LruCache
import com.beem.catmap.CatMapApp
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.utils.BlockUtils
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await
import kotlin.math.max

class UserBlockRepository private constructor(
    private val currentUserManager: CurrentUserManager
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val realDb: FirebaseDatabase = FirebaseDatabase.getInstance()
    private val userCache = LruCache<String, Kullanici>(20)
    private val blockRelationsRef = realDb.getReference("block_relations")

    companion object {
        @Volatile
        private var INSTANCE: UserBlockRepository? = null

        fun getInstance(): UserBlockRepository {
            return INSTANCE ?: synchronized(this) {
                val appInstance = CatMapApp.instance
                val userManager = CurrentUserManager.getInstance(appInstance)
                INSTANCE ?: UserBlockRepository(userManager).also { INSTANCE = it }
            }
        }
    }

    suspend fun getInitialBlockedUsers(
        kisiId: String,
        limit: Long = 20
    ): Pair<List<Kullanici>, DocumentSnapshot?> {
        // İlk yükleme network'ten taze veriyle yapılır
        val (networkUsers, lastDoc) = getBlockedUsersPageFromNetwork(kisiId, limit, null)

        val newIds = networkUsers.mapNotNull { it.id }

        // Mevcut engellenenler ID listesine ekle/güncelle (Listeyi tamamen ezmek yerine birleştir)
        val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableSet()
        currentIds.addAll(newIds)
        currentUserManager.updateBenimEngellediklerim(currentIds.toList())

        return Pair(networkUsers, lastDoc)
    }

    suspend fun getBlockedUsersPageFromNetwork(
        kisiId: String,
        limit: Long = 20,
        lastDocumentSnapshot: DocumentSnapshot? = null
    ): Pair<List<Kullanici>, DocumentSnapshot?> {

        var query = db.collection("users")
            .document(kisiId)
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

            Kullanici().apply {
                this.id = id
                this.kullaniciAdi = kullaniciAdi
                this.fotoUrl = fotoUrl
            }.also { user ->
                userCache.put(id, user)
            }
        }

        return Pair(liste, newLastDoc)
    }

    suspend fun blockUser(
        kisiId: String,
        engellenecekKullanici: Kullanici
    ) {
        val targetId = engellenecekKullanici.id ?: return
        val kullaniciAdi = engellenecekKullanici.kullaniciAdi ?: ""
        val fotoUrl = engellenecekKullanici.fotoUrl ?: ""

        // A. Engellemeden ÖNCE takip bağlarını kontrol et
        val myFollowingDoc = db.collection("users")
            .document(kisiId)
            .collection("takipEdilenler")
            .document(targetId)
            .get()
            .await()

        val myFollowerDoc = db.collection("users")
            .document(kisiId)
            .collection("takipciler")
            .document(targetId)
            .get()
            .await()

        val isIWasFollowing = myFollowingDoc.exists()
        val isHeWasFollowing = myFollowerDoc.exists()

        // 1. Firestore İşlemi (Engellenenler koleksiyonuna yaz)
        db.collection("users")
            .document(kisiId)
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

        addBlockToRealtimeDb(kisiId = kisiId, targetId = targetId)

        // 2. LRU Cache Güncellemesi
        userCache.put(targetId, engellenecekKullanici)

        // 3. Engellenenler ID Listesini Güncelle
        val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (!currentIds.contains(targetId)) {
            currentIds.add(0, targetId)
            currentUserManager.updateBenimEngellediklerim(currentIds)
        }

        // 4. LOKAL SAYAÇLARI GÜNCELLE (Eğer takipleşiyorsak sayıları düş)
        val currentUser = currentUserManager.getCurrentUser()
        var currentFollowingCount = currentUser.takipEdilenSayisi ?: 0L
        var currentFollowerCount = currentUser.takipciSayisi ?: 0L

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

        removeBlockFromRealtimeDb(kisiId = kisiId, targetId = engelliKullaniciId)

        // 2. LRU Cache'ten Kaldırma
        userCache.remove(engelliKullaniciId)

        // 3. Lokal ID Listesi Güncellemesi
        val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (currentIds.contains(engelliKullaniciId)) {
            currentIds.remove(engelliKullaniciId)
            currentUserManager.updateBenimEngellediklerim(currentIds)
        }
    }

    suspend fun isUserBlocked(kisiId: String, targetUserId: String): Boolean {
        if (targetUserId.isBlank() || kisiId.isBlank()) return false

        if (userCache.get(targetUserId) != null) {
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

    private suspend fun addBlockToRealtimeDb(kisiId: String, targetId: String) {
        try {
            val relationKey = BlockUtils.generateRelationKey(kisiId, targetId)

            blockRelationsRef.child(relationKey)
                .child(kisiId)
                .setValue(true)
                .await()
        } catch (e: Exception) {
            Log.e("RTDB", "Realtime DB engelleme kaydı yazılamadı: ${e.message}")
        }
    }
    private suspend fun removeBlockFromRealtimeDb(kisiId: String, targetId: String) {
        try {
            val relationKey = BlockUtils.generateRelationKey(kisiId, targetId)

            blockRelationsRef.child(relationKey)
                .child(kisiId)
                .setValue(false)
                .await()
        } catch (e: Exception) {
            Log.e("RTDB", "Realtime DB engelleme kaydı silinemedi: ${e.message}")
        }
    }

    fun clearCache() {
        userCache.evictAll()
    }
}