package com.beem.catmap.repository

import android.net.Uri
import android.util.Log
import com.beem.catmap.models.CatModel
import com.beem.catmap.models.CommentModel
import com.firebase.geofire.GeoFireUtils
import com.firebase.geofire.GeoLocation
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.abs

class CatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val catsCollection = db.collection("cats")
    private val storage = FirebaseStorage.getInstance()

    /**
     * Coroutines 'await()' kullanarak callback cehennemini yok ettik.
     * Bu fonksiyon sadece veriyi çeker ve UI'a (ViewModel'a) temiz bir List döndürür.
     */
    suspend fun getAllCats(): List<CatModel> {
        return try {
            val snapshot = catsCollection.get().await()
            snapshot.documents.mapNotNull { doc ->
                // Firestore dokümanını doğrudan Kotlin Data Class'ına çeviriyoruz
                val cat = doc.toObject(CatModel::class.java)
                cat?.copy(id = doc.id) // Firestore ID'sini modelin içine enjekte et
            }
        } catch (e: Exception) {
            Log.e("CatRepository", "Kediler çekilirken hata: ${e.message}")
            emptyList()
        }
    }

    /**
     * Kediye yorum ekleme işlemi. Başarılı olursa true, hata olursa false döner.
     */
    suspend fun addComment(catId: String, comment: CommentModel): Boolean {
        return try {
            catsCollection.document(catId)
                .collection("yorumlar")
                .add(comment)
                .await()
            true
        } catch (e: Exception) {
            Log.e("CatRepository", "Yorum eklenirken hata: ${e.message}")
            false
        }
    }

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

    private suspend fun uploadImagesToStorage(imageUris: List<Uri>): List<String> = withContext(Dispatchers.IO) {
        val downloadUrls = mutableListOf<String>()
        imageUris.forEachIndexed { index, uri ->
            val fileName = "fotoklasoru/${System.currentTimeMillis()}_${UUID.randomUUID()}_$index.jpg"
            val storageRef = storage.reference.child(fileName)

            storageRef.putFile(uri).await()

            val downloadUrl = storageRef.downloadUrl.await().toString()
            downloadUrls.add(downloadUrl)
        }
        downloadUrls
    }

    suspend fun uploadCatPost(
        catName: String,
        catAbout: String,
        latitude: Double,
        longitude: Double,
        userId: String,
        imageUris: List<Uri>
    ): String = withContext(Dispatchers.IO) {
        val uploadedPhotoUrls = uploadImagesToStorage(imageUris)
        if (uploadedPhotoUrls.isEmpty()) {
            throw Exception("Fotoğraflar yüklenemedi, URL listesi boş!")
        }

        val hash = GeoFireUtils.getGeoHashForLocation(GeoLocation(latitude, longitude))

        val catData = hashMapOf(
            "kediAdi" to catName,
            "kediHakkinda" to catAbout,
            "latitude" to latitude,
            "longitude" to longitude,
            "geohash" to hash,
            "photoUri" to uploadedPhotoUrls,
            "YukleyenKullaniciID" to userId,
            "createdAt" to System.currentTimeMillis()
        )

        val documentRef = catsCollection.add(catData).await()
        documentRef.id
    }
}