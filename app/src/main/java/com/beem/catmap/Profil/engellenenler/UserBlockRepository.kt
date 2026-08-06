package com.beem.catmap.data.repository

import com.beem.catmap.KullaniciAuth.Kullanici
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserBlockRepository private constructor() {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    companion object {
        @Volatile
        private var INSTANCE: UserBlockRepository? = null

        fun getInstance(): UserBlockRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: UserBlockRepository().also { INSTANCE = it }
            }
        }
    }

    suspend fun getBlockedUsersPage(
        kisiId: String,
        limit: Long = 20,
        lastDocumentSnapshot: DocumentSnapshot? = null
    ): Pair<List<Kullanici>, DocumentSnapshot?> {

        var query = db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .limit(limit)

        if (lastDocumentSnapshot != null) {
            query = query.startAfter(lastDocumentSnapshot)
        }

        val snapshot = query.get().await()
        val newLastDoc = snapshot.documents.lastOrNull()

        val liste = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val kullaniciAdi = doc.getString("kullaniciAdi") ?: ""
            val fotoUrl = doc.getString("fotoUrl") ?: ""

            Kullanici().apply {
                this.id = id
                this.kullaniciAdi = kullaniciAdi
                this.fotoUrl = fotoUrl
            }
        }

        return Pair(liste, newLastDoc)
    }

    suspend fun blockUser(
        kisiId: String,
        engellenecekKullaniciId: String,
        kullaniciAdi: String,
        fotoUrl: String
    ) {
        db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .document(engellenecekKullaniciId)
            .set(
                mapOf(
                    "blockedAt" to FieldValue.serverTimestamp(),
                    "kullaniciAdi" to kullaniciAdi,
                    "fotoUrl" to fotoUrl
                )
            )
            .await()
    }

    suspend fun unblockUser(
        kisiId: String,
        engeliKaldirilacakId: String
    ) {
        db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .document(engeliKaldirilacakId)
            .delete()
            .await()
    }
}