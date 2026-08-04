package com.beem.catmap.gonderi

import android.app.Application
import android.util.Log
import androidx.collection.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem
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

    private val repository: PostRepository = PostRepository
    private val userManager = CurrentUserManager.getInstance(application)

    private val _uiState = MutableStateFlow(ProfilePostUiState())
    val uiState: StateFlow<ProfilePostUiState> = _uiState.asStateFlow()

    private val _haritaSilindiEvent = MutableSharedFlow<Boolean>(replay = 0)
    val haritaSilindiEvent: SharedFlow<Boolean> = _haritaSilindiEvent.asSharedFlow()

    private val _yukleyenID = MutableStateFlow<String>("")
    val yukleyenID: StateFlow<String> = _yukleyenID.asStateFlow()

    private val profileCache = LruCache<String, ProfilePostCacheData>(3)

    private val PAGE_SIZE = 10
    var isLoadingMore = false

    val isLastPage: Boolean
        get() = profileCache.get(_yukleyenID.value)?.isLastPage ?: false

    init {
        observeProfileEvents()
    }

    private fun observeProfileEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                Log.d("POST_FLOW_DEBUG", "PostViewModel: Eventbus'tan Dinlendi -> $event")
                when (event) {
                    is ProfileEvent.PostAdded -> {
                        Log.d("POST_FLOW_DEBUG", "PostViewModel: PostAdd isteği yakalandı. gonderiKaydet() çağrılıyor...")
                        gonderileriGetir(UserSession.userId, forceRefresh = false)
                    }
                    is ProfileEvent.PostDeleted -> {
                        gonderileriGetir(UserSession.userId, forceRefresh = false)
                    }
                    else -> {}
                }
            }
        }
    }

    fun setYukleyenID(id: String) {
        _yukleyenID.value = id
    }

    fun profilDurumunuHazirla(targetUserId: String) {
        setYukleyenID(targetUserId)
        val isSelf = (targetUserId == UserSession.userId)

        val cached = profileCache.get(targetUserId)
        if (cached != null) {
            Log.d("CACHED", "Cache dolu geldi: ${cached.idList.size}")
            _uiState.update {
                it.copy(
                    posts = cached.posts,
                    postCount = cached.idList.size,
                    isEmpty = cached.posts.isEmpty(),
                    isLoading = false,
                    isAccessDenied = false
                )
            }
            if (isSelf) {
                userManager.updateGonderiSayisi(cached.idList.size.toLong())
            }
        } else {
            _uiState.update {
                it.copy(
                    posts = emptyList(),
                    postCount = 0,
                    isEmpty = true,
                    isLoading = false,
                    isAccessDenied = false
                )
            }
        }
    }

    fun gonderileriGetir(userId: String, isFollowing: Boolean = true, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return
        setYukleyenID(userId)

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isAccessDenied = false) }

            repository.getUserPosts(userId, forceRefresh)
                .onSuccess { cacheData ->
                    if (userId == UserSession.userId) {
                        userManager.updateGonderiSayisi(cacheData.idList.size.toLong())
                    }

                    _uiState.update {
                        it.copy(
                            posts = cacheData.posts,
                            postCount = cacheData.idList.size,
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
                        it.copy(posts = cacheData.posts, isMoreLoading = false)
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

}