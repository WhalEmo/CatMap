package com.beem.catmap.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.UserBlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserBlockViewModel(
    private val repository: UserBlockRepository = UserBlockRepository()
) : ViewModel() {

    private val _benimEngellediklerim = MutableStateFlow<List<String>>(emptyList())
    val benimEngellediklerim: StateFlow<List<String>> = _benimEngellediklerim.asStateFlow()

    private val _beniEngelleyenler = MutableStateFlow<List<String>>(emptyList())
    val beniEngelleyenler: StateFlow<List<String>> = _beniEngelleyenler.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()

    fun engelle(
        engellenecekKullaniciId: String,
        kisiId: String,
        onResult: (Boolean, String) -> Unit
    ) {
        viewModelScope.launch {
            try {
                repository.blockUser(kisiId, engellenecekKullaniciId)

                _benimEngellediklerim.update {
                    listOf(engellenecekKullaniciId) + it
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

                _benimEngellediklerim.update {
                    it.filterNot { id -> id == engellenenKullaniciId }
                }

                onResult(true, "Engel kaldırıldı")
            } catch (e: Exception) {
                Log.e("Engelle", e.message ?: "")
                onResult(false, "Engel kaldırılamadı")
            }
        }
    }

    /**
     * İlk sayfa yükleme
     */
    fun benimEngellediklerimiGetir(currentUserId: String) {
        viewModelScope.launch {
            try {
                _isLastPage.value = false // Yeniden çekildiğinde sayfa durumunu sıfırla
                val liste = repository.getBlockedUsersFirstPage(currentUserId)

                _benimEngellediklerim.value = liste
                if (liste.isEmpty()) {
                    _isLastPage.value = true
                }

            } catch (e: Exception) {
                Log.e("EngelVerisi", e.message ?: "")
                _benimEngellediklerim.value = emptyList()
            }
        }
    }

    /**
     * Sonraki sayfa yükleme (Pagination tetikleyicisi)
     */
    fun dahaFazlaEngellenenGetir(currentUserId: String) {
        if (_isLoadingMore.value || _isLastPage.value) return

        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val yeniListe = repository.getBlockedUsersNextPage(currentUserId)

                if (yeniListe.isEmpty()) {
                    _isLastPage.value = true
                } else {
                    _benimEngellediklerim.update {
                        it + yeniListe
                    }
                }

            } catch (e: Exception) {
                Log.e("EngelVerisi", e.message ?: "")
            }

            _isLoadingMore.value = false
        }
    }
}