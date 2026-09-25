package com.beem.catmap.data.repository

import com.beem.catmap.data.local.CacheHelperPostLike
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

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
            CacheHelperPostLike.getInstance().begen(catId)
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
            CacheHelperPostLike.getInstance().begeniKaldir(catId)
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
            if (!snapshot.exists()) return null

            val data = snapshot.data ?: return null
            val isBanned = (data["isBanned"] as? Boolean) ?: (data["banned"] as? Boolean) ?: false

            if (isBanned) {
                data.toMutableMap().apply {
                    this["isBanned"] = true
                    this["KullaniciAdi"] = "Kısıtlanmış Kullanıcı"
                    this["profilFotoUrl"] = ""
                    this["Hakkinda"] = "Bu hesap topluluk kuralları ihlali nedeniyle askıya alınmıştır."
                }
            } else {
                data
            }
        } catch (e: Exception) {
            null
        }
    }
    suspend fun getPublicUserInfo(userId: String): Map<String, Any>? = withContext(Dispatchers.IO) {
        try {
            val snapshot = db.collection("publicUsers")
                .document(userId)
                .get()
                .await()

            if (!snapshot.exists()) return@withContext null

            val data = snapshot.data ?: return@withContext null
            val isBanned = (data["isBanned"] as? Boolean) ?: (data["banned"] as? Boolean) ?: false

            if (isBanned) {
                val maskedData = data.toMutableMap()
                maskedData["isBanned"] = true
                maskedData["KullaniciAdi"] = "Kısıtlanmış Kullanıcı"
                maskedData["profilFotoUrl"] = ""
                maskedData["Hakkinda"] = "Bu hesap topluluk kuralları ihlali nedeniyle askıya alınmıştır."
                maskedData
            } else {
                data
            }
        } catch (e: Exception) {
            null
        }
    }
    suspend fun isCatSent(ownerId: String, catId: String): Boolean {
        return try {
            val querySnapshot = db.collection("users")
                .document(ownerId)
                .collection("GonderilenKediler")
                .whereEqualTo("kediID", catId)
                .get()
                .await()

            !querySnapshot.isEmpty
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