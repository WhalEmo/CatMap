package com.beem.catmap.data.repository

import android.content.Context
import android.net.Uri
import android.util.Log
import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import java.util.UUID

class PostRepository {
    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val catsCollection = db.collection("cats")

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

    suspend fun kullaniciGonderiSil(userId: String, kediId: String): Result<Unit> {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            val rawList = snapshot.get("GonderilenKediler") as? List<Map<String, Any>> ?: emptyList()
            val silinecek = rawList.firstOrNull { it["kediID"] == kediId }
                ?: throw Exception("Silinecek gönderi bulunamadı.")

            usersCollection
                .document(userId)
                .update("GonderilenKediler", FieldValue.arrayRemove(silinecek))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi silinirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun haritadanKediSil(kediId: String): Result<Unit> = runCatching {
        catsCollection
            .document(kediId)
            .delete()
            .await()
    }

    suspend fun kullaniciGonderiKaydet(userId: String, kediId: String): Result<Unit> {
        return try {
            val yeniKedi = mapOf(
                "kediID" to kediId,
                "tarih" to Timestamp.now()
            )
            usersCollection
                .document(userId)
                .update("GonderilenKediler", FieldValue.arrayUnion(yeniKedi))
                .await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi kaydedilirken hata: ${e.message}")
            Result.failure(e)
        }
    }
}