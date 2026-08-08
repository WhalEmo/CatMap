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

    // Coroutine Job takipleri
    private var fetchPostsJob: Job? = null
    private var loadMoreJob: Job? = null

    init {
        observeProfileEvents()
    }

    private fun observeProfileEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                when (event) {
                    is ProfileEvent.PostAdded -> {
                        val state = _uiState.value
                        if (state.isLoading) return@collect
                        if (state.posts.any { it.kediID == event.post.kediID }) return@collect
                        _uiState.update {
                            it.copy(
                                posts = listOf(event.post) + it.posts,
                                isEmpty = false
                            )
                        }
                    }

                    is ProfileEvent.PostDeleted -> {

                        _uiState.update { state ->
                            val updatedPosts = state.posts.filterNot { it.kediID == event.catId }
                            state.copy(
                                posts = updatedPosts,
                                isEmpty = updatedPosts.isEmpty()
                            )
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


    fun dahaFazlaGonderiGetir() {
        val currentState = _uiState.value
        val userId = _yukleyenID.value

        if (userId.isBlank() ||
            currentState.isMoreLoading ||
            currentState.isLoading ||
            currentState.isLastPage ||
            currentState.lastDocument == null
        ) {
            return
        }

        loadMoreJob?.cancel()
        loadMoreJob = viewModelScope.launch {
            _uiState.update { it.copy(isMoreLoading = true) }

            repository.getKullaniciGonderileri(userId = userId, lastDocument = currentState.lastDocument)
                .onSuccess { pageResult ->
                    _uiState.update { state ->
                        val updatedPosts = state.posts + pageResult.posts
                        state.copy(
                            posts = updatedPosts,
                            isEmpty = updatedPosts.isEmpty(),
                            isMoreLoading = false,
                            isLastPage = pageResult.isLastPage,
                            lastDocument = pageResult.lastDocument
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isMoreLoading = false) }
                    UiMessageManager.emitMessage(
                        UiMessageState.Error(exception.localizedMessage ?: "Daha fazla gönderi alınamadı.")
                    )
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

    fun setPostLoadingState() {
        _uiState.update {
            it.copy(
                isLoading = true,
                isAccessDenied = false,
                isEmpty = false,
                isLastPage = false,     // EKLENDİ: Sayfalamayı sıfırla
                lastDocument = null     // EKLENDİ: Son döküman referansını temizle
            )
        }
    }


    fun gonderileriGetir(
        userId: String,
        isFollowing: Boolean = true,
        forceRefresh: Boolean = false
    ) {
        if (userId.isBlank()) return
        setYukleyenID(userId)



        fetchPostsJob?.cancel()
        loadMoreJob?.cancel()

        val isSelfProfile = userId == UserSession.userId
        val canAccess = isSelfProfile || isFollowing

        if (!canAccess) {
            _uiState.update {
                it.copy(
                    posts = emptyList(),
                    isEmpty = false,
                    isLoading = false,
                    isAccessDenied = true,
                    isLastPage = true,
                    lastDocument = null
                )
            }
            return
        }

        fetchPostsJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    isAccessDenied = false,
                    isEmpty = false,
                    posts = emptyList(),
                    lastDocument = null,
                    isLastPage = false
                )
            }

            repository.getKullaniciGonderileri(
                userId = userId,
                lastDocument = null,
                forceRefresh = forceRefresh
            )
                .onSuccess { pageResult ->

                    _uiState.update { currentState ->
                        currentState.copy(
                            posts = pageResult.posts,
                            isEmpty = pageResult.posts.isEmpty(),
                            isLoading = false,
                            isAccessDenied = false,
                            isLastPage = pageResult.isLastPage,
                            lastDocument = pageResult.lastDocument
                        )
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false, isAccessDenied = true) }
                    UiMessageManager.emitMessage(
                        UiMessageState.Error(exception.localizedMessage ?: "Gönderiler yüklenemedi.")
                    )
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
        isLast: Boolean = true,
        isAccessDenied: Boolean = false
    ) {
        setYukleyenID(userId)

        _uiState.update {
            it.copy(
                posts = initialPosts,
                isEmpty = initialPosts.isEmpty() && !isAccessDenied,
                isLoading = false,
                isAccessDenied = isAccessDenied,
                isLastPage = isLast,
                lastDocument = lastDoc
            )
        }
    }

    fun setAccessDenied(isDenied: Boolean) {
        _uiState.update { currentState ->
            currentState.copy(
                isAccessDenied = isDenied,
                posts = emptyList(),
                isLoading = false,      // EKLENDİ: Yüklemeyi sonlandır
                isEmpty = false,        // EKLENDİ: "Gönderi yok" uyarısıyla çakışmayı önle
                isLastPage = true,      // EKLENDİ: Sayfalamayı durdur
                lastDocument = null     // EKLENDİ: Referansı temizle
            )
        }
    }

}

