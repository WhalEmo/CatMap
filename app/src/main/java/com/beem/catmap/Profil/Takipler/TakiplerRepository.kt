package com.beem.catmap.Profil.Takipler

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
        lastDocument: DocumentSnapshot? = null
    ): Result<PaginatedResult<Kullanici>> = runCatching {
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

        PaginatedResult(
            items = items,
            lastDocument = snapshot.documents.lastOrNull(),
            isLastPage = items.size < limit
        )
    }

    suspend fun getTakipEdilenler(
        userId: String,
        limit: Long = 10,
        lastDocument: DocumentSnapshot? = null
    ): Result<PaginatedResult<Kullanici>> = runCatching {
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

        PaginatedResult(
            items = items,
            lastDocument = snapshot.documents.lastOrNull(),
            isLastPage = items.size < limit
        )
    }
}