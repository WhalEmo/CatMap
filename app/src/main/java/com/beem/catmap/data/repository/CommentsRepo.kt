package com.beem.catmap.data.repository

import com.beem.catmap.data.model.CommentModel
import com.beem.catmap.data.model.UserProfileInfo
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await
import kotlin.collections.forEach

class CommentsRepo {
    private val db = FirebaseFirestore.getInstance()
    companion object {
        @Volatile
        private var INSTANCE: CommentsRepo? = null

        fun getInstance(): CommentsRepo {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CommentsRepo().also {
                    INSTANCE = it
                }
            }
        }
    }
    suspend fun getInitialComments(
        catId: String,
        limit: Long  = 10L
    ): Pair<List<CommentModel>, DocumentSnapshot?> {
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


                CommentModel(id, username, null, content, date, null, uploaderId, false).apply {
                    this.likeCount = begeniSayisi
                    this.sumRepliesCount = yanitSayisi
                }

            }

            val userIds = comments.map { it.loadId }.filter { it.isNotEmpty() }.distinct()

            if (userIds.isNotEmpty()) {
                val usersMap = mutableMapOf<String, UserProfileInfo>()

                userIds.chunked(10).forEach { chunk ->
                    val usersSnapshot = db.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()

                    for (userDoc in usersSnapshot.documents) {
                        val photoUrl = userDoc.getString("profilFotoUrl")
                        val isBanned = userDoc.getBoolean("isBanned") ?: (userDoc.getBoolean("banned") ?: false)

                        usersMap[userDoc.id] = UserProfileInfo(photoUrl, isBanned)
                    }
                }

                // Yorumları güncelle: Banlıysa içeriği ve resmi sınırla
                comments.forEach { comment ->
                    val userInfo = usersMap[comment.loadId]
                    if (userInfo?.isBanned == true) {
                        comment.profileImage = ""
                        comment.commentContent = "Bu kullanıcının hesabı askıya alındığı için içerik kısıtlanmıştır."
                        comment.username = "Kısıtlanmış Kullanıcı"
                    } else {
                        comment.profileImage = userInfo?.photoUrl
                    }
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
    ): Pair<List<CommentModel>, DocumentSnapshot?> {
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

                CommentModel(id, username, null, content, date, null, uploaderId, false).apply {
                    this.likeCount = begeniSayisi
                    this.sumRepliesCount = yanitSayisi
                }
            }

            val userIds = comments.map { it.loadId }.filter { it.isNotEmpty() }.distinct()

            if (userIds.isNotEmpty()) {
                val usersMap = mutableMapOf<String, UserProfileInfo>()

                userIds.chunked(10).forEach { chunk ->
                    val usersSnapshot = db.collection("users")
                        .whereIn(FieldPath.documentId(), chunk)
                        .get()
                        .await()

                    for (userDoc in usersSnapshot.documents) {
                        val photoUrl = userDoc.getString("profilFotoUrl")
                        val isBanned = userDoc.getBoolean("isBanned") ?: (userDoc.getBoolean("banned") ?: false)

                        usersMap[userDoc.id] = UserProfileInfo(photoUrl, isBanned)
                    }
                }

                comments.forEach { comment ->
                    val userInfo = usersMap[comment.loadId]
                    if (userInfo?.isBanned == true) {
                        comment.profileImage = ""
                        comment.commentContent = "Bu kullanıcının hesabı askıya alındığı için içerik kısıtlanmıştır."
                        comment.username = "Kısıtlanmış Kullanıcı"
                    } else {
                        comment.profileImage = userInfo?.photoUrl
                    }
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
            val commentRef = db.collection("cats")
                .document(catId)
                .collection("yorumlar")
                .document(yorumId)

            val repliesSnapshot = commentRef
                .collection("yanitlar")
                .get()
                .await()

            val likesSnapshot = commentRef
                .collection("begenenler")
                .get()
                .await()

            val batch = db.batch()

            for (doc in repliesSnapshot.documents) {
                batch.delete(doc.reference)
            }

            for (doc in likesSnapshot.documents) {
                batch.delete(doc.reference)
            }
            batch.delete(commentRef)
            batch.commit().await()
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