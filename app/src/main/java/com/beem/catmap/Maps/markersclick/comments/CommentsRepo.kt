package com.beem.catmap.Maps.markersclick.comments

import com.beem.catmap.YorumYanit.Yorum_Model
import kotlin.collections.emptyList
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class CommentsRepo {
    private val db = FirebaseFirestore.getInstance()


    fun getCommentsRealtime(catId: String, limit: Long): Flow<List<Yorum_Model>> = callbackFlow {
        val query = db.collection("cats")
            .document(catId)
            .collection("yorumlar")
            .orderBy("zaman", Query.Direction.DESCENDING)
            .limit(limit)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            val commentsList = snapshot?.documents?.mapNotNull { doc ->
                val id = doc.id
                val username = doc.getString("kullanici_adi") ?: ""
                val content = doc.getString("icerik") ?: ""
                val uploaderId = doc.getString("Yukleyen_ID") ?: ""
                val date = doc.getDate("zaman")
                Yorum_Model(id, username, content, date, null, uploaderId)
            } ?: emptyList()

            trySend(commentsList)
        }

        awaitClose { listener.remove() }
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
                Yorum_Model(id, username, content, date, null, uploaderId)
            }

            val newLastDoc = querySnapshot.documents.lastOrNull()
            Pair(comments, newLastDoc)
        } catch (e: Exception) {
            Pair(emptyList(), null)
        }
    }


    suspend fun addComment(catId: String, content: String, username: String, userId: String): Boolean {
        return try {
            val commentData = hashMapOf(
                "icerik" to content,
                "zaman" to FieldValue.serverTimestamp(),
                "kullanici_adi" to username,
                "Yukleyen_ID" to userId
            )
            db.collection("cats").document(catId).collection("yorumlar").add(commentData).await()
            true
        } catch (e: Exception) {
            false
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
}