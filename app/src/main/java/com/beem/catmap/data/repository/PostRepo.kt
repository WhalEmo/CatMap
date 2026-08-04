package com.beem.catmap.data.repository

import android.annotation.SuppressLint
import android.util.Log
import androidx.collection.LruCache
import com.beem.catmap.gonderi.ProfilePostCacheData
import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

object PostRepository {
    @SuppressLint("StaticFieldLeak")
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val catsCollection = db.collection("cats")

    private val profileCache = LruCache<String, ProfilePostCacheData>(5)
    private val PAGE_SIZE = 10

    // Kullanıcının profilindeki tüm kedi ID ve tarihlerini 1 KERE çeker
    suspend fun getKullaniciGonderiIdListesi(userId: String): Result<List<GonderilenKediItem>> {
        return try {
            val snapshot = usersCollection
                .document(userId)
                .get()
                .await()

            if (!snapshot.exists()) {
                Result.success(emptyList())
            } else {
                val rawList = snapshot.get("GonderilenKediler") as? List<Map<String, Any>> ?: emptyList()
                val items = rawList.mapNotNull { map ->
                    val kediID = map["kediID"] as? String
                    val tarih = map["tarih"] as? Timestamp
                    if (kediID != null) {
                        GonderilenKediItem(kediID = kediID, tarih = tarih)
                    } else {
                        null
                    }
                }.sortedByDescending { it.tarih } // En yeni en üstte

                Result.success(items)
            }
        } catch (e: Exception) {
            Log.e("PostRepository", "Kullanıcı kedi ID listesi alınamadı: ${e.message}")
            Result.failure(e)
        }
    }

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

        try {
            val snapshot = usersCollection.document(userId).get().await()
            val rawList = snapshot.get("GonderilenKediler") as? List<Map<String, Any>> ?: emptyList()

            val fullIdList = rawList.mapNotNull { map ->
                val kediID = map["kediID"] as? String
                val tarih = map["tarih"] as? Timestamp
                if (kediID != null) GonderilenKediItem(kediID = kediID, tarih = tarih) else null
            }.sortedByDescending { it.tarih }

            if (fullIdList.isEmpty()) {
                val emptyCache = ProfilePostCacheData(emptyList(), emptyList(), 0, true)
                profileCache.put(userId, emptyCache)
                return@withContext Result.success(emptyCache)
            }

            // 2. İlk batch'i çek
            val firstBatch = fullIdList.take(PAGE_SIZE)
            val isLast = firstBatch.size >= fullIdList.size
            val gonderiler = fetchGonderilerByIdsInternal(firstBatch)

            val newCacheData = ProfilePostCacheData(
                posts = gonderiler,
                idList = fullIdList,
                offset = firstBatch.size,
                isLastPage = isLast
            )

            profileCache.put(userId, newCacheData)
            Log.d("POST_REPO_DEBUG", "Veriler FIRESTORE'dan çekilip ÖNBELLEĞE yazıldı.")

            Result.success(newCacheData)
        } catch (e: Exception) {
            Log.e("POST_REPO_DEBUG", "Gönderiler çekilirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun dahaFazlaGonderiGetir(userId: String): Result<ProfilePostCacheData> = withContext(Dispatchers.IO) {
        val cachedData = profileCache.get(userId)
            ?: return@withContext Result.failure(Exception("Önbellek bulunamadı!"))

        if (cachedData.isLastPage || cachedData.offset >= cachedData.idList.size) {
            return@withContext Result.success(cachedData)
        }

        try {
            val currentOffset = cachedData.offset
            val nextOffset = (currentOffset + PAGE_SIZE).coerceAtMost(cachedData.idList.size)
            val nextBatch = cachedData.idList.subList(currentOffset, nextOffset)
            val isLast = nextOffset >= cachedData.idList.size

            val newGonderiler = fetchGonderilerByIdsInternal(nextBatch)
            val updatedPosts = cachedData.posts + newGonderiler

            val updatedCache = ProfilePostCacheData(
                posts = updatedPosts,
                idList = cachedData.idList,
                offset = nextOffset,
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
            val tarihMap = items.associate { it.kediID to it.tarih }
            val kediIds = items.map { it.kediID }.distinct()
            val chunks = kediIds.chunked(30)

            val gonderiler = coroutineScope {
                chunks.map { chunk ->
                    async {
                        val snapshot = catsCollection
                            .whereIn(FieldPath.documentId(), chunk)
                            .get()
                            .await()

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
                }.awaitAll().flatten()
            }

            val sortedList = gonderiler.sortedByDescending { it.tarih }
            Result.success(sortedList)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi detayları çekilirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun kullaniciGonderiSil(userId: String, kediId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val snapshot = usersCollection.document(userId).get().await()
            val rawList = snapshot.get("GonderilenKediler") as? List<Map<String, Any>> ?: emptyList()
            val silinecek = rawList.firstOrNull { it["kediID"] == kediId }
                ?: throw Exception("Silinecek gönderi bulunamadı.")

            usersCollection.document(userId)
                .update("GonderilenKediler", FieldValue.arrayRemove(silinecek))
                .await()

            removePostFromCacheInternal(userId, kediId)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun haritadanKediSil(kediId: String): Result<Unit> = runCatching {
        catsCollection.document(kediId).delete().await()
    }

    suspend fun kullaniciGonderiKaydet(userId: String, kediId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val yeniKedi = mapOf(
                "kediID" to kediId,
                "tarih" to Timestamp.now()
            )
            usersCollection.document(userId)
                .update("GonderilenKediler", FieldValue.arrayUnion(yeniKedi))
                .await()

            addPostToCacheInternal(userId, kediId)

            Result.success(Unit)
        } catch (e: Exception) {
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
        val firstBatchIds = updatedIdList.take(PAGE_SIZE)

        val yeniListe = fetchGonderilerByIdsInternal(firstBatchIds)
        val isLast = firstBatchIds.size >= updatedIdList.size

        profileCache.put(
            userId,
            ProfilePostCacheData(yeniListe, updatedIdList, firstBatchIds.size, isLast)
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