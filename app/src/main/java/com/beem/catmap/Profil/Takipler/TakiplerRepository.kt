package com.beem.catmap.Profil.Takipler

import androidx.collection.LruCache
import com.beem.catmap.KullaniciAuth.Kullanici
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

data class PaginatedResult<T>(
    val items: List<T>,
    val lastDocument: DocumentSnapshot?,
    val isLastPage: Boolean = false
)

class TakiplerRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
) {

    // Son 3 kullanıcının verilerini saklamak için LRU Cache (3 kullanıcı x 2 liste = 6 giriş)
    private val memoryCache = LruCache<String, PaginatedResult<Kullanici>>(6)

    suspend fun getTakipciler(
        userId: String,
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null,
        forceRefresh: Boolean = false
    ): Result<PaginatedResult<Kullanici>> = runCatching {

        val cacheKey = "takipciler_$userId"

        // İlk sayfa isteniyorsa ve yenileme zorlanmıyorsa, direkt RAM'den dön
        if (lastDocument == null && !forceRefresh) {
            val cachedData = memoryCache.get(cacheKey)
            if (cachedData != null) {
                return Result.success(cachedData)
            }
        }

        var query = db.collection("users")
            .document(userId)
            .collection("takipciler")
            .limit(limit)

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            Kullanici().apply {
                kullaniciAdi = doc.getString("KullaniciAdi")
                fotoUrl = doc.getString("profilFotoUrl")
                id = doc.getString("ID")
                TakipciMi = 2
            }
        }

        val isLastPage = items.size < limit
        val lastDoc = snapshot.documents.lastOrNull()

        val newResult = PaginatedResult(
            items = items,
            lastDocument = lastDoc,
            isLastPage = isLastPage
        )

        // Cache Güncelleme Mantığı (DÜZELTİLDİ):
        if (lastDocument == null) {
            // İlk sayfa çekildiğinde (ister ilk açılış ister refresh olsun) RAM cache'i taze veriyle güncelle
            memoryCache.put(cacheKey, newResult)
        } else {
            // Sonraki sayfaları çekiyorsak eski listeye ekle
            memoryCache.get(cacheKey)?.let { oldCache ->
                val combinedList = oldCache.items + items
                memoryCache.put(
                    cacheKey,
                    PaginatedResult(combinedList, lastDoc, isLastPage)
                )
            }
        }

        newResult
    }

    suspend fun getTakipEdilenler(
        userId: String,
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null,
        forceRefresh: Boolean = false // <-- EKLENDİ
    ): Result<PaginatedResult<Kullanici>> = runCatching {

        val cacheKey = "takipEdilenler_$userId"

        if (lastDocument == null && !forceRefresh) { // <-- DÜZELTİLDİ
            val cachedData = memoryCache.get(cacheKey)
            if (cachedData != null) {
                return Result.success(cachedData)
            }
        }

        var query = db.collection("users")
            .document(userId)
            .collection("takipEdilenler")
            .limit(limit)

        if (lastDocument != null) {
            query = query.startAfter(lastDocument)
        }

        val snapshot = query.get().await()
        val items = snapshot.documents.mapNotNull { doc ->
            Kullanici().apply {
                kullaniciAdi = doc.getString("KullaniciAdi")
                fotoUrl = doc.getString("profilFotoUrl")
                id = doc.getString("ID")
                TakipEdiyorMuyum = 2
            }
        }

        val isLastPage = items.size < limit
        val lastDoc = snapshot.documents.lastOrNull()

        val newResult = PaginatedResult(
            items = items,
            lastDocument = lastDoc,
            isLastPage = isLastPage
        )

        if (lastDocument == null) { // <-- DÜZELTİLDİ
            memoryCache.put(cacheKey, newResult)
        } else {
            memoryCache.get(cacheKey)?.let { oldCache ->
                val combinedList = oldCache.items + items
                memoryCache.put(
                    cacheKey,
                    PaginatedResult(combinedList, lastDoc, isLastPage)
                )
            }
        }

        newResult
    }
}