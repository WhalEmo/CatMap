package com.beem.catmap.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.model.UserModel

import com.beem.catmap.data.repository.UserBlockRepository
import com.beem.catmap.ui.profile.block.BlockActionState
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
class UserBlockViewModel : ViewModel() {
    private var targetUserId: String? = null
    private val repository: UserBlockRepository = UserBlockRepository.getInstance()

    private val _benimEngellediklerim = MutableStateFlow<List<UserModel>>(emptyList())
    val benimEngellediklerim: StateFlow<List<UserModel>> = _benimEngellediklerim.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isLoadingMore = MutableStateFlow(false)
    val isLoadingMore: StateFlow<Boolean> = _isLoadingMore.asStateFlow()

    private val _isLastPage = MutableStateFlow(false)
    val isLastPage: StateFlow<Boolean> = _isLastPage.asStateFlow()

    private val _blockActionState = MutableSharedFlow<BlockActionState>()
    val blockActionState: SharedFlow<BlockActionState> = _blockActionState.asSharedFlow()
    private var lastDocument: DocumentSnapshot? = null

    fun benimEngellediklerimiGetir(currentUserId: String) {
        this.targetUserId = currentUserId
        viewModelScope.launch {
            _isLoading.value = true
            try {
                _isLastPage.value = false
                lastDocument = null

                val (liste, newLastDoc) = repository.getInitialBlockedUsers(
                    userId = currentUserId,
                    limit = 20
                )

                lastDocument = newLastDoc
                _benimEngellediklerim.value = liste

                if (liste.isEmpty() || newLastDoc == null) {
                    _isLastPage.value = true
                }

            } catch (e: Exception) {
                Log.e("UserBlockViewModel", "Engellenenler getirilemedi: ${e.message}")
                _benimEngellediklerim.value = emptyList()
                _isLastPage.value = true
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun dahaFazlaEngellenenGetir(currentUserId: String) {
        if (_isLoadingMore.value || _isLastPage.value || lastDocument == null) return
        viewModelScope.launch {
            _isLoadingMore.value = true

            try {
                val (yeniListe, newLastDoc) = repository.getBlockedUsersPageFromNetwork(
                    userId = currentUserId,
                    limit = 20,
                    lastDocumentSnapshot = lastDocument
                )

                if (yeniListe.isEmpty()) {
                    _isLastPage.value = true
                } else {
                    lastDocument = newLastDoc
                    _benimEngellediklerim.update { current ->
                        (current + yeniListe).distinctBy { it.id }
                    }
                }

            } catch (e: Exception) {
                Log.e("UserBlockViewModel", "Daha fazla yükleme hatası: ${e.message}")
            } finally {
                _isLoadingMore.value = false
            }
        }
    }

    fun engelle(
        engellenecekUserModel: UserModel,
        kisiId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _blockActionState.emit(BlockActionState.Loading("İşlem yapılıyor..."))
            try {
                repository.blockUser(kisiId, engellenecekUserModel)

                _benimEngellediklerim.update { current ->
                    val filtered = current.filterNot { it.id == engellenecekUserModel.id }
                    listOf(engellenecekUserModel) + filtered
                }

                _blockActionState.emit(BlockActionState.Success("Engellendi"))
                onResult(true)
            } catch (e: Exception) {
                Log.e("UserBlockViewModel", "Engelleme hatası: ${e.message}")
                _blockActionState.emit(BlockActionState.Error("Engellenemedi"))
                onResult(false)
            }
        }
    }

    fun engelKaldir(
        engellenenKullaniciId: String,
        kisiId: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch {
            _blockActionState.emit(BlockActionState.Loading("İşlem yapılıyor..."))
            try {
                repository.unblockUser(kisiId, engellenenKullaniciId)

                _benimEngellediklerim.update { current ->
                    val updated = current.filterNot { it.id == engellenenKullaniciId }
                    if (updated.isEmpty()) {
                        _isLastPage.value = true
                    }
                    updated
                }

                _blockActionState.emit(BlockActionState.Success("Engel kaldırıldı"))
                onResult(true)
            } catch (e: Exception) {
                Log.e("UserBlockViewModel", "Engel kaldırma hatası: ${e.message}")
                _blockActionState.emit(BlockActionState.Error("Engel kaldırılamadı"))
                onResult(false)
            }
        }
    }
}