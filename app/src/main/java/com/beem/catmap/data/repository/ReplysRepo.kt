package com.beem.catmap.data.repository

import com.beem.catmap.data.model.ReplyModel
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ReplysRepo {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: ReplysRepo? = null

        fun getInstance(): ReplysRepo {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ReplysRepo().also {
                    INSTANCE = it
                }
            }
        }
    }
    suspend fun loadReplies(
        catId: String,
        yorumId: String,
        limit: Int,
        lastVisibleDoc: DocumentSnapshot?
    ): Result<Pair<List<ReplyModel>, DocumentSnapshot?>> {
        return try {
            val yanitlarRef = db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")

            var query = yanitlarRef
                .orderBy("yanitzaman", Query.Direction.DESCENDING)
                .limit(limit.toLong())

            if (lastVisibleDoc != null) {
                query = query.startAfter(lastVisibleDoc)
            }

            val snapshots = query.get().await()
            val yanitListesi = mutableListOf<ReplyModel>()

            for (doc in snapshots.documents) {
                val yanit = ReplyModel(
                    doc.id,
                    doc.getString("kullanici_adi"),
                    doc.getString("yaniticerik"),
                    doc.getDate("yanitzaman"),
                    doc.getString("YanitiYukleyenID"),
                    doc.getLong("begeniSayisiYanit")?.toInt() ?: 0,
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

            db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(commentId)
                .update("yanitSayisi", FieldValue.increment(1))
                .await()

            docRef.id
        } catch (e: Exception) {
            null
        }
    }

    suspend fun deleteReply(catId: String, yorumId: String, yanitId: String): Boolean {
        return try {
            db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .collection("yanitlar")
                .document(yanitId)
                .delete()
                .await()

            db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)
                .update("yanitSayisi", FieldValue.increment(-1))
                .await()

            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateReply(catId: String, yorumId: String, yanitId: String, yeniIcerik: String): Boolean {
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



     fun yanitBegen(catId: String, yorumId: String, yanitId: String, kullaniciId: String): Task<Void> {
            val batch = db.batch()

            val begenenRef = db.collection("cats").document(catId)
                .collection("yorumlar").document(yorumId)
                .collection("yanitlar").document(yanitId)
                .collection("begenenlerYanit").document(kullaniciId)

            batch.set(begenenRef, hashMapOf<String, Any>())

            val yanitRef = db.collection("cats").document(catId)
                .collection("yorumlar").document(yorumId)
                .collection("yanitlar").document(yanitId)
            batch.update(yanitRef, "begeniSayisiYanit", FieldValue.increment(1))

            return batch.commit()

        }


    fun yanitBegeniKaldir(catId: String, yorumId: String, yanitId: String, kullaniciId: String): Task<Void> {
        val batch = db.batch()

        val begenenRef = db.collection("cats").document(catId)
            .collection("yorumlar").document(yorumId)
            .collection("yanitlar").document(yanitId)
            .collection("begenenlerYanit").document(kullaniciId)

        batch.delete(begenenRef)

        val yanitRef = db.collection("cats").document(catId)
            .collection("yorumlar").document(yorumId)
            .collection("yanitlar").document(yanitId)
        batch.update(yanitRef, "begeniSayisiYanit", FieldValue.increment(-1))

        return batch.commit()

    }
}