package com.beem.catmap.yorumyanit.data.repository

import com.beem.catmap.YorumYanit.Yanit_Model
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class YanitRepository {

    private val db = FirebaseFirestore.getInstance()

    suspend fun yanitlariCek(
        catId: String,
        yorumId: String,
        limit: Int,
        lastVisibleDoc: DocumentSnapshot?
    ): Result<Pair<List<Yanit_Model>, DocumentSnapshot?>> {
        return try {
            val yanitlarRef = db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")

            var query = yanitlarRef
                .orderBy("yanitzaman", Query.Direction.ASCENDING)
                .limit(limit.toLong())

            if (lastVisibleDoc != null) {
                query = query.startAfter(lastVisibleDoc)
            }

            val snapshots = query.get().await()
            val yanitListesi = mutableListOf<Yanit_Model>()

            for (doc in snapshots.documents) {
                val yanit = Yanit_Model(
                    doc.id,
                    doc.getString("kullanici_adi"),
                    doc.getString("yaniticerik"),
                    doc.getDate("yanitzaman"),
                    doc.getString("YanitiYukleyenID"),
                    false,
                    doc.getLong("begeniSayisiYanit")?.toInt() ?: 0,
                    false,
                    false
                )
                yanitListesi.add(yanit)
            }

            val newLastVisible = if (!snapshots.isEmpty) {
                snapshots.documents[snapshots.size() - 1]
            } else {
                lastVisibleDoc
            }

            Result.success(Pair(yanitListesi, newLastVisible))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReply(catId: String, commentId: String, content: String, username: String, userId: String): String? {
        return try {
            val replyData = hashMapOf(
                "yaniticerik" to content,
                "yanitzaman" to FieldValue.serverTimestamp(),
                "kullanici_adi" to username,
                "YanitiYukleyenID" to userId
            )
            val docRef = db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(commentId)
                .collection("yanitlar")
                .add(replyData)
                .await()

            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun yanitSil(catId: String, yorumId: String, yanitId: String): Boolean {
        return try {
            db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .document(yanitId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun yanitGuncelle(catId: String, yorumId: String, yanitId: String, yeniIcerik: String): Boolean {
        return try {
            db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .document(yanitId)
                .update("yaniticerik", yeniIcerik)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun yanitBegen(catId: String, yorumId: String, yanitId: String, kullaniciId: String): Boolean {
        return try {
            db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .document(yanitId)
                .collection("begenenlerYanit")
                .document(kullaniciId)
                .set(emptyMap<String, Any>())
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun yanitBegeniKaldir(catId: String, yorumId: String, yanitId: String, kullaniciId: String): Boolean {
        return try {
            db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .document(yanitId)
                .collection("begenenlerYanit")
                .document(kullaniciId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
}