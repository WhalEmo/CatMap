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

class TakiplerRepository private constructor() {

    private val db = FirebaseFirestore.getInstance()

    // 10 farklı kullanıcının takipçi/takip edilen ilk sayfa listesini bellekte tutar
    private val takipcilerCache = LruCache<String, PaginatedResult<Kullanici>>(10)
    private val takipEdilenlerCache = LruCache<String, PaginatedResult<Kullanici>>(10)

    companion object {
        @Volatile
        private var INSTANCE: TakiplerRepository? = null

        fun getInstance(): TakiplerRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: TakiplerRepository().also { INSTANCE = it }
            }
        }
    }

    suspend fun getTakipciler(
        userId: String,
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null,
        forceRefresh: Boolean = false
    ): Result<PaginatedResult<Kullanici>> = runCatching {

        // İlk sayfa isteği yapılıyorsa ve zorla yenileme (Pull-to-refresh) istenmiyorsa Cache'ten ver
        if (lastDocument == null && !forceRefresh) {
            takipcilerCache.get(userId)?.let { cachedResult ->
                return@runCatching cachedResult
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
                id = doc.getString("ID") ?: doc.id
                kullaniciAdi = doc.getString("KullaniciAdi") ?: ""
                fotoUrl = doc.getString("profilFotoUrl") ?: ""
                takipciMi = 2
            }
        }

        val result = PaginatedResult(
            items = items,
            lastDocument = snapshot.documents.lastOrNull(),
            isLastPage = items.size < limit
        )

        // İlk sayfayı Cache'e kaydet
        if (lastDocument == null) {
            takipcilerCache.put(userId, result)
        }

        result
    }

    suspend fun getTakipEdilenler(
        userId: String,
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null,
        forceRefresh: Boolean = false
    ): Result<PaginatedResult<Kullanici>> = runCatching {

        if (lastDocument == null && !forceRefresh) {
            takipEdilenlerCache.get(userId)?.let { cachedResult ->
                return@runCatching cachedResult
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
                kullaniciAdi = doc.getString("KullaniciAdi") ?: ""
                fotoUrl = doc.getString("profilFotoUrl") ?: ""
                id = doc.getString("ID") ?: ""
                takipEdiyorMuyum = 2
            }
        }

        val result = PaginatedResult(
            items = items,
            lastDocument = snapshot.documents.lastOrNull(),
            isLastPage = items.size < limit
        )

        if (lastDocument == null) {
            takipEdilenlerCache.put(userId, result)
        }

        result
    }

    // Kullanıcı birini takip ettiğinde veya takipten çıkardığında cache'i temizlemek için kullanabilirsiniz
    fun clearUserCache(userId: String) {
        takipcilerCache.remove(userId)
        takipEdilenlerCache.remove(userId)
    }
}