package com.beem.catmap.Maps.markersclick.comments

import com.beem.catmap.YorumYanit.Yorum_Model
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class CommentsRepo {
    private val db = FirebaseFirestore.getInstance()

    suspend fun getInitialComments(
        catId: String,
        limit: Long
    ): Pair<List<Yorum_Model>, DocumentSnapshot?> {
        return try {
            val querySnapshot = db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .orderBy("zaman", Query.Direction.DESCENDING)
                .limit(limit)
                .get()
                .await()

            val comments = querySnapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val username = doc.getString("kullanici_adi") ?: ""
                val content = doc.getString("icerik") ?: ""
                val uploaderId = doc.getString("Yukleyen_ID") ?: ""
                val date = doc.getDate("zaman")
                val begeniSayisi = doc.getLong("begeniSayisi")?.toInt() ?: 0
                val yanitSayisi = doc.getLong("yanitSayisi")?.toInt() ?: 0


                Yorum_Model(id, username, content, date, null, uploaderId, false).apply {
                    this.begeniSayisi = begeniSayisi
                    this.toplamYanitSayisi = yanitSayisi
                }

            }

            val lastDoc = querySnapshot.documents.lastOrNull()
            Pair(comments, lastDoc)
        } catch (e: Exception) {
            Pair(emptyList(), null)
        }
    }


    suspend fun loadMoreComments(
        catId: String,
        lastDoc: DocumentSnapshot,
        limit: Long
    ): Pair<List<Yorum_Model>, DocumentSnapshot?> {
        return try {
            val querySnapshot = db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .orderBy("zaman", Query.Direction.DESCENDING)
                .startAfter(lastDoc)
                .limit(limit)
                .get()
                .await()

            val comments = querySnapshot.documents.mapNotNull { doc ->
                val id = doc.id
                val username = doc.getString("kullanici_adi") ?: ""
                val content = doc.getString("icerik") ?: ""
                val uploaderId = doc.getString("Yukleyen_ID") ?: ""
                val date = doc.getDate("zaman")
                val begeniSayisi = doc.getLong("begeniSayisi")?.toInt() ?: 0
                val yanitSayisi = doc.getLong("yanitSayisi")?.toInt() ?: 0

                Yorum_Model(id, username, content, date, null, uploaderId, false).apply {
                    this.begeniSayisi = begeniSayisi
                    this.toplamYanitSayisi = yanitSayisi
                }
            }

            val newLastDoc = querySnapshot.documents.lastOrNull()
            Pair(comments, newLastDoc)
        } catch (e: Exception) {
            Pair(emptyList(), null)
        }
    }

    suspend fun addComment(catId: String, content: String, username: String, userId: String): String? {
        return try {
            val commentData = hashMapOf(
                "icerik" to content,
                "zaman" to FieldValue.serverTimestamp(),
                "kullanici_adi" to username,
                "Yukleyen_ID" to userId,
                "yanitSayisi" to 0
            )
            val documentRef = db.collection("cats").document(catId).collection("yorumlar").add(commentData).await()
            documentRef.id
        } catch (e: Exception) {
            null
        }
    }


    fun yorumBegen(catId: String, yorumId: String, kullaniciId: String): Task<Void> {
        val batch = db.batch()

        val begenenRef = db.collection("cats").document(catId)
            .collection("yorumlar").document(yorumId)
            .collection("begenenler").document(kullaniciId)
        batch.set(begenenRef, hashMapOf<String, Any>())

        val yorumRef = db.collection("cats").document(catId)
            .collection("yorumlar").document(yorumId)
        batch.update(yorumRef, "begeniSayisi", FieldValue.increment(1))

        return batch.commit()
    }



    fun yorumBegeniKaldir(catId: String, yorumId: String, kullaniciId: String): Task<Void> {
        val batch = db.batch()

        val begenenRef = db.collection("cats").document(catId)
            .collection("yorumlar").document(yorumId)
            .collection("begenenler").document(kullaniciId)
        batch.delete(begenenRef)

        val yorumRef = db.collection("cats").document(catId)
            .collection("yorumlar").document(yorumId)
        batch.update(yorumRef, "begeniSayisi", FieldValue.increment(-1))

        return batch.commit()
    }

    suspend fun deleteComment(catId: String, yorumId: String): Boolean {
        return try {
            val repliesSnapshot = db.collection("cats").document(catId)
                .collection("yorumlar").document(yorumId)
                .collection("yanitlar").get().await()

            for (doc in repliesSnapshot.documents) {
                doc.reference.delete().await()
            }

            db.collection("cats").document(catId)
                .collection("yorumlar").document(yorumId)
                .delete()
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun updateCommentContent(catId: String, yorumId: String, yeniIcerik: String): Boolean {
        return try {
            db.collection("cats").document(catId)
                .collection("yorumlar").document(yorumId)
                .update("icerik", yeniIcerik)
                .await()
            true
        } catch (e: Exception) {
            false
        }
    }
    suspend fun getCommentCount(catId: String): Int {
        return try {
            if (catId.isEmpty()) return 0
            val querySnapshot = db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .get()
                .await()
            querySnapshot.size()
        } catch (e: Exception) {
            0
        }
    }
}