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

    private val repository: PostRepository = PostRepository()
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
                        onPostAddedRemote(UserSession.userId, event.post)
                    }
                    is ProfileEvent.PostDeleted -> {
                        event.catId?.let {
                            removePostFromCacheAndUi(
                                userId = UserSession.userId,
                                kediId = event.catId
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

    fun gonderileriGetir(
        userId: String,
        isFollowing: Boolean = false,
        forceRefresh: Boolean = false,
    ) {
        if (userId.isBlank()) return

        if (!isFollowing) {
            _uiState.update {
                it.copy(
                    posts = emptyList(),
                    postCount = 0,
                    isEmpty = true,
                    isLoading = false,
                    isAccessDenied = true
                )
            }
            return
        }

        val cachedData = profileCache.get(userId)
        if (cachedData != null && !forceRefresh) {
            _uiState.update {
                it.copy(
                    posts = cachedData.posts,
                    postCount = cachedData.idList.size,
                    isEmpty = cachedData.posts.isEmpty(),
                    isLoading = false,
                    isAccessDenied = false
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, isAccessDenied = false) }

            repository.getKullaniciGonderiIdListesi(userId)
                .onSuccess { fullIdList ->

                    if (userId == UserSession.userId) {
                        userManager.updateGonderiSayisi(fullIdList.size.toLong())
                    }

                    if (fullIdList.isEmpty()) {
                        saveToCache(userId, emptyList(), emptyList(), 0, true)
                        _uiState.update {
                            it.copy(posts = emptyList(), postCount = 0, isEmpty = true, isLoading = false)
                        }
                        return@launch
                    }

                    val firstBatch = fullIdList.take(PAGE_SIZE)
                    val isLast = firstBatch.size >= fullIdList.size

                    repository.getGonderiDetaylariByIds(firstBatch)
                        .onSuccess { gonderiler ->
                            saveToCache(userId, gonderiler, fullIdList, firstBatch.size, isLast)
                            _uiState.update {
                                it.copy(
                                    posts = gonderiler,
                                    postCount = fullIdList.size,
                                    isEmpty = gonderiler.isEmpty(),
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
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    UiMessageManager.emitMessage(
                        UiMessageState.Error(exception.localizedMessage ?: "Hata oluştu.")
                    )
                }
        }
    }

    fun onPostAddedRemote(userId: String, yeniGonderi: Gonderi) {
        val kediId = yeniGonderi.kediID ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            val yeniKediItem = GonderilenKediItem(
                kediID = kediId,
                tarih = yeniGonderi.tarih
            )
            val cachedData = profileCache.get(userId)

            if (cachedData != null) {
                val updatedIdList = listOf(yeniKediItem) + cachedData.idList
                val firstPageIds = updatedIdList.take(PAGE_SIZE)

                repository.getGonderiDetaylariByIds(firstPageIds)
                    .onSuccess { yeniListe ->
                        val isLast = firstPageIds.size >= updatedIdList.size
                        saveToCache(
                            userId = userId,
                            posts = yeniListe,
                            idList = updatedIdList,
                            offset = firstPageIds.size,
                            isLastPage = isLast
                        )

                        if (userId == UserSession.userId) {
                            userManager.updateGonderiSayisi(updatedIdList.size.toLong())
                        }

                        _uiState.update {
                            it.copy(
                                posts = yeniListe,
                                postCount = updatedIdList.size,
                                isEmpty = yeniListe.isEmpty(),
                                isLoading = false,
                                isAccessDenied = false
                            )
                        }
                        Log.d("POST_FLOW_DEBUG", "PostViewModel: Önbellek ve UI State başarıyla güncellendi.")
                    }
                    .onFailure {
                        _uiState.update { it.copy(isLoading = false) }
                        UiMessageManager.emitMessage(UiMessageState.Error("Profil önbelleği yenilenemedi."))
                    }
            } else {
                // Önbellek yoksa doğrudan uzak sunucudan güncel listeyi çekmesini söylüyoruz
                gonderileriGetir(userId, isFollowing = true, forceRefresh = true)
            }
        }
    }

    fun dahaFazlaGonderiGetir() {
        val userId = _yukleyenID.value
        if (userId.isBlank() || isLoadingMore || isLastPage) return

        val cachedData = profileCache.get(userId) ?: return
        val currentOffset = cachedData.offset

        if (currentOffset >= cachedData.idList.size) return

        isLoadingMore = true
        _uiState.update { it.copy(isMoreLoading = true) }

        viewModelScope.launch {
            val nextOffset = (currentOffset + PAGE_SIZE).coerceAtMost(cachedData.idList.size)
            val nextBatch = cachedData.idList.subList(currentOffset, nextOffset)
            val isLast = nextOffset >= cachedData.idList.size

            repository.getGonderiDetaylariByIds(nextBatch)
                .onSuccess { newGonderiler ->
                    val updatedPosts = cachedData.posts + newGonderiler
                    saveToCache(userId, updatedPosts, cachedData.idList, nextOffset, isLast)
                    _uiState.update {
                        it.copy(
                            posts = updatedPosts,
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

    fun gonderiKaydet(userId: String, yeniGonderi: Gonderi) {
        val kediId = yeniGonderi.kediID ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            repository.kullaniciGonderiKaydet(userId, kediId)
                .onSuccess {
                    val yeniKediItem = GonderilenKediItem(
                        kediID = kediId,
                        tarih = yeniGonderi.tarih
                    )
                    val cachedData = profileCache.get(userId)
                    if (cachedData != null) {
                        val updatedIdList = listOf(yeniKediItem) + cachedData.idList
                        val firstPageIds = updatedIdList.take(PAGE_SIZE)

                        repository.getGonderiDetaylariByIds(firstPageIds)
                            .onSuccess { yeniListe ->
                                val isLast = firstPageIds.size >= updatedIdList.size
                                saveToCache(
                                    userId = userId,
                                    posts = yeniListe,
                                    idList = updatedIdList,
                                    offset = firstPageIds.size,
                                    isLastPage = isLast
                                )

                                if (userId == UserSession.userId) {
                                    userManager.updateGonderiSayisi(updatedIdList.size.toLong())
                                }

                                _uiState.update {
                                    it.copy(
                                        posts = yeniListe,
                                        postCount = updatedIdList.size,
                                        isEmpty = yeniListe.isEmpty(),
                                        isLoading = false,
                                        isAccessDenied = false
                                    )
                                }
                                UiMessageManager.emitMessage(UiMessageState.Success("Gönderi başarıyla paylaşıldı."))
                            }
                            .onFailure {
                                _uiState.update { it.copy(isLoading = false) }
                                UiMessageManager.emitMessage(UiMessageState.Error("Gönderi yenilenemedi"))
                            }
                    } else {
                        gonderileriGetir(userId, isFollowing = true, forceRefresh = true)
                        UiMessageManager.emitMessage(UiMessageState.Success("Gönderi başarıyla paylaşıldı."))
                    }
                }
                .onFailure { exception ->
                    _uiState.update { it.copy(isLoading = false) }
                    UiMessageManager.emitMessage(
                        UiMessageState.Error(exception.localizedMessage ?: "Hata oluştu.")
                    )
                }
        }
    }


    private fun saveToCache(
        userId: String,
        posts: List<Gonderi>,
        idList: List<GonderilenKediItem>,
        offset: Int,
        isLastPage: Boolean
    ) {
        profileCache.put(
            userId,
            ProfilePostCacheData(posts, idList, offset, isLastPage)
        )
    }


    private fun removePostFromCacheAndUi(userId: String, kediId: String) {
        val cachedData = profileCache.get(userId)
        val activePosts = _uiState.value.posts

        // Silinecek eleman ekranda var mı?
        val wasInLoadedPosts = activePosts.any { it.kediID == kediId }
        val updatedPosts = activePosts.filterNot { it.kediID == kediId }.toList()

        val updatedIdList = cachedData?.idList?.filterNot { it.kediID == kediId } ?: emptyList()

        // Yalnızca silinen eleman yüklenenler (posts) içerisindeyse offset değerini 1 düşürürüz
        val currentOffset = cachedData?.offset ?: updatedPosts.size
        val updatedOffset = if (wasInLoadedPosts) {
            (currentOffset - 1).coerceAtLeast(0)
        } else {
            currentOffset.coerceAtMost(updatedIdList.size)
        }

        if (userId == UserSession.userId) {
            userManager.updateGonderiSayisi(updatedIdList.size.toLong())
        }

        if (cachedData != null) {
            saveToCache(userId, updatedPosts, updatedIdList, updatedOffset, cachedData.isLastPage)
        }

        _uiState.update {
            it.copy(
                posts = updatedPosts,
                postCount = updatedIdList.size,
                isEmpty = updatedPosts.isEmpty(),
                isLoading = false
            )
        }
    }
}