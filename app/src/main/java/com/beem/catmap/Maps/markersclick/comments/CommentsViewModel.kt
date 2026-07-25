package com.beem.catmap.Maps.markersclick.comments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.YorumYanit.CacheHelperYorum
import com.beem.catmap.YorumYanit.Yorum_Model
import com.beem.catmap.data.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CommentsRepo()
    private val userRepository = UserRepository.getInstance(application)

    private val _comments = MutableStateFlow<List<Yorum_Model>>(emptyList())
    val comments: StateFlow<List<Yorum_Model>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty = _isEmpty.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<Boolean>()
    val actionSuccess = _actionSuccess.asSharedFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()


    private val _begenilenYorumIDs = MutableStateFlow<Set<String>>(emptySet())
    val begenilenYorumIDs: StateFlow<Set<String>> = _begenilenYorumIDs.asStateFlow()

    private val _hataMesaji = MutableSharedFlow<String>()
    val hataMesaji: SharedFlow<String> = _hataMesaji.asSharedFlow()

    init {
        _begenilenYorumIDs.value = CacheHelperYorum.loadBegenilenSet(getApplication()) ?: emptySet()
    }

    private var catId: String = ""
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var isLastPage = false
    private val pageSize = 10L


    fun toggleBegeni(catId: String?, yorum: Yorum_Model?, kullaniciId: String?) {
        if (catId == null || yorum == null || kullaniciId == null) return

        val yorumId = yorum.yorumID ?: return
        val mevcutBegeniler = _begenilenYorumIDs.value.toMutableSet()
        val durumBegenilmis = mevcutBegeniler.contains(yorumId)

        if (durumBegenilmis) {
            mevcutBegeniler.remove(yorumId)
        } else {
            mevcutBegeniler.add(yorumId)
        }

        _begenilenYorumIDs.value = mevcutBegeniler
        CacheHelperYorum.saveBegenilenSet(getApplication(), mevcutBegeniler)

        // BURASI EKLENDİ: Listeyi güncellerken ilgili yorumun hem beğeni durumunu hem de sayısını artırıp/azaltıyoruz
        val currentList = _comments.value.map { item ->
            if (item.yorumID == yorumId) {
                item.copy().apply {
                    val yeniBegeniSayisi = if (durumBegenilmis) {
                        (item.begeniSayisi - 1).coerceAtLeast(0)
                    } else {
                        item.begeniSayisi + 1
                    }
                    this.begeniSayisi = yeniBegeniSayisi
                    setBegenildiMi(!durumBegenilmis)
                }
            } else {
                item
            }
        }
        _comments.value = currentList

        if (durumBegenilmis) {
            repository.yorumBegeniKaldir(catId, yorumId, kullaniciId)
                .addOnFailureListener {
                    rollbackBegeni(catId, yorum, kullaniciId)
                }
        } else {
            repository.yorumBegen(catId, yorumId, kullaniciId)
                .addOnFailureListener {
                    rollbackBegeni(catId, yorum, kullaniciId)
                }
        }
    }
    private fun rollbackBegeni(catId: String, yorum: Yorum_Model, kullaniciId: String) {
        viewModelScope.launch {
            _hataMesaji.emit("İşlem başarısız oldu, lütfen tekrar deneyin.")
        }
        toggleBegeni(catId, yorum, kullaniciId)
    }
    fun initCatId(catId: String) {
        if (this.catId == catId) return
        this.catId = catId

        fetchInitialComments()
    }

    fun fetchInitialComments() {
        if (catId.isEmpty()) return

        viewModelScope.launch {
           _isLoading.value = true
            isLastPage = false
            lastVisibleDoc = null

            val (initialList, lastDoc) = repository.getInitialComments(catId, pageSize)

            _comments.value = initialList
            lastVisibleDoc = lastDoc
            _isEmpty.value = initialList.isEmpty()
            if (initialList.size < pageSize) {
                isLastPage = true
            }
            _isLoading.value = false
        }
    }

    fun loadMoreComments() {
        if (_isLoading.value || isLastPage || catId.isEmpty()) return

        val currentLastDoc = lastVisibleDoc ?: return

        viewModelScope.launch {
            _isLoading.value = true

            val (newComments, newLastDoc) = repository.loadMoreComments(
                catId = catId,
                lastDoc = currentLastDoc,
                limit = pageSize
            )

            if (newComments.isNotEmpty()) {
                val updatedList = _comments.value.toMutableList().apply {
                    addAll(newComments)
                }
                _comments.value = updatedList
                lastVisibleDoc = newLastDoc

                if (newComments.size < pageSize) {
                    isLastPage = true
                }
            } else {
                isLastPage = true
            }

            _isLoading.value = false
        }
    }

    fun sendComment(content: String) {
        if (content.trim().isEmpty() || catId.isEmpty()) return
        val currentUser = userRepository.getCurrentUser() ?: return

        viewModelScope.launch {
            val success = repository.addComment(
                catId = catId,
                content = content,
                username = currentUser.kullaniciAdi ?: "",
                userId = userRepository.getCurrentUserId() ?: ""
            )
            if (success) {
                _actionSuccess.emit(true)
                fetchInitialComments()
            }
        }
    }

    fun sendReply(commentId: String, content: String, onComplete: (String?) -> Unit) {
        if (content.trim().isEmpty() || catId.isEmpty()) return
        val currentUser = userRepository.getCurrentUser() ?: return

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
            }
            onComplete(replyId)
        }
    }
    fun deleteComment(yorumId: String) {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val success = repository.deleteComment(catId, yorumId)
            if (success) {
                val currentList = _comments.value.toMutableList()
                currentList.removeAll { it.yorumID == yorumId }
                _comments.value = currentList
            } else {
                _hataMesaji.emit("Yorum silinemedi.")
            }
        }
    }

    fun updateComment(yorumId: String, yeniIcerik: String) {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val success = repository.updateCommentContent(catId, yorumId, yeniIcerik)
            if (success) {
                val currentList = _comments.value.map { yorum ->
                    if (yorum.yorumID == yorumId) {
                        yorum.copy().apply {
                            setYorumicerik(yeniIcerik)
                        }
                    } else {
                        yorum
                    }
                }
                _comments.value = currentList
            } else {
                _hataMesaji.emit("Yorum güncellenemedi.")
            }
        }
    }


    fun fetchCommentCount() {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val count = repository.getCommentCount(catId)
            _commentCount.value = count
        }
    }
    fun toggleYanitlarGorunurluk(yorumId: String) {
        val currentList = _comments.value.map { yorum ->
            if (yorum.yorumID == yorumId) {
                yorum.copy().apply {
                    setYanitlarGorunuyor(!isYanitlarGorunuyor)
                }
            } else {
                yorum
            }
        }
        _comments.value = currentList
    }
}