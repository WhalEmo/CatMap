package com.beem.catmap.Maps.markersclick.comments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.YorumYanit.Yorum_Model
import com.beem.catmap.data.repository.UserRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CommentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CommentsRepo()
    private val userRepository = UserRepository.getInstance(application)

    private val _comments = MutableStateFlow<List<Yorum_Model>>(emptyList())
    val comments: StateFlow<List<Yorum_Model>> = _comments.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading = _isLoading.asStateFlow()

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty = _isEmpty.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<Boolean>()
    val actionSuccess = _actionSuccess.asSharedFlow()

    private var catId: String = ""
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var isLastPage = false
    private val pageSize = 10L

    fun initCatId(catId: String) {
        this.catId = catId
        listenComments()
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

    private fun listenComments() {
        if (catId.isEmpty()) return

        viewModelScope.launch {
            repository.getCommentsRealtime(catId, pageSize).collect { list ->
                _comments.value = list
                _isEmpty.value = list.isEmpty()
            }
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
            if (success) _actionSuccess.emit(true)
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
            onComplete(replyId)
        }
    }
}