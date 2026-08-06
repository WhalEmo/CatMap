package com.beem.catmap.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.CatMapApp
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.repository.UserBlockRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserBlockViewModel(
) : ViewModel() {

    private val repository: UserBlockRepository = UserBlockRepository.getInstance()
    private val currentUserManager: CurrentUserManager = CurrentUserManager.getInstance(CatMapApp.instance)
    private val _benimEngellediklerim = MutableStateFlow<List<Kullanici>>(emptyList())
    val benimEngellediklerim: StateFlow<List<Kullanici>> = _benimEngellediklerim.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()

    // Sayfalama için imleç (cursor) ViewModel seviyesinde saklanıyor
    private var lastDocument: DocumentSnapshot? = null

    fun benimEngellediklerimiGetir(currentUserId: String) {
        viewModelScope.launch {
            try {
                _isLastPage.value = false
                lastDocument = null // İlk sayfa çekilirken sıfırla

                val (liste, newLastDoc) = repository.getBlockedUsersPage(
                    kisiId = currentUserId,
                    limit = 20,
                    lastDocumentSnapshot = null
                )

                lastDocument = newLastDoc
                _benimEngellediklerim.value = liste

                if (liste.isEmpty()) {
                    _isLastPage.value = true
                }

                // Cache / Session güncellemesi ViewModel'da yapılıyor
                val idListesi = liste.mapNotNull { it.id }
                currentUserManager.updateBenimEngellediklerim(idListesi)

            } catch (e: Exception) {
                Log.e("EngelVerisi", e.message ?: "")
                _benimEngellediklerim.value = emptyList()
            }
        }
    }

    fun dahaFazlaEngellenenGetir(currentUserId: String) {
        if (_isLoadingMore.value || _isLastPage.value || lastDocument == null) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val (yeniListe, newLastDoc) = repository.getBlockedUsersPage(
                    kisiId = currentUserId,
                    limit = 20,
                    lastDocumentSnapshot = lastDocument
                )

                if (yeniListe.isEmpty()) {
                    _isLastPage.value = true
                } else {
                    lastDocument = newLastDoc
                    _benimEngellediklerim.update { current -> current + yeniListe }

                    // Cache / Session güncellemesi
                    val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
                    val newIds = yeniListe.mapNotNull { it.id }
                    currentIds.addAll(newIds)
                    currentUserManager.updateBenimEngellediklerim(currentIds.distinct())
                }

            } catch (e: Exception) {
                Log.e("EngelVerisi", e.message ?: "")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun engelle(
        engellenecekKullanici: Kullanici,
        kisiId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                val id = engellenecekKullanici.id ?: return@launch
                val ad = engellenecekKullanici.kullaniciAdi ?: ""
                val foto = engellenecekKullanici.fotoUrl ?: ""

                repository.blockUser(kisiId, id, ad, foto)

                _benimEngellediklerim.update { listOf(engellenecekKullanici) + it }

                // Cache Güncelle
                val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
                if (!currentIds.contains(id)) {
                    currentIds.add(0, id)
                    currentUserManager.updateBenimEngellediklerim(currentIds)
                }

                onResult(true, "Engellendi")
            } catch (e: Exception) {
                Log.e("Engelle", e.message ?: "")
                onResult(false, "Engellenemedi")
            }
        }
    }

    fun engelKaldir(
        engellenenKullaniciId: String,
        kisiId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.unblockUser(kisiId, engellenenKullaniciId)

                _benimEngellediklerim.update { current ->
                    current.filterNot { it.id == engellenenKullaniciId }
                }

                val currentIds = currentUserManager.benimEngellediklerimState.value.toMutableList()
                if (currentIds.contains(engellenenKullaniciId)) {
                    currentIds.remove(engellenenKullaniciId)
                    currentUserManager.updateBenimEngellediklerim(currentIds)
                }

                onResult(true, "Engel kaldırıldı")
            } catch (e: Exception) {
                Log.e("Engelle", e.message ?: "")
                onResult(false, "Engel kaldırılamadı")
            }
        }
    }
}