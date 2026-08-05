package com.beem.catmap.gonderi

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = PostRepository.getInstance(application)

    private val _uiState = MutableStateFlow(ProfilePostUiState())
    val uiState: StateFlow<ProfilePostUiState> = _uiState.asStateFlow()

    private val _haritaSilindiEvent = MutableSharedFlow<Boolean>(replay = 0)
    val haritaSilindiEvent: SharedFlow<Boolean> = _haritaSilindiEvent.asSharedFlow()

    private val _yukleyenID = MutableStateFlow("")
    val yukleyenID: StateFlow<String> = _yukleyenID.asStateFlow()

    private var lastDocument: DocumentSnapshot? = null
    var isLastPage: Boolean = false
        private set
    private var isLoadingMore: Boolean = false

    // İstek çakışmalarını önlemek için Job takibi
    private var fetchPostsJob: Job? = null

    init {
        observeProfileEvents()
    }

    private fun observeProfileEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                Log.d("POST_FLOW_DEBUG", "PostViewModel: Eventbus'tan Dinlendi -> $event")
                when (event) {
                    is ProfileEvent.PostAdded, is ProfileEvent.PostDeleted -> {
                        val activeUserId = _yukleyenID.value
                        if (activeUserId.isNotBlank() && activeUserId == UserSession.userId) {
                            gonderileriGetir(activeUserId, forceRefresh = true)
                        }
                    }
                    else -> {}
                }
            }
        }
    }

    fun setYukleyenID(id: String) {
        _yukleyenID.value = id
    }

    fun gonderileriGetir(userId: String, isFollowing: Boolean = true, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return
        setYukleyenID(userId)

        fetchPostsJob?.cancel()

        lastDocument = null
        isLastPage = false
        isLoadingMore = false

        fetchPostsJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isAccessDenied = false) }

            repository.getKullaniciGonderileri(userId = userId, lastDocument = null)
                .onSuccess { pageResult ->
                    lastDocument = pageResult.lastDocument
                    isLastPage = pageResult.isLastPage

                    _uiState.update {
                        it.copy(
                            posts = pageResult.posts,
                            isEmpty = pageResult.posts.isEmpty(),
                            isLoading = false,
                            isAccessDenied = false
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    UiMessageManager.emitMessage(
                        UiMessageState.Error(exception.localizedMessage ?: "Gönderiler yüklenemedi.")
                    )
                }
        }
    }

    fun dahaFazlaGonderiGetir() {
        val userId = _yukleyenID.value
        if (userId.isBlank() || isLoadingMore || isLastPage || lastDocument == null) return

        isLoadingMore = true
        _uiState.update { it.copy(isMoreLoading = true) }

        viewModelScope.launch {
            repository.getKullaniciGonderileri(userId = userId, lastDocument = lastDocument)
                .onSuccess { pageResult ->
                    lastDocument = pageResult.lastDocument
                    isLastPage = pageResult.isLastPage

                    val currentPosts = _uiState.value.posts
                    val updatedPosts = currentPosts + pageResult.posts

                    _uiState.update {
                        it.copy(
                            posts = updatedPosts,
                            isEmpty = updatedPosts.isEmpty(),
                            isMoreLoading = false
                        )
                    }
                    isLoadingMore = false
                }
                .onFailure {
                    _uiState.update { it.copy(isMoreLoading = false) }
                    isLoadingMore = false
                }
        }
    }

    fun gonderiSil(userId: String, kediId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.kullaniciGonderiSil(userId, kediId)
                .onSuccess {
                    UiMessageManager.emitMessage(UiMessageState.Success("Gönderi başarıyla silindi."))

                    val updatedPosts = _uiState.value.posts.filterNot { it.kediID == kediId }
                    _uiState.update {
                        it.copy(
                            posts = updatedPosts,
                            isEmpty = updatedPosts.isEmpty(),
                            isLoading = false
                        )
                    }

                    ProfileEventBus.emitEvent(ProfileEvent.PostDeleted(catId = kediId))
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    UiMessageManager.emitMessage(
                        UiMessageState.Error(exception.localizedMessage ?: "Gönderi silinemedi.")
                    )
                }
        }
    }

    fun haritadanVeGonderilerdenSil(userId: String, kediId: String) {
        if (kediId.isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Sıralı silme ile tutarlılığı garanti altına alıyoruz
            val gonderiRes = if (userId.isNotBlank()) repository.kullaniciGonderiSil(userId, kediId) else Result.success(Unit)
            val haritaRes = repository.haritadanKediSil(kediId)

            if (gonderiRes.isSuccess && haritaRes.isSuccess) {
                val updatedPosts = _uiState.value.posts.filterNot { it.kediID == kediId }
                _uiState.update {
                    it.copy(
                        posts = updatedPosts,
                        isEmpty = updatedPosts.isEmpty(),
                        isLoading = false
                    )
                }

                CatEventBus.emitEvent(CatMapEvent.Deleted(catId = kediId))
                ProfileEventBus.emitEvent(ProfileEvent.PostDeleted(catId = kediId))
                _haritaSilindiEvent.emit(true)
                UiMessageManager.emitMessage(UiMessageState.Success("Haritadan silindi."))
            } else {
                _uiState.update { it.copy(isLoading = false) }
                val errorMsg = gonderiSilError(gonderiRes, haritaRes)
                UiMessageManager.emitMessage(UiMessageState.Error(errorMsg))
            }
        }
    }

    private fun gonderiSilError(gonderiRes: Result<Unit>, haritaRes: Result<Unit>): String {
        return gonderiRes.exceptionOrNull()?.localizedMessage
            ?: haritaRes.exceptionOrNull()?.localizedMessage
            ?: "Silme işlemi sırasında hata oluştu."
    }

    fun setupFromFullProfile(
        userId: String,
        initialPosts: List<Gonderi>,
        lastDoc: DocumentSnapshot? = null,
        isLast: Boolean = true
    ) {
        setYukleyenID(userId)

        this.lastDocument = lastDoc
        this.isLastPage = isLast
        this.isLoadingMore = false

        _uiState.update {
            it.copy(
                posts = initialPosts,
                isEmpty = initialPosts.isEmpty(),
                isLoading = false,
                isAccessDenied = false
            )
        }
    }
}