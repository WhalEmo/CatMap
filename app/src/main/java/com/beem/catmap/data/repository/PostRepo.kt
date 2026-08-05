package com.beem.catmap.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import kotlin.math.max

class PostRepository private constructor(context: Context) {

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: PostRepository? = null

        fun getInstance(context: Context): PostRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PostRepository(context.applicationContext).also {
                    INSTANCE = it
                }
            }
        }
        private const val PAGE_SIZE = 10L
    }

    private val db = FirebaseFirestore.getInstance()
    private val usersCollection = db.collection("users")
    private val catsCollection = db.collection("cats")
    private val userManager = CurrentUserManager.getInstance(context)

    data class PostPageResult(
        val posts: List<Gonderi>,
        val lastDocument: DocumentSnapshot?,
        val isLastPage: Boolean
    )

    suspend fun getKullaniciGonderileri(
        userId: String,
        lastDocument: DocumentSnapshot? = null
    ): Result<PostPageResult> = withContext(Dispatchers.IO) {
        if (userId.isBlank()) return@withContext Result.failure(Exception("Geçersiz UserId"))

        try {
            var query = usersCollection
                .document(userId)
                .collection("GonderilenKediler")
                .orderBy("tarih", Query.Direction.DESCENDING)
                .limit(PAGE_SIZE)

            if (lastDocument != null) {
                query = query.startAfter(lastDocument)
            }

            val snapshot = query.get().await()

            if (snapshot.isEmpty) {
                return@withContext Result.success(
                    PostPageResult(
                        posts = emptyList(),
                        lastDocument = lastDocument,
                        isLastPage = true
                    )
                )
            }

            val newLastDoc = snapshot.documents.lastOrNull()
            val batchItems = snapshot.documents.mapNotNull { doc ->
                val kediID = doc.getString("kediID")
                val tarih = doc.getTimestamp("tarih")
                if (kediID != null) GonderilenKediItem(kediID = kediID, tarih = tarih) else null
            }

            val isLast = snapshot.size() < PAGE_SIZE
            val gonderiler = fetchGonderilerByIdsInternal(batchItems)

            Result.success(
                PostPageResult(
                    posts = gonderiler,
                    lastDocument = newLastDoc,
                    isLastPage = isLast
                )
            )
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderiler çekilirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun kullaniciGonderiSil(
        userId: String,
        kediId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userRef = usersCollection.document(userId)
            val subCollRef = userRef.collection("GonderilenKediler")

            val querySnapshot = subCollRef.whereEqualTo("kediID", kediId).get().await()

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val currentPostCount = userSnap.getLong("gonderiSayisi") ?: 0L

                for (doc in querySnapshot.documents) {
                    transaction.delete(doc.reference)
                }

                if (querySnapshot.documents.isNotEmpty()) {
                    val newCount = max(currentPostCount - 1, 0L)
                    transaction.update(userRef, "gonderiSayisi", newCount)
                }
                null
            }.await()

            if (userId == UserSession.userId) {
                val currentCount = userManager.profileState.value.gonderiSayisi
                userManager.updateGonderiSayisi(max(currentCount - 1, 0L))
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi silinirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun haritadanKediSil(kediId: String): Result<Unit> = runCatching {
        catsCollection.document(kediId).delete().await()
    }

    suspend fun kullaniciGonderiKaydet(
        userId: String,
        kediId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val userRef = usersCollection.document(userId)
            val subCollRef = userRef.collection("GonderilenKediler")

            val yeniKediDoc = subCollRef.document()
            val yeniKediData = mapOf(
                "kediID" to kediId,
                "tarih" to Timestamp.now()
            )

            db.runTransaction { transaction ->
                val userSnap = transaction.get(userRef)
                val currentPostCount = userSnap.getLong("gonderiSayisi") ?: 0L

                transaction.set(yeniKediDoc, yeniKediData)
                transaction.update(userRef, "gonderiSayisi", currentPostCount + 1)
                null
            }.await()

            if (userId == UserSession.userId) {
                val currentCount = userManager.profileState.value.gonderiSayisi
                userManager.updateGonderiSayisi(currentCount + 1)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("PostRepository", "Gönderi kaydedilirken hata: ${e.message}")
            Result.failure(e)
        }
    }

    private suspend fun fetchGonderilerByIdsInternal(items: List<GonderilenKediItem>): List<Gonderi> {
        if (items.isEmpty()) return emptyList()
        val tarihMap = items.associate { it.kediID to it.tarih }
        val kediIds = items.map { it.kediID }.distinct()
        val chunks = kediIds.chunked(30)

        return coroutineScope {
            chunks.map { chunk ->
                async {
                    val snapshot = catsCollection.whereIn(FieldPath.documentId(), chunk).get().await()
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
            }.awaitAll().flatten().sortedByDescending { it.tarih }
        }
    }
}