package com.beem.catmap.data.repository

import android.net.Uri
import android.util.Log
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.model.SpotOperationResult
import com.beem.catmap.data.model.SpotReport
import com.beem.catmap.data.model.SpotState
import com.beem.catmap.utils.CatLogger
import com.beem.catmap.utils.cache.CacheEntry
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.GeoPoint
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageException
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.time.Duration.Companion.milliseconds

class FeedingSpotRepository {

    companion object {
        @Volatile
        private var INSTANCE: FeedingSpotRepository? = null

        fun getInstance(): FeedingSpotRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: FeedingSpotRepository().also {
                    INSTANCE = it
                }
            }
        }
    }

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance() // 🚀 YENİ: Storage Eklendi
    private val spotsCollection = db.collection("feedingSpots")

    private val geohashCache = ConcurrentHashMap<String, CacheEntry<List<FeedingSpot>>>()
    private val CACHE_EXPIRATION_MS = 5 * 60 * 1000L

    // 🚀 ZIRH: Kullanıcı bilgileri sabit "val" ile değil, fonksiyonla anlık çekilir.
    // Böylece hesap değişimlerinde eski veri (Stale Data) kalması engellenir.
    fun getCurrentUserId(): String = UserSession.userModel.id
    fun getCurrentUserName(): String = UserSession.userModel.name


    /**
     * 📸 YENİ: Fotoğrafı Storage'a yükler ve indirme linkini (URL) döndürür
     */
    suspend fun uploadReportPhoto(storageRef: StorageReference, uri: Uri): Result<String> {
        return try {
            val uploadTask = storageRef.putFile(uri)

            suspendCancellableCoroutine { continuation ->
                continuation.invokeOnCancellation {
                    Log.w("SPOT_DEBUG2", "Coroutine iptal edildi -> Firebase UploadTask zorla durduruluyor!")
                    uploadTask.cancel()
                }

                uploadTask.addOnSuccessListener {
                    continuation.resume(Unit)
                }.addOnFailureListener {
                    continuation.resumeWithException(it)
                }
            }

            val downloadUrl = storageRef.putFile(uri).await().storage.downloadUrl.await().toString()
            Result.success(downloadUrl)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            CatLogger.logError("FeedingSpotRepo", "uploadReportPhoto", e)
            Result.failure(e)
        }
    }


    suspend fun getReportsForSpot(spotId: String): Result<List<SpotReport>> {
        return try {
            val snapshot = spotsCollection.document(spotId)
                .collection("reports")
                .orderBy("reportedAt", Query.Direction.DESCENDING)
                .limit(5)
                .get()
                .await()

            val reports = snapshot.documents.mapNotNull { doc ->
                doc.toObject(SpotReport::class.java)
            }
            Result.success(reports)
        } catch (e: Exception) {
            CatLogger.logError("FeedingSpotRepo", "getReportsForSpot", e)
            Result.failure(e)
        }
    }


    /**
     * 🗺️ COĞRAFİ SORGULAMA (GEOQUERY) İLE HARİTAYI YÜKLE
     * Sadece kullanıcının belirli bir yarıçapındaki (örn: 10km) aktif ve güvenli noktaları getirir.
     */
    suspend fun getActiveSpotsNearLocation(
        lat: Double,
        lng: Double,
        radiusInMeters: Double = 10000.0 // Varsayılan 10 Kilometre Çap
    ): Result<List<FeedingSpot>> {
        return try {
            val center = GeoLocation(lat, lng)
            val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters)


            val tasks: MutableList<Task<QuerySnapshot>> = ArrayList()
            val cachedSpots = mutableListOf<FeedingSpot>()
            val queriedBounds = mutableListOf<String>()

            // 2. Her bir kare için Firebase'e eşzamanlı sorgu at
            for (b in bounds) {
                val boundKey = "${b.startHash}-${b.endHash}"
                val cacheFeedingSpot = geoHashCache(boundKey)

                if (cacheFeedingSpot != null) {
                    cachedSpots.addAll(cacheFeedingSpot)
                } else {
                    val q = spotsCollection
                        .orderBy("geohash")
                        .startAt(b.startHash)
                        .endAt(b.endHash)
                    tasks.add(q.get())
                    queriedBounds.add(boundKey)
                }
            }

            val fetchedSpotsFromDb = mutableListOf<FeedingSpot>()

            if (tasks.isNotEmpty()) {
                val snapshots = Tasks.whenAllSuccess<QuerySnapshot>(tasks).await()

                snapshots.forEachIndexed { index, snap ->
                    val boundSpots = snap.documents.mapNotNull { doc ->
                        doc.toObject(FeedingSpot::class.java)
                    }

                    // İlgili aralığı Cache'e yaz
                    val boundKey = queriedBounds[index]
                    setGeoHashCache(boundKey, boundSpots)

                    fetchedSpotsFromDb.addAll(boundSpots)
                }
            }

            val allRawSpots = cachedSpots + fetchedSpotsFromDb
            val matchingSpots = mutableListOf<FeedingSpot>()

            // 4. Noktaları çembere (Mesafe ve İş Mantığına) göre filtrele
            for (spot in allRawSpots) {
                if (!spot.isActive || spot.reportAsFakeCount >= 3) continue

                val coords = spot.coordinates ?: continue
                val docLocation = GeoLocation(coords.latitude, coords.longitude)
                val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)

                if (distanceInM <= radiusInMeters) {
                    matchingSpots.add(spot)
                }
            }

            Result.success(matchingSpots.distinctBy { it.id })
        } catch (e: Exception) {
            CatLogger.logError("FeedingSpotRepo", "getActiveSpotsNearLocation", e)
            Result.failure(e)
        }
    }

    private fun geoHashCache(hashKey: String): List<FeedingSpot>? {
        val cacheEntry = geohashCache[hashKey]
        val currentTime = System.currentTimeMillis()

        if (cacheEntry != null && (currentTime - cacheEntry.timestamp) < CACHE_EXPIRATION_MS) {
            Log.d("SpotRepo", "Veri RAM den geldi.")
            return cacheEntry.data
        } else {
            Log.d("SpotRepo", "Veri RAM de yok veya süresi doldu. Veri var mı: ${cacheEntry != null}.")
            return null
        }
    }

    private fun setGeoHashCache(hashKey: String, spots: List<FeedingSpot>) {
        Log.d("SpotRepo", "Veri RAM'e yazıldı.")
        val currentTime = System.currentTimeMillis()
        geohashCache[hashKey] = CacheEntry(spots, currentTime)
    }

    /**
     * 🚀 TEK NOKTADAN YÖNETİM (SRP): Yeni Ekleme ve Güncelleme İşlemlerinin Tamamı
     */
    suspend fun submitSpotOperation(
        isCreateMode: Boolean,
        existingSpotId: String,
        lat: Double,
        lng: Double,
        userLat: Double,
        userLng: Double,
        spotName: String,
        city: String,
        district: String,
        neighborhood: String,
        photoUri: Uri,
        reportTags: List<String>,
        note: String,
        newStatus: SpotState
    ): Result<SpotOperationResult> {
        var uploadedPhotoRef: StorageReference? = null

        return try {
            val batch = db.batch()
            val userId = getCurrentUserId()
            val userName = getCurrentUserName()

            // 1. Döküman Referansını ve ID'yi Belirle
            val spotRef = if (isCreateMode) spotsCollection.document() else spotsCollection.document(existingSpotId)
            val finalSpotId = spotRef.id


            val storageRef = storage.reference.child("reports/$finalSpotId/${UUID.randomUUID()}.jpg")
            uploadedPhotoRef = storageRef

            val photoResult = uploadReportPhoto(storageRef = storageRef, photoUri)

            if (photoResult.isFailure) throw Exception("Fotoğraf yüklenemedi.")
            val photoUrl = photoResult.getOrThrow()


            // 3. Rapor (Report) Objesini Oluştur ve Batch'e Ekle

            currentCoroutineContext().ensureActive()

            val reportRef = spotRef.collection("reports").document()
            val report = SpotReport(
                id = reportRef.id,
                reporterId = userId,
                reporterName = userName,
                reportedAt = System.currentTimeMillis(),
                reportTags = reportTags,
                photoUrl = photoUrl,
                note = note,
                reporterLat = userLat,
                reporterLng = userLng
            )
            batch.set(reportRef, report)

            // 4. Noktayı (Spot) Duruma Göre İşle ve Batch'e Ekle
            val finalSpot: FeedingSpot
            if (isCreateMode) {
                val generatedGeohash = GeoFireUtils.getGeoHashForLocation(GeoLocation(lat, lng))
                val newSpot = FeedingSpot(
                    id = finalSpotId,
                    spotName = spotName,
                    city = city,
                    district = district,
                    neighborhood = neighborhood,
                    coordinates = GeoPoint(lat, lng),
                    geohash = generatedGeohash,
                    currentStatus = newStatus.name,
                    creatorId = userId,
                    lastUpdatedBy = userId,
                    lastUpdatedAt = System.currentTimeMillis()
                )
                finalSpot = newSpot
                batch.set(spotRef, newSpot)
            } else {
                val spotUpdates = mapOf(
                    "currentStatus" to newStatus.name,
                    "lastUpdatedAt" to System.currentTimeMillis(),
                    "lastUpdatedBy" to userId
                )
                batch.update(spotRef, spotUpdates)

                finalSpot = FeedingSpot(
                    id = finalSpotId,
                    spotName = spotName,
                    coordinates = GeoPoint(lat, lng),
                    currentStatus = newStatus.name
                )
            }

            currentCoroutineContext().ensureActive()

            batch.commit().await()

            updateOrAddToCache(finalSpot)

            Result.success(SpotOperationResult(finalSpot, report))
        } catch (e: CancellationException) {
            uploadedPhotoRef?.let { ref ->
                withContext(NonCancellable) {
                    try {
                        delay(300.milliseconds)
                        ref.delete().await()
                        Log.d("SPOT_DEBUG", "✅ Tamamlanmış öksüz dosya Storage'dan silindi: ${ref.name}")
                    } catch (delEx: StorageException) {
                        if (delEx.errorCode == StorageException.ERROR_OBJECT_NOT_FOUND) {
                            Log.d("SPOT_DEBUG", "✅ Dosya sunucuya hiç yazılmadan UploadTask başarıyla iptal edildi.")
                        }
                    } catch (otherEx: Exception) {
                        Log.w("SPOT_DEBUG", "Silme hatası: ${otherEx.message}")
                    }
                }
            }
            throw e
        } catch (e: Exception) {
            CatLogger.logError("FeedingSpotRepo", "submitSpotOperation", e)
            Result.failure(e)
        }
    }

    private fun updateOrAddToCache(spot: FeedingSpot) {
        val coords = spot.coordinates ?: return
        val spotHash = spot.geohash.takeIf { it.isNotEmpty() }
            ?: GeoFireUtils.getGeoHashForLocation(GeoLocation(coords.latitude, coords.longitude))

        // Bellekteki her bir aralığı kontrol et
        geohashCache.forEach { (boundKey, cacheEntry) ->
            val parts = boundKey.split("-")
            if (parts.size == 2) {
                val startHash = parts[0]
                val endHash = parts[1]

                // Eğer noktanın hash'i bu sorgu aralığının içindeyse
                if (spotHash in startHash..endHash) {
                    val currentList = cacheEntry.data.toMutableList()
                    val existingIndex = currentList.indexOfFirst { it.id == spot.id }

                    if (existingIndex != -1) {
                        // Güncelleme: Eski kaydı yenisiyle değiştir
                        currentList[existingIndex] = spot
                    } else {
                        // Yeni Ekleme: Listeye dahil et
                        currentList.add(spot)
                    }

                    // Cache girdisini güncelle (zaman damgasını koruyabilir veya tazeleyebilirsiniz)
                    geohashCache[boundKey] = CacheEntry(currentList, cacheEntry.timestamp)
                    Log.d("SpotRepo", "Nokta (${spot.id}) ilgili cache aralığına ($boundKey) işlendi.")
                }
            }
        }
    }

    /**
     * 🛡️ TROLL İHBARI (UPDATE)
     * Noktanın sahte olduğunu düşünen biri butona bastığında çalışır.
     */
    suspend fun reportSpotAsFake(spotId: String): Result<Unit> {
        return try {
            val spotRef = spotsCollection.document(spotId)

            db.runTransaction { transaction ->
                val snapshot = transaction.get(spotRef)
                if (snapshot.exists()) {
                    val currentCount = snapshot.getLong("reportAsFakeCount") ?: 0L
                    transaction.update(spotRef, "reportAsFakeCount", currentCount + 1)

                    if (currentCount + 1 >= 3) {
                        transaction.update(spotRef, "isActive", false)
                    }
                }
            }.await()
            Result.success(Unit)
        } catch (e: Exception) {
            CatLogger.logError("FeedingSpotRepo", "reportSpotAsFake", e)
            Result.failure(e)
        }
    }
}