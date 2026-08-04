package com.beem.catmap.data.repository

import android.content.Context
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.session.CurrentUserManager
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class UserBlockRepository(context: Context) {

    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val currentUserManager = CurrentUserManager.getInstance(context)
    private var lastDocument: DocumentSnapshot? = null

    suspend fun getBlockedUsersFirstPage(
        kisiId: String,
        limit: Long = 20
    ): List<Kullanici> {
        lastDocument = null

        val query = db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .limit(limit)

        val snapshot = query.get().await()
        lastDocument = snapshot.documents.lastOrNull()

        val liste = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val kullaniciAdi = doc.getString("kullaniciAdi") ?: ""
            val fotoUrl = doc.getString("fotoUrl") ?: ""

            // Kullanici sınıfınızın yapısına göre bu alanları atıyoruz
            Kullanici().apply {
                this.id = id
                this.kullaniciAdi = kullaniciAdi
                this.fotoUrl = fotoUrl
            }
        }

        // Cache için sadece ID listesini saklamaya devam edebilirsiniz
        val idListesi = liste.mapNotNull { it.id }
        currentUserManager.updateBenimEngellediklerim(idListesi)

        return liste
    }

    suspend fun getBlockedUsersNextPage(
        kisiId: String,
        limit: Long = 20
    ): List<Kullanici> {
        val last = lastDocument ?: return emptyList()

        val query = db.collection("users")
            .document(kisiId)
            .collection("blockedUsers")
            .orderBy("blockedAt", Query.Direction.DESCENDING)
            .startAfter(last)
            .limit(limit)

        val snapshot = query.get().await()
        lastDocument = snapshot.documents.lastOrNull()

        val newListe = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val kullaniciAdi = doc.getString("kullaniciAdi") ?: ""
            val fotoUrl = doc.getString("fotoUrl") ?: ""

            Kullanici().apply {
                this.id = id
                this.kullaniciAdi = kullaniciAdi
                this.fotoUrl = fotoUrl
            }
        }

        if (newListe.isNotEmpty()) {
            val currentList = currentUserManager.benimEngellediklerimState.value.toMutableList()
            val newIds = newListe.mapNotNull { it.id }
            currentList.addAll(newIds)
            currentUserManager.updateBenimEngellediklerim(currentList.distinct())
        }

        return newListe
    }

    /**
     * Kullanıcıyı engeller ve ad/pp bilgilerini kaydeder
     */
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

        val currentList = currentUserManager.benimEngellediklerimState.value.toMutableList()
        if (currentList.contains(engeliKaldirilacakId)) {
            currentList.remove(engeliKaldirilacakId)
            currentUserManager.updateBenimEngellediklerim(currentList)
        }
    }
}