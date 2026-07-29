package com.beem.catmap.data.repository

import com.beem.catmap.Profil.Gonderiler.CacheHelperGonderiBegeni
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CatRepository {
    private val db = FirebaseFirestore.getInstance()

    suspend fun addLike(userId: String, catId: String): Boolean {
        return try {
            val userRef = db.collection("users").document(userId)
            val catRef = db.collection("cats").document(catId)

            userRef.update("begendigiGonderiler", FieldValue.arrayUnion(catId)).await()
            catRef.update("begeniSayisi", FieldValue.increment(1)).await()
            CacheHelperGonderiBegeni.getInstance().begen(catId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun removeLike(userId: String, catId: String): Boolean {
        return try {
            val userRef = db.collection("users").document(userId)
            val catRef = db.collection("cats").document(catId)

            userRef.update("begendigiGonderiler", FieldValue.arrayRemove(catId)).await()
            catRef.update("begeniSayisi", FieldValue.increment(-1)).await()
            CacheHelperGonderiBegeni.getInstance().begeniKaldir(catId)
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun getCatLikeCount(catId: String): Long {
        return try {
            val doc = db.collection("cats").document(catId).get().await()
            doc.getLong("begeniSayisi") ?: 0L
        } catch (e: Exception) {
            0L
        }
    }
    suspend fun getUserInfo(userId: String): Map<String, Any?>? {
        return try {
            val snapshot = db.collection("users").document(userId).get().await()
            if (snapshot.exists()) snapshot.data else null
        } catch (e: Exception) {
            null
        }
    }

    suspend fun removeCatFromUserPosts(userId: String, catId: String): Boolean {
        return try {
            val userRef = db.collection("users").document(userId)
            val snapshot = userRef.get().await()

            if (snapshot.exists()) {
                val gonderilenKediler = snapshot.get("GonderilenKediler") as? List<Map<String, Any>>

                val silinecekKedi = gonderilenKediler?.firstOrNull { item ->
                    catId == item["kediID"]
                }

                if (silinecekKedi != null) {
                    userRef.update("GonderilenKediler", FieldValue.arrayRemove(silinecekKedi)).await()
                    return true
                }
            }
            false
        } catch (e: Exception) {
            false
        }
    }

    suspend fun deleteCatFromMap(catId: String): Boolean {
        return try {
            db.collection("cats").document(catId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}