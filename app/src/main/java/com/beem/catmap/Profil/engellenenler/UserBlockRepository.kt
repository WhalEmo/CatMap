package com.beem.catmap.data.repository

import com.beem.catmap.data.session.CurrentUserManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserBlockRepository(
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val currentUserManager: CurrentUserManager
) {

    private var lastDocument: DocumentSnapshot? = null

    /**
     * İlk sayfa engellenen kullanıcıları çeker.
     * Eğer daha önce cache'lendi ise Firestore'a gitmeden cache'deki listeyi döndürebiliriz
     * veya ilk sayfayı tazelemek için Firestore'dan çekip cache'i güncelleyebiliriz.
     */
    suspend fun getBlockedUsersFirstPage(
        kisiId: String,
        limit: Long = 20
    ): List<String> {
        lastDocument = null

        val query = db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .limit(limit)

        val snapshot = query.get().await()
        lastDocument = snapshot.documents.lastOrNull()

        val liste = snapshot.documents.map { it.id }

        // Gelen güncel listeyi CurrentUserManager'a (SharedPreferences + StateFlow) kaydediyoruz
        currentUserManager.updateBenimEngellediklerim(liste)

        return liste
    }

    /**
     * Sonraki sayfa engellenen kullanıcıları çeker (Pagination)
     */
    suspend fun getBlockedUsersNextPage(
        kisiId: String,
        limit: Long = 20
    ): List<String> {
        val last = lastDocument ?: return emptyList()

        val query = db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .startAfter(last)
            .limit(limit)

        val snapshot = query.get().await()
        lastDocument = snapshot.documents.lastOrNull()

        return snapshot.documents.map { it.id }
    }

    suspend fun blockUser(
        kisiId: String,
        engellenecekKullaniciId: String
    ) {
        db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .document(engellenecekKullaniciId)
            .set(
                mapOf(
                    "blockedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()

        // Cache listesini anlık güncelle (StateFlow ve SharedPreferences)
        val currentList = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (!currentList.contains(engellenecekKullaniciId)) {
            currentList.add(0, engellenecekKullaniciId)
            currentUserManager.updateBenimEngellediklerim(currentList)
        }
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

        // Cache listesini anlık güncelle
        val currentList = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (currentList.contains(engeliKaldirilacakId)) {
            currentList.remove(engeliKaldirilacakId)
            currentUserManager.updateBenimEngellediklerim(currentList)
        }
    }
}