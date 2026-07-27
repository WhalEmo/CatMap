package com.beem.catmap.Maps.markersclick.comments.replys

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.YorumYanit.Yanit_Model
import com.beem.catmap.data.repository.UserRepository
import com.beem.catmap.yorumyanit.data.repository.YanitRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class YanitViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YanitRepository()
    private val userRepository = UserRepository.getInstance(application)

    private val _yanitlarState = MutableStateFlow<List<Yanit_Model>>(emptyList())
    val yanitlarState: StateFlow<List<Yanit_Model>> = _yanitlarState.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _dahaFazlaGozukuyorMu = MutableStateFlow(false)
    val dahaFazlaGozukuyorMu: StateFlow<Boolean> = _dahaFazlaGozukuyorMu.asStateFlow()

    private val _yanitYokMu = MutableStateFlow(false)
    val yanitYokMu: StateFlow<Boolean> = _yanitYokMu.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<Boolean>()
    val actionSuccess: SharedFlow<Boolean> = _actionSuccess.asSharedFlow()

    private val _hataMesaji = MutableSharedFlow<String>()
    val hataMesaji: SharedFlow<String> = _hataMesaji.asSharedFlow()

    private var catId: String = ""
    private var lastVisibleDoc: DocumentSnapshot? = null
    private val tumYanitlar = mutableListOf<Yanit_Model>()

    fun initCatId(id: String) {
        catId = id
    }

    fun yanitlariYukle(yorumId: String, limit: Int, clearList: Boolean) {
        if (catId.isEmpty() || yorumId.isEmpty()) return

        if (clearList) {
            tumYanitlar.clear()
            lastVisibleDoc = null
            _yanitlarState.value = emptyList()
        }

        _isLoading.value = true

        viewModelScope.launch {
            val result = repository.yanitlariCek(catId, yorumId, limit, lastVisibleDoc)

            result.onSuccess { (yeniYanitlar, yeniLastDoc) ->
                lastVisibleDoc = yeniLastDoc

                if (clearList) {
                    tumYanitlar.clear()
                }

                tumYanitlar.addAll(yeniYanitlar)
                _yanitlarState.value = ArrayList(tumYanitlar)

                _dahaFazlaGozukuyorMu.value = yeniYanitlar.size >= limit
                _yanitYokMu.value = tumYanitlar.isEmpty()
            }.onFailure {
                _yanitYokMu.value = tumYanitlar.isEmpty()
                _hataMesaji.emit("Yanıtlar yüklenirken bir hata oluştu.")
            }

            _isLoading.value = false
        }
    }

    fun sendReply(commentId: String, content: String, onComplete: (String?) -> Unit) {
        if (content.trim().isEmpty() || catId.isEmpty()) {
            onComplete(null)
            return
        }
        val currentUser = userRepository.getCurrentUser() ?: run {
            onComplete(null)
            return
        }

        viewModelScope.launch {
            val replyId = repository.addReply(
                catId = catId,
                commentId = commentId,
                content = content,
                username = currentUser.kullaniciAdi ?: "",
                userId = userRepository.getCurrentUserId() ?: ""
            )
            if (replyId != null) {
                _actionSuccess.emit(true)
                yanitlariYukle(commentId, 10, true)
            } else {
                _hataMesaji.emit("Yanıt gönderilemedi.")
            }
            onComplete(replyId)
        }
    }

    fun deleteYanit(yorumId: String, yanitId: String) {
        if (catId.isEmpty() || yorumId.isEmpty() || yanitId.isEmpty()) return

        viewModelScope.launch {
            val success = repository.yanitSil(catId, yorumId, yanitId)
            if (success) {
                val currentList = _yanitlarState.value.toMutableList()
                currentList.removeAll { it.yanitId == yanitId }
                _yanitlarState.value = currentList
            } else {
                _hataMesaji.emit("Yanıt silinemedi.")
            }
        }
    }
/*
    fun updateYanit(yorumId: String, yanitId: String, yeniIcerik: String) {
        if (catId.isEmpty() || yorumId.isEmpty() || yanitId.isEmpty() || yeniIcerik.trim()
                .isEmpty()
        ) return

        viewModelScope.launch {
            val success = yanitRepository.yanitGuncelle(catId, yorumId, yanitId, yeniIcerik)
            if (success) {
                // 1. Önbellekteki (cache) ilgili yanıtın içeriğini güncelliyoruz
                val liste = yanitCacheMap[yorumId]
                liste?.forEach { yanit ->
                    if (yanit.yanitId == yanitId) {
                        yanit.yaniticerik = yeniIcerik
                    }
                }

                // 2. Güncel listeyi alıp UI'ı tetikliyoruz
                val guncelYanitlar = liste ?: emptyList()
                val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false
                updateCommentYanitState(yorumId, guncelYanitlar, true, dahaFazla)
            } else {
                _hataMesaji.emit("Yanıt güncellenemedi.")
            }
        }
    }

 */
}