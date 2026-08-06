package com.beem.catmap.ui.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.repository.UserBlockRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UserBlockViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: UserBlockRepository = UserBlockRepository(application)
    private val _benimEngellediklerim = MutableStateFlow<List<Kullanici>>(emptyList())
    val benimEngellediklerim: StateFlow<List<Kullanici>> = _benimEngellediklerim.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()

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

                _benimEngellediklerim.update {
                    listOf(engellenecekKullanici) + it
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
                    it.filterNot { kullanici -> kullanici.id == engellenenKullaniciId }
                }

                onResult(true, "Engel kaldırıldı")
            } catch (e: Exception) {
                Log.e("Engelle", e.message ?: "")
                onResult(false, "Engel kaldırılamadı")
            }
        }
    }

    fun benimEngellediklerimiGetir(currentUserId: String) {
        viewModelScope.launch {
            try {
                _isLastPage.value = false
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