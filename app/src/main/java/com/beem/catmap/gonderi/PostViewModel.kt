package com.beem.catmap.gonderi

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepository(application)

    private val _uiState = MutableStateFlow(ProfilePostUiState())
    val uiState: StateFlow<ProfilePostUiState> = _uiState.asStateFlow()

    private val _haritaSilindiEvent = MutableSharedFlow<Boolean>(replay = 0)
    val haritaSilindiEvent: SharedFlow<Boolean> = _haritaSilindiEvent.asSharedFlow()

    private val _yukleyenID = MutableStateFlow("")
    val yukleyenID: StateFlow<String> = _yukleyenID.asStateFlow()

    var isLoadingMore = false

    val isLastPage: Boolean
        get() = repository.isLastPage(_yukleyenID.value)

    init {
        observeProfileEvents()
    }

    private fun observeProfileEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                Log.d("POST_FLOW_DEBUG", "PostViewModel: Eventbus'tan Dinlendi -> $event")
                when (event) {
                    is ProfileEvent.PostAdded -> {
                        Log.d("POST_FLOW_DEBUG", "PostViewModel: PostAdd isteği yakalandı. gonderileriGetir çağrılıyor...")
                        gonderileriGetir(UserSession.userId, forceRefresh = true)
                    }
                    is ProfileEvent.PostDeleted -> {
                        gonderileriGetir(UserSession.userId, forceRefresh = true)
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

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isAccessDenied = false) }

            repository.getUserPosts(userId, forceRefresh)
                .onSuccess { cacheData ->
                    _uiState.update {
                        it.copy(
                            posts = cacheData.posts,
                            isEmpty = cacheData.posts.isEmpty(),
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
        if (userId.isBlank() || isLoadingMore) return

        isLoadingMore = true
        _uiState.update { it.copy(isMoreLoading = true) }

        viewModelScope.launch {
            repository.dahaFazlaGonderiGetir(userId)
                .onSuccess { cacheData ->
                    _uiState.update {
                        it.copy(
                            posts = cacheData.posts,
                            isEmpty = cacheData.posts.isEmpty(),
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

            runCatching {
                coroutineScope {
                    val haritaSilJob = async { repository.haritadanKediSil(kediId) }
                    val gonderiSilJob = if (userId.isNotBlank()) {
                        async { repository.kullaniciGonderiSil(userId, kediId) }
                    } else null

                    haritaSilJob.await().getOrThrow()
                    gonderiSilJob?.await()?.getOrThrow()
                }
            }.onSuccess {
                CatEventBus.emitEvent(CatMapEvent.Deleted(catId = kediId))
                ProfileEventBus.emitEvent(ProfileEvent.PostDeleted(catId = kediId))
                _haritaSilindiEvent.emit(true)
                UiMessageManager.emitMessage(UiMessageState.Success("Haritadan silindi."))
            }.onFailure { exception ->
                _uiState.update { it.copy(isLoading = false) }
                UiMessageManager.emitMessage(
                    UiMessageState.Error(exception.localizedMessage ?: "Hata oluştu.")
                )
            }
        }
    }

    fun setupFromFullProfile(userId: String, cacheData: ProfilePostCacheData) {
        setYukleyenID(userId)

        _uiState.update {
            it.copy(
                posts = cacheData.posts,

                isEmpty = cacheData.posts.isEmpty(),
                isLoading = false,
                isAccessDenied = false
            )
        }
    }
}