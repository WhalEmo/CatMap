package com.beem.catmap.data.repository

import android.net.Uri
import android.util.Log
import com.beem.catmap.models.CatModel
import com.beem.catmap.ui.manager.UploadProgressState
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs

class MapRepository {

    private val db = FirebaseFirestore.getInstance()
    private val catsCollection = db.collection("cats")
    private val storage = FirebaseStorage.getInstance()

    suspend fun getCatsNearLocation(userLat: Double, userLng: Double): List<CatModel> {
        return withContext(Dispatchers.IO) { // İşlemi arka plana (IO) zorla
            try {
                val snapshot = catsCollection.get().await()
                snapshot.documents.mapNotNull { doc ->
                    val lat = doc.getDouble("latitude") ?: 0.0
                    val lng = doc.getDouble("longitude") ?: 0.0
                    if (abs(userLat - lat) <= 0.009 && abs(userLng - lng) <= 0.0113) {
                        val cat = doc.toObject(CatModel::class.java)
                        cat?.copy(id = doc.id)
                    } else {
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e("CatRepository", "Konuma göre kediler çekilirken hata: ${e.message}")
                emptyList()
            }
        }
    }


    fun uploadCatPostWithProgress(
        catName: String,
        catAbout: String,
        latitude: Double,
        longitude: Double,
        userId: String,
        imageUris: List<Uri>
    ): Flow<UploadProgressState> = callbackFlow {

        if (imageUris.isEmpty()) {
            trySend(UploadProgressState.Error(Exception("En az bir kedi fotoğrafı yüklemelisiniz!")))
            close()
            return@callbackFlow
        }

        val uploadedUrls = mutableListOf<String>()
        val totalImages = imageUris.size
        val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))

        // Firebase Storage'a resimleri sırayla yükleyen ve takibini yapan asenkron döngü
        // Bu mantıkla her resim bittiğinde ana yüzdeyi güvenle yukarı tetikleriz
        fun uploadImageAt(index: Int) {
            if (index >= totalImages) {
                // 🚀 TÜM RESİMLER BİTTİ: Şimdi Firestore'a kayıt zamanı
                val catData = hashMapOf(
                    "kediAdi" to catName,
                    "kediHakkinda" to catAbout,
                    "latitude" to latitude,
                    "longitude" to longitude,
                    "geohash" to hash,
                    "photoUri" to uploadedUrls,
                    "YukleyenKullaniciID" to userId,
                    "createdAt" to FieldValue.serverTimestamp()
                )

                catsCollection.add(catData)
                    .addOnSuccessListener { documentRef ->
                        trySend(UploadProgressState.Success(documentRef.id))
                        close()
                    }
                    .addOnFailureListener { e ->
                        trySend(UploadProgressState.Error(e))
                        close()
                    }
                return
            }

            val uri = imageUris[index]
            val fileName = "fotoklasoru/${System.currentTimeMillis()}_${UUID.randomUUID()}_$index.jpg"
            val storageRef = storage.reference.child(fileName)
            val uploadTask = storageRef.putFile(uri)

            uploadTask.addOnProgressListener { snapshot ->
                if (snapshot.totalByteCount > 0) {
                    val progress = (100.0 * snapshot.bytesTransferred / snapshot.totalByteCount).toInt()
                    val globalProgress = ((index * 100) + progress) / totalImages
                    val safeProgress = if (globalProgress >= 100) 99 else globalProgress
                    trySend(UploadProgressState.Loading(safeProgress))
                }
            }.addOnSuccessListener {
                // Resim bitti, indirme URL'ini alalım
                storageRef.downloadUrl.addOnSuccessListener { downloadUrl ->
                    uploadedUrls.add(downloadUrl.toString())
                    // Bir sonraki resmi yüklemeye geç
                    uploadImageAt(index + 1)
                }.addOnFailureListener { e ->
                    trySend(UploadProgressState.Error(e))
                    close()
                }
            }.addOnFailureListener { e ->
                trySend(UploadProgressState.Error(e))
                close()
            }
        }

        // İlk resimden yüklemeyi başlat dayıcım
        uploadImageAt(0)

        // Emniyet kilidi: Akış kırılırsa task'ları durdurmak için
        awaitClose { /* İptal gerekirse */ }
    }


    suspend fun fetchCatsInArea(latitude: Double, longitude: Double, radiusInMeters: Double = 5000.0): List<CatModel> {
        return try {
            val center = GeoLocation(latitude, longitude)

            val bounds = GeoFireUtils.getGeoHashQueryBounds(center, radiusInMeters)

            coroutineScope {
                val deferredQueries = bounds.map { b ->
                    async {
                        catsCollection
                            .orderBy("geohash")
                            .startAt(b.startHash)
                            .endAt(b.endHash)
                            .get()
                            .await()
                    }
                }

                val snapshots = deferredQueries.map { it.await() }
                val tempCatsList = mutableListOf<CatModel>()

                for (snap in snapshots) {
                    for (doc in snap.documents) {
                        val lat = doc.getDouble("latitude") ?: continue
                        val lng = doc.getDouble("longitude") ?: continue

                        val docLocation = GeoLocation(lat, lng)
                        val distanceInM = GeoFireUtils.getDistanceBetween(docLocation, center)

                        if (distanceInM <= radiusInMeters) {
                            val cat = doc.toObject(CatModel::class.java)
                            cat?.let {
                                val finalizedCat = it.copy(id = doc.id)
                                tempCatsList.add(finalizedCat)
                            }
                        }
                    }
                }

                tempCatsList.distinctBy { it.id }
            }
        } catch (e: Exception) {
            Log.e("CatRepository", "Alan taranırken hata oluştu: ${e.message}")
            emptyList()
        }
    }


    suspend fun findCatById(catId: String): CatModel? {
        return try {
            val documentSnapshot = catsCollection.document(catId).get().await()

            if (documentSnapshot.exists()) {
                val cat = documentSnapshot.toObject(CatModel::class.java)

                cat?.copy(id = documentSnapshot.id)
            } else {
                Log.w("CatRepository", "Kedi bulunamadı! ID: $catId")
                null
            }
        } catch (e: Exception) {
            Log.e("CatRepository", "Kedi detayı çekilirken hata (ID: $catId): ${e.message}")
            null
        }
    }

}