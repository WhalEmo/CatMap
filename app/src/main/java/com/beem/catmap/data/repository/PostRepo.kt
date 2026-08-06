package com.beem.catmap.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.collection.LruCache
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.gonderi.ProfilePostCacheData
import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

class PostRepository(private val context: Context) {

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val catsCollection = db.collection("cats")

    private val userManager = CurrentUserManager.getInstance(context)

    private val profileCache = LruCache<String, ProfilePostCacheData>(5)
    private val PAGE_SIZE = 10L

    // Son çekilen belgeyi tutmak için sayfalama takibi (UserId -> LastDocument)
    private val lastDocumentMap = mutableMapOf<String, DocumentSnapshot?>()


    fun isLastPage(userId: String): Boolean {
        return profileCache.get(userId)?.isLastPage ?: false
    }

    suspend fun getKullaniciGonderiIdListesi(userId: String): Result<List<GonderilenKediItem>> {
        return try {
            val snapshot = usersCollection
                .document(userId)
                .collection("GonderilenKediler")
                .orderBy("tarih", Query.Direction.DESCENDING)
                .get()
                .await()

            val items = snapshot.documents.mapNotNull { doc ->
                val kediID = doc.getString("kediID")
                val tarih = doc.getTimestamp("tarih")
                if (kediID != null) GonderilenKediItem(kediID = kediID, tarih = tarih) else null
            }

            Result.success(items)
        } catch (e: Exception) {
            Log.e("PostRepository", "Kullanıcı kedi ID listesi alınamadı: ${e.message}")
            Result.failure(e)
        }
    }

    // İlk sayfayı Firestore Alt Koleksiyonundan çeker
    suspend fun getUserPosts(
        userId: String,
        forceRefresh: Boolean = false
    ): Result<ProfilePostCacheData> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(Exception("Geçersiz UserId"))

        val cachedData = profileCache.get(userId)
        if (cachedData != null && !forceRefresh) {
            Log.d("POST_REPO_DEBUG", "Veriler ÖNBELLEKTEN getirildi. UserId: $userId")
            return@withContext Result.success(cachedData)
        }

        lastDocumentMap[userId] = null

        try {
            val query = usersCollection
                .document(userId)
                .collection("GonderilenKediler")
                .orderBy("tarih", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                val emptyCache = ProfilePostCacheData(emptyList(), emptyList(), 0, true)
                profileCache.put(userId, emptyCache)
                lastDocumentMap[userId] = null
                return@withContext Result.success(emptyCache)
            }

            // Son belgeyi sayfalama için kaydet
            lastDocumentMap[userId] = snapshot.documents.lastOrNull()

            val batchItems = snapshot.documents.mapNotNull { doc ->
                val kediID = doc.getString("kediID")
                val tarih = doc.getTimestamp("tarih")
                if (kediID != null) GonderilenKediItem(kediID = kediID, tarih = tarih) else null
            }

            val isLast = snapshot.size() < PAGE_SIZE
            val gonderiler = fetchGonderilerByIdsInternal(batchItems)

            val newCacheData = ProfilePostCacheData(
                posts = gonderiler,
                idList = batchItems,
                offset = batchItems.size,
                isLastPage = isLast
            )

            profileCache.put(userId, newCacheData)
            Result.success(newCacheData)
        } catch (e: Exception) {
            Log.e("POST_REPO_DEBUG", "Gönderiler çekilirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    // Sayfalama (Pagination): Son kalınan belgeden itibaren sonraki PAGE_SIZE kadar veriyi çeker
    suspend fun dahaFazlaGonderiGetir(userId: String): Result<ProfilePostCacheData> = withContext(Dispatchers.IO) {
        val cachedData = profileCache.get(userId)
            ?: return@withContext Result.failure(Exception("Önbellek bulunamadı!"))

        val lastDoc = lastDocumentMap[userId]
        if (cachedData.isLastPage || lastDoc == null) {
            return@withContext Result.success(cachedData)
        }

        try {
            val query = usersCollection
                .document(userId)
                .collection("GonderilenKediler")
                .orderBy("tarih", Query.Direction.DESCENDING)
                .startAfter(lastDoc)
                .limit(PAGE_SIZE)

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                val updatedCache = ProfilePostCacheData(
                    posts = cachedData.posts,
                    idList = cachedData.idList,
                    offset = cachedData.offset,
                    isLastPage = true
                )
                profileCache.put(userId, updatedCache)
                return@withContext Result.success(updatedCache)
            }

            lastDocumentMap[userId] = snapshot.documents.lastOrNull()

            val newBatchItems = snapshot.documents.mapNotNull { doc ->
                val kediID = doc.getString("kediID")
                val tarih = doc.getTimestamp("tarih")
                if (kediID != null) GonderilenKediItem(kediID = kediID, tarih = tarih) else null
            }

            val isLast = snapshot.size() < PAGE_SIZE
            val newGonderiler = fetchGonderilerByIdsInternal(newBatchItems)

            val updatedPosts = cachedData.posts + newGonderiler
            val updatedIdList = cachedData.idList + newBatchItems

            val updatedCache = ProfilePostCacheData(
                posts = updatedPosts,
                idList = updatedIdList,
                offset = updatedPosts.size,
                isLastPage = isLast
            )

            profileCache.put(userId, updatedCache)
            Result.success(updatedCache)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGonderiDetaylariByIds(items: List<GonderilenKediItem>): Result<List<Gonderi>> {
        if (items.isEmpty()) return Result.success(emptyList())

        return try {
            val gonderiler = fetchGonderilerByIdsInternal(items)
            Result.success(gonderiler)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi detayları çekilirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun kullaniciGonderiSil(
        userId: String,
        kediId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userRef = usersCollection.document(userId)
            val subCollRef = userRef.collection("GonderilenKediler")

            val querySnapshot = subCollRef.whereEqualTo("kediID", kediId).get().await()

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val currentPostCount = userSnap.getLong("gonderiSayisi") ?: 0L

                for (doc in querySnapshot.documents) {
                    transaction.delete(doc.reference)
                }

                if (querySnapshot.documents.isNotEmpty()) {
                    val newCount = max(currentPostCount - 1, 0L)
                    transaction.update(userRef, "gonderiSayisi", newCount)
                }
                null
            }.await()

            // Local cache ve session güncellemesi
            removePostFromCacheInternal(userId, kediId)

            if (userId == UserSession.userId) {
                val currentCount = userManager.profileState.value.gonderiSayisi
                userManager.updateGonderiSayisi(max(currentCount - 1, 0L))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi silinirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun haritadanKediSil(kediId: String): Result<Unit> = runCatching {
        catsCollection.document(kediId).delete().await()
    }

    suspend fun kullaniciGonderiKaydet(
        userId: String,
        kediId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userRef = usersCollection.document(userId)
            val subCollRef = userRef.collection("GonderilenKediler")

            val yeniKediDoc = subCollRef.document()
            val yeniKediData = mapOf(
                "kediID" to kediId,
                "tarih" to Timestamp.now()
            )

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val currentPostCount = userSnap.getLong("gonderiSayisi") ?: 0L

                transaction.set(yeniKediDoc, yeniKediData)
                transaction.update(userRef, "gonderiSayisi", currentPostCount + 1)
                null
            }.await()

            addPostToCacheInternal(userId, kediId)

            if (userId == UserSession.userId) {
                val currentCount = userManager.profileState.value.gonderiSayisi
                userManager.updateGonderiSayisi(currentCount + 1)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi kaydedilirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    private fun removePostFromCacheInternal(userId: String, kediId: String) {
        val cachedData = profileCache.get(userId) ?: return
        val wasInLoaded = cachedData.posts.any { it.kediID == kediId }
        val updatedPosts = cachedData.posts.filterNot { it.kediID == kediId }
        val updatedIdList = cachedData.idList.filterNot { it.kediID == kediId }
        val updatedOffset = if (wasInLoaded) (cachedData.offset - 1).coerceAtLeast(0) else cachedData.offset

        profileCache.put(
            userId,
            ProfilePostCacheData(updatedPosts, updatedIdList, updatedOffset, cachedData.isLastPage)
        )
    }

    private suspend fun addPostToCacheInternal(userId: String, kediId: String) {
        val cachedData = profileCache.get(userId) ?: return
        val yeniKediItem = GonderilenKediItem(kediID = kediId, tarih = Timestamp.now())
        val updatedIdList = listOf(yeniKediItem) + cachedData.idList

        val yeniListe = fetchGonderilerByIdsInternal(listOf(yeniKediItem)) + cachedData.posts

        profileCache.put(
            userId,
            ProfilePostCacheData(yeniListe, updatedIdList, cachedData.offset + 1, cachedData.isLastPage)
        )
    }

    private suspend fun fetchGonderilerByIdsInternal(items: List<GonderilenKediItem>): List<Gonderi> {
        if (items.isEmpty()) return emptyList()
        val tarihMap = items.associate { it.kediID to it.tarih }
        val kediIds = items.map { it.kediID }.distinct()
        val chunks = kediIds.chunked(30)

        return coroutineScope {
            chunks.map { chunk ->
                async {
                    val snapshot = catsCollection.whereIn(FieldPath.documentId(), chunk).get().await()
                    snapshot.documents.mapNotNull { doc ->
                        val fotoList = doc.get("photoUri") as? List<String> ?: emptyList()
                        if (fotoList.isEmpty()) return@mapNotNull null
                        Gonderi(
                            kediID = doc.id,
                            fotoUrlListesi = fotoList,
                            aciklama = doc.getString("kediHakkinda"),
                            kediAdi = doc.getString("kediAdi"),
                            tarih = tarihMap[doc.id],
                            begeniSayisi = doc.getLong("begeniSayisi") ?: 0L
                        )
                    }
                }
            }.awaitAll().flatten().sortedByDescending { it.tarih }
        }
    }
}