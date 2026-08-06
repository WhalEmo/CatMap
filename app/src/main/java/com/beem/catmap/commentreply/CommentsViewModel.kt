package com.beem.catmap.commentreply
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.CacheHelperComment
import com.beem.catmap.models.ReplyModel
import com.beem.catmap.models.CommentModel
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.data.repository.CommentsRepo
import com.beem.catmap.data.repository.ReplysRepo
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Date

class CommentViewModel(application: Application) : AndroidViewModel(application) {
    private val commentRepository = CommentsRepo.getInstance()
    private val replysRepo = ReplysRepo.getInstance()
    private val currentUserManager = CurrentUserManager.getInstance(application)

    private val _comments = MutableStateFlow<List<CommentModel>>(emptyList())
    val comments: StateFlow<List<CommentModel>> = _comments.asStateFlow()

    private val replyCacheMap = mutableMapOf<String, MutableList<ReplyModel>>()
    private val replyLastDocMap = mutableMapOf<String, DocumentSnapshot?>()
    private val replyHasMoreMap = mutableMapOf<String, Boolean>()
    private val replyLoadedMap = mutableMapOf<String, Boolean>()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty: StateFlow<Boolean> = _isEmpty.asStateFlow()

    private val _isPaginationLoading = MutableStateFlow(false)
    val isPaginationLoading: StateFlow<Boolean> = _isPaginationLoading.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<String>()
    val actionSuccess: SharedFlow<String> = _actionSuccess.asSharedFlow()

    private val _errorMessage = MutableSharedFlow<String>()
    val errorMessage: SharedFlow<String> = _errorMessage.asSharedFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()

    private val _likedCommentIds = MutableStateFlow<Set<String>>(emptySet())
    val likedCommentIds: StateFlow<Set<String>> = _likedCommentIds.asStateFlow()

    private val _likedReplyIds = MutableStateFlow<Set<String>>(emptySet())
    val likedReplyIds: StateFlow<Set<String>> = _likedReplyIds.asStateFlow()
    private var catId: String = ""
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var isLastPage = false
    private val pageSize = 10L
    init {
        _likedCommentIds.value = CacheHelperComment.loadLikedSet(getApplication()) ?: emptySet()
        _likedReplyIds.value = CacheHelperComment.loadLikedReplySet(getApplication()) ?: emptySet()
    }
    fun initCatId(catId: String) {
        if (this.catId == catId) return
        this.catId = catId
        fetchInitialComments()
    }
    private fun applyLikedCommentState(comments: List<CommentModel>): List<CommentModel> {
        val likedSet = _likedCommentIds.value
        comments.forEach { comment ->
            comment.setLiked(comment.commentId != null && likedSet.contains(comment.commentId))
        }
        return comments
    }
    private fun applyLikedReplyState(replies: List<ReplyModel>): List<ReplyModel> {
        val likedSet = _likedReplyIds.value
        replies.forEach { reply ->
            reply.setLiked(reply.replyId != null && likedSet.contains(reply.replyId))
        }
        return replies
    }
    fun fetchInitialComments() {
        if (catId.isEmpty()) return
        _isLoading.value = true
        viewModelScope.launch {
            isLastPage = false
            lastVisibleDoc = null

            val (initialList, lastDoc) = commentRepository.getInitialComments(catId, pageSize)
            val processedList = applyLikedCommentState(initialList)

            _comments.value = processedList
            lastVisibleDoc = lastDoc
            _isEmpty.value = processedList.isEmpty()

            if (processedList.size < pageSize) {
                isLastPage = true
            }
            _isLoading.value = false
        }
    }
    fun loadMoreComments() {
        if (_isLoading.value || _isPaginationLoading.value || isLastPage || catId.isEmpty()) return
        val currentLastDoc = lastVisibleDoc ?: return

        viewModelScope.launch {
            _isPaginationLoading.value = true
            val (newComments, newLastDoc) = commentRepository.loadMoreComments(catId, currentLastDoc, pageSize)

            if (newComments.isNotEmpty()) {
                val processedNewComments = applyLikedCommentState(newComments)
                _comments.update { current ->
                    current.toMutableList().apply { addAll(processedNewComments) }
                }
                lastVisibleDoc = newLastDoc
                if (newComments.size < pageSize) isLastPage = true
            } else {
                isLastPage = true
            }
            _isPaginationLoading.value = false
        }
    }
    fun toggleRepliesVisibility(commentId: String) {
        _comments.update { currentList ->
            val index = currentList.indexOfFirst { it.commentId == commentId }
            if (index == -1) return@update currentList

            val updatedList = currentList.toMutableList()
            val comment = updatedList[index].copy()

            val newVisibilityState = !comment.isAreRepliesVisible

            if (!newVisibilityState) {
                comment.setAreRepliesVisible(false)
                val cachedReplies = replyCacheMap[commentId]
                cachedReplies?.removeAll { it.isLocalOnly && !it.isSending }
                comment.setReplies(ArrayList(cachedReplies ?: emptyList()))
            }
            comment.setAreRepliesVisible(newVisibilityState)
            val isAlreadyLoaded = replyLoadedMap[commentId] ?: false
            if (newVisibilityState && !isAlreadyLoaded) {
                loadReplies(commentId, limit = 3, clearList = true)
            }
            updatedList[index] = comment
            updatedList
        }
    }
    fun loadReplies(commentId: String, limit: Int, clearList: Boolean) {
        if (catId.isEmpty() || commentId.isEmpty()) return
        val lastDoc = if (clearList) null else replyLastDocMap[commentId]
        viewModelScope.launch {
            val result = replyRepository.loadReplies(catId, commentId, limit, lastDoc)

            result.onSuccess { (newReplies, newLastDoc) ->
                replyLoadedMap[commentId] = true
                val processedReplies = applyLikedReplyState(newReplies)
                replyLastDocMap[commentId] = newLastDoc
                val replyList = replyCacheMap.getOrPut(commentId) { mutableListOf() }

                if (clearList) { replyList.clear() }
                processedReplies.forEach { firebaseReply ->
                    val exists = replyList.any { it.replyId == firebaseReply.replyId }
                    if (!exists) {
                        replyList.add(firebaseReply)
                    }
                }
                val hasMoreReplies = processedReplies.size >= limit
                replyHasMoreMap[commentId] = hasMoreReplies

                val isCurrentlyVisible = _comments.value
                    .find { it.commentId == commentId }
                    ?.isAreRepliesVisible ?: false

                updateCommentReplyState(
                    commentId = commentId,
                    newReplies = replyList,
                    isVisible = isCurrentlyVisible,
                    hasMore = hasMoreReplies
                )
            }.onFailure {
                _errorMessage.emit("Yanıtlar yüklenirken hata oluştu.")
            }
        }
    }
    private fun updateCommentReplyState(
        commentId: String,
        newReplies: List<ReplyModel>,
        isVisible: Boolean? = null,
        hasMore: Boolean,
        totalCountDelta: Int = 0
    ) {
        _comments.update { currentList ->
            val index = currentList.indexOfFirst { it.commentId == commentId }
            if (index == -1) return@update currentList

            currentList.toMutableList().apply {
                val oldComment = this[index]
                this[index] = oldComment.copy().apply {
                    setReplies(ArrayList(newReplies))
                    setAreRepliesVisible(isVisible ?: oldComment.isAreRepliesVisible())
                    setHasMoreReplies(hasMore)

                    val newTotal = (oldComment.sumRepliesCount + totalCountDelta).coerceAtLeast(0)
                    setSumRepliesCount(newTotal)
                }
            }
        }
    }
    fun sendComment(content: String) {
        if (content.trim().isEmpty() || catId.isEmpty()) return

        val currentUser = currentUserManager.getCurrentUser() ?: return
        val currentUserId = currentUserManager.getCurrentUserId() ?: ""
        val username = currentUser.kullaniciAdi ?: ""

        val tempId = "temp_${System.currentTimeMillis()}"

        viewModelScope.launch {
            val newComment = CommentModel().apply {
                this.commentId = tempId
                this.commentContent = content
                this.username = username
                this.loadId = currentUserId
                this.date = Date()
                this.likeCount = 0
                this.isLiked = false
                this.isAreRepliesVisible = false
                this.replies = ArrayList()
                this.sending = true
            }

            _comments.update { current ->
                current.toMutableList().apply { add(0, newComment) }
            }
            _isEmpty.value = false

            val realCommentId = commentRepository.addComment(catId, content, username, currentUserId)

            if (realCommentId != null) {
                _actionSuccess.emit("Yorum gönderildi.")
                _commentCount.value += 1

                _comments.update { current ->
                    current.map { comment ->
                        if (comment.commentId == tempId) {
                            comment.copy().apply {
                                this.commentId = realCommentId
                                this.sending = false
                            }
                        } else {
                            comment
                        }
                    }
                }
            } else {
                _comments.update { current ->
                    current.toMutableList().apply { removeAll { it.commentId == tempId } }
                }
                _isEmpty.value = _comments.value.isEmpty()
                _errorMessage.emit("Yorum gönderilemedi.")
            }
        }
    }
    fun sendReply(commentId: String, content: String, onComplete: (String?) -> Unit) {
        if (content.trim().isEmpty() || catId.isEmpty()) {
            onComplete(null)
            return
        }

        val currentUser = currentUserManager.getCurrentUser() ?: run {
            onComplete(null)
            return
        }

        val currentUserId = currentUserManager.getCurrentUserId() ?: ""
        val username = currentUser.kullaniciAdi ?: ""
        val tempReplyId = "temp_${System.currentTimeMillis()}"

        viewModelScope.launch {
            val newReply = ReplyModel().apply {
                this.replyId = tempReplyId
                this.commentId = commentId
                this.replyContent = content
                this.name = username
                this.replyUserId = currentUserId
                this.date = Date()
                this.likeCountReply = 0
                this.isLiked = false
                this.isSending = true
                this.isLocalOnly = true
            }
            val targetComment = _comments.value.find { it.commentId == commentId }
            val isAlreadyVisible = targetComment?.isAreRepliesVisible == true
            val existingReplies = replyCacheMap[commentId] ?: mutableListOf()
            val updatedReplies = existingReplies.toMutableList().apply { add(0, newReply) }
            replyCacheMap[commentId] = updatedReplies
            val hasMore = replyHasMoreMap[commentId] ?: false
            updateCommentReplyState(commentId, updatedReplies, isVisible = isAlreadyVisible, hasMore = hasMore)
            val replyId = replyRepository.addReply(
                catId = catId,
                commentId = commentId,
                content = content,
                username = username,
                userId = currentUserId
            )
            if (replyId != null) {
                _actionSuccess.emit("Yanıt gönderildi.")

                val currentReplyList = (replyCacheMap[commentId] ?: mutableListOf()).toMutableList()
                val tempIndex = currentReplyList.indexOfFirst { it.replyId == tempReplyId }

                if (tempIndex != -1) {
                    val updatedRealReply = currentReplyList[tempIndex].copy().apply {
                        this.replyId = replyId
                        this.isSending = false
                        this.isLocalOnly = false
                    }
                    currentReplyList[tempIndex] = updatedRealReply
                    replyCacheMap[commentId] = currentReplyList
                }

                updateCommentReplyState(
                    commentId,
                    currentReplyList,
                    isVisible = isAlreadyVisible,
                    hasMore = hasMore,
                    totalCountDelta = 1
                )
                onComplete(replyId)
            } else {
                val errorList = (replyCacheMap[commentId] ?: mutableListOf()).toMutableList()
                errorList.removeAll { it.replyId == tempReplyId }
                replyCacheMap[commentId] = errorList

                updateCommentReplyState(
                    commentId,
                    errorList,
                    isVisible = isAlreadyVisible,
                    hasMore = hasMore,
                    totalCountDelta = 0
                )

                _errorMessage.emit("Yanıt gönderilemedi.")
                onComplete(null)
            }
        }
    }
    fun updateComment(commentId: String, newContent: String) {
        if (catId.isEmpty() || commentId.isEmpty() || newContent.trim().isEmpty()) return

        viewModelScope.launch {
            val success = commentRepository.updateCommentContent(catId, commentId, newContent)
            if (success) {
                _comments.update { currentList ->
                    val index = currentList.indexOfFirst { it.commentId == commentId }
                    if (index == -1) return@update currentList

                    currentList.toMutableList().apply {
                        this[index] = this[index].copy().apply {
                            setCommentContent(newContent)
                        }
                    }
                }
                _actionSuccess.emit("Yorum güncellendi.")
            } else {
                _errorMessage.emit("Yorum güncellenemedi.")
            }
        }
    }
    fun updateReply(commentId: String, replyId: String, newContent: String) {
        if (catId.isEmpty() || commentId.isEmpty() || replyId.isEmpty() || newContent.trim().isEmpty()) return

        viewModelScope.launch {
            val success = replyRepository.updateReply(catId, commentId, replyId, newContent)
            if (success) {
                val cachedReplies = replyCacheMap[commentId] ?: mutableListOf()
                val index = cachedReplies.indexOfFirst { it.replyId == replyId }

                if (index != -1) {
                    val updatedReply = cachedReplies[index].copy().apply {
                        setReplyContent(newContent)
                    }
                    val updatedList = cachedReplies.toMutableList()
                    updatedList[index] = updatedReply

                    replyCacheMap[commentId] = updatedList
                    val hasMore = replyHasMoreMap[commentId] ?: false

                    updateCommentReplyState(commentId, updatedList, null, hasMore)
                    _actionSuccess.emit("Yanıt güncellendi.")
                }
            } else {
                _errorMessage.emit("Yanıt güncellenemedi.")
            }
        }
    }
    fun deleteComment(commentId: String) {
        if (catId.isEmpty() || commentId.isEmpty()) return

        viewModelScope.launch {
            val success = commentRepository.deleteComment(catId, commentId)
            if (success) {
                _comments.update { current ->
                    current.toMutableList().apply { removeAll { it.commentId == commentId } }
                }
                _isEmpty.value = _comments.value.isEmpty()
                _commentCount.value = (_commentCount.value - 1).coerceAtLeast(0)
                _actionSuccess.emit("Yorum silindi.")
            } else {
                _errorMessage.emit("Yorum silinemedi.")
            }
        }
    }
    fun deleteReply(commentId: String, replyId: String) {
        if (catId.isEmpty() || commentId.isEmpty() || replyId.isEmpty()) return

        viewModelScope.launch {
            val success = replyRepository.deleteReply(catId, commentId, replyId)
            if (success) {
                val replyList = replyCacheMap[commentId]
                replyList?.removeAll { it.replyId == replyId }

                val updatedReplies = replyList ?: emptyList()
                val hasMore = replyHasMoreMap[commentId] ?: false

                updateCommentReplyState(commentId, updatedReplies, null, hasMore, totalCountDelta = -1)
                _actionSuccess.emit("Yanıt silindi.")
            } else {
                _errorMessage.emit("Yanıt silinemedi.")
            }
        }
    }
    fun toggleCommentLike(catId: String?, comment: CommentModel?, userId: String?) {
        if (catId == null || comment == null || userId == null) return
        val commentId = comment.commentId ?: return

        val currentLikes = _likedCommentIds.value.toMutableSet()
        val isLiked = currentLikes.contains(commentId)

        if (isLiked) {
            currentLikes.remove(commentId)
        } else {
            currentLikes.add(commentId)
        }

        _likedCommentIds.value = currentLikes
        CacheHelperComment.saveLikedSet(getApplication(), currentLikes)

        _comments.update { currentList ->
            val index = currentList.indexOfFirst { it.commentId == commentId }
            if (index == -1) return@update currentList

            currentList.toMutableList().apply {
                val item = this[index]
                val newLikeCount = if (isLiked) (item.likeCount - 1).coerceAtLeast(0) else item.likeCount + 1

                this[index] = item.copy().apply {
                    this.likeCount = newLikeCount
                    setLiked(!isLiked)
                }
            }
        }
        if (isLiked) {
            commentRepository.yorumBegeniKaldir(catId, commentId, userId).addOnFailureListener {
                rollbackCommentLikeState(commentId, isPreviouslyLiked = true)
            }
        } else {
            commentRepository.yorumBegen(catId, commentId, userId).addOnFailureListener {
                rollbackCommentLikeState(commentId, isPreviouslyLiked = false)
            }
        }
    }
    fun toggleReplyLike(catId: String, commentId: String, reply: ReplyModel?, userId: String?) {
        if (catId.isEmpty() || commentId.isEmpty() || reply == null || userId == null) return
        val replyId = reply.replyId ?: return

        val currentLikes = _likedReplyIds.value.toMutableSet()
        val isLiked = currentLikes.contains(replyId)

        if (isLiked) {
            currentLikes.remove(replyId)
        } else {
            currentLikes.add(replyId)
        }

        _likedReplyIds.value = currentLikes
        CacheHelperComment.saveLikedReplySet(getApplication(), currentLikes)

        val replyList = replyCacheMap[commentId] ?: mutableListOf()
        val index = replyList.indexOfFirst { it.replyId == replyId }

        if (index != -1) {
            val targetReply = replyList[index]
            val newCount = if (isLiked) (targetReply.likeCountReply - 1).coerceAtLeast(0) else targetReply.likeCountReply + 1

            val updatedReply = targetReply.copy().apply {
                likeCountReply = newCount
                setLiked(!isLiked)
            }

            val updatedReplies = replyList.toMutableList()
            updatedReplies[index] = updatedReply

            replyCacheMap[commentId] = updatedReplies
            val hasMore = replyHasMoreMap[commentId] ?: false

            updateCommentReplyState(commentId, updatedReplies, null, hasMore)

            viewModelScope.launch {
                if (isLiked) {
                    replyRepository.yanitBegeniKaldir(catId, commentId, replyId, userId).addOnFailureListener {
                        rollbackReplyLikeState(commentId, replyId, isPreviouslyLiked = true)
                    }
                } else {
                    replyRepository.yanitBegen(catId, commentId, replyId, userId).addOnFailureListener {
                        rollbackReplyLikeState(commentId, replyId, isPreviouslyLiked = false)
                    }
                }
            }
        }
    }
    private fun rollbackCommentLikeState(commentId: String, isPreviouslyLiked: Boolean) {
        viewModelScope.launch { _errorMessage.emit("İşlem başarısız oldu.") }

        val currentLikes = _likedCommentIds.value.toMutableSet()
        if (isPreviouslyLiked) {
            currentLikes.add(commentId)
        } else {
            currentLikes.remove(commentId)
        }
        _likedCommentIds.value = currentLikes
        CacheHelperComment.saveLikedSet(getApplication(), currentLikes)

        _comments.update { currentList ->
            val index = currentList.indexOfFirst { it.commentId == commentId }
            if (index == -1) return@update currentList

            currentList.toMutableList().apply {
                val item = this[index]
                val rolledBackCount = if (isPreviouslyLiked) item.likeCount + 1 else (item.likeCount - 1).coerceAtLeast(0)

                this[index] = item.copy().apply {
                    this.likeCount = rolledBackCount
                    setLiked(isPreviouslyLiked)
                }
            }
        }
    }
    private fun rollbackReplyLikeState(commentId: String, replyId: String, isPreviouslyLiked: Boolean) {
        viewModelScope.launch { _errorMessage.emit("Yanıt beğenilemedi.") }

        val currentLikes = _likedReplyIds.value.toMutableSet()
        if (isPreviouslyLiked) {
            currentLikes.add(replyId)
        } else {
            currentLikes.remove(replyId)
        }
        _likedReplyIds.value = currentLikes
        CacheHelperComment.saveLikedReplySet(getApplication(), currentLikes)

        val replyList = replyCacheMap[commentId] ?: mutableListOf()
        val index = replyList.indexOfFirst { it.replyId == replyId }

        if (index != -1) {
            val targetReply = replyList[index]
            val rolledBackCount = if (isPreviouslyLiked) targetReply.likeCountReply + 1 else (targetReply.likeCountReply - 1).coerceAtLeast(0)

            val updatedReply = targetReply.copy().apply {
                likeCountReply = rolledBackCount
                setLiked(isPreviouslyLiked)
            }
            val updatedReplies = replyList.toMutableList()
            updatedReplies[index] = updatedReply
            replyCacheMap[commentId] = updatedReplies
            val hasMore = replyHasMoreMap[commentId] ?: false

            updateCommentReplyState(commentId, updatedReplies, null, hasMore)
        }
    }
    fun fetchCommentCount() {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val count = commentRepository.getCommentCount(catId)
            _commentCount.value = count
        }
    }
}

