package com.beem.catmap.data.repository

import android.util.Log
import android.util.LruCache
import com.beem.catmap.CatMapApp
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.session.CurrentUserManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserBlockRepository private constructor(
    private val currentUserManager: CurrentUserManager
) {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    // 1. Engellenen Kullanıcı Nesnelerini Saklayan LRU Cache (Son 100 Kullanıcı)
    private val userCache = LruCache<String, Kullanici>(100)

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

    /**
     * İlk yüklemede önce LRU Cache'e, yoksa Firestore'a başvurur.
     */
    suspend fun getInitialBlockedUsers(
        kisiId: String,
        limit: Long = 20
    ): Pair<List<Kullanici>, DocumentSnapshot?> {
        val cachedIds = currentUserManager.benimEngellediklerimState.value

        // 1. Durum: Tüm ID'ler LRU Cache'te tam nesne olarak mevcut mu?
        if (cachedIds.isNotEmpty()) {
            val cachedUsers = cachedIds.mapNotNull { id -> userCache.get(id) }

            // Eğer bellekteki profil detayları tamsa doğrudan dön
            if (cachedUsers.size == cachedIds.size) {
                return Pair(cachedUsers, null)
            }
        }

        // 2. Durum: Bellekte eksik veya veri yoksa Firestore'dan çek
        val (networkUsers, lastDoc) = getBlockedUsersPageFromNetwork(kisiId, limit, null)

        // LRU Cache ve Session güncellemesi
        networkUsers.forEach { user ->
            user.id?.let { userCache.put(it, user) }
        }
        val newIds = networkUsers.mapNotNull { it.id }
        currentUserManager.updateBenimEngellediklerim(newIds)

        return Pair(networkUsers, lastDoc)
    }

    /**
     * Sayfalama (Paging) ile Firestore'dan veri çeker ve LRU Cache'i besler.
     */
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
                userCache.put(id, user) // Çekilen her veriyi LRU Cache'e atıyoruz
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

        // 1. Firestore İşlemi
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

        // 2. LRU Cache Güncellemesi
        userCache.put(targetId, engellenecekKullanici)

        // 3. Lokal ID Listesi / Shared Güncellemesi
        val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (!currentIds.contains(targetId)) {
            currentIds.add(0, targetId)
            currentUserManager.updateBenimEngellediklerim(currentIds)
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
            Log.d("LRU","CHCDEN GELDI")
            return true
        }

        Log.d("LRU","CHCDEN GELMEDI")
        val cachedIds = currentUserManager.benimEngellediklerimState.value
        if (cachedIds.contains(targetUserId)){
            Log.d("LRU","CHCDEN GELMEDI2")
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
        userCache.evictAll()
    }
}