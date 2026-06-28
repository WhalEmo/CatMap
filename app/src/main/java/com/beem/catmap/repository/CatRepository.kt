package com.beem.catmap.repository

import android.util.Log
import com.beem.catmap.models.CatModel
import com.beem.catmap.models.CommentModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.abs

class CatRepository {

    private val db = FirebaseFirestore.getInstance()
    private val catsCollection = db.collection("cats")

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
}