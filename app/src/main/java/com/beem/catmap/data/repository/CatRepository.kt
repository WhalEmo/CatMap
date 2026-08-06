package com.beem.catmap.data.repository

import com.beem.catmap.CatMapApp
import com.beem.catmap.Profil.Gonderiler.CacheHelperGonderiBegeni
import com.beem.catmap.data.session.CurrentUserManager
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class CatRepository {
    private val db = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: CatRepository? = null

        fun getInstance(): CatRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CatRepository().also {
                    INSTANCE = it
                }
            }
        }
    }

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


    suspend fun deleteCatFromMap(catId: String): Boolean {
        return try {
            db.collection("cats").document(catId).delete().await()
            true
        } catch (e: Exception) {
            false
        }
    }
}