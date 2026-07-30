package com.beem.catmap.gonderi

import androidx.collection.LruCache
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem
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

class PostViewModel : ViewModel() {

    private val repository: PostRepository = PostRepository()
    private val followRepository: FollowRepository = FollowRepository()

    private val _gonderilerState = MutableStateFlow<UiState<List<Gonderi>>>(UiState.Idle)
    val gonderilerState: StateFlow<UiState<List<Gonderi>>> = _gonderilerState.asStateFlow()

    private val _gonderiSayisi = MutableStateFlow<Int>(0)
    val gonderiSayisi: StateFlow<Int> = _gonderiSayisi.asStateFlow()

    private val _islemSonucu = MutableSharedFlow<UiState<String>>()
    val islemSonucu: SharedFlow<UiState<String>> = _islemSonucu.asSharedFlow()

    private val _haritaSilindiEvent = MutableSharedFlow<Boolean>()
    val haritaSilindiEvent: SharedFlow<Boolean> = _haritaSilindiEvent.asSharedFlow()

    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    private val _yukleyenID = MutableStateFlow<String>("")
    val yukleyenID: StateFlow<String> = _yukleyenID.asStateFlow()

    private val profilePostsCache = LruCache<String, MutableList<Gonderi>>(3)
    private val profileKediItemsCache = LruCache<String, MutableList<GonderilenKediItem>>(3)
    private val profileOffsetCache = LruCache<String, Int>(3)
    private val profileLastPageCache = LruCache<String, Boolean>(3)

    private val PAGE_SIZE = 12
    var isLoadingMore = false

    val isLastPage: Boolean
        get() = profileLastPageCache.get(_yukleyenID.value) ?: false

    fun setYukleyenID(id: String) {
        _yukleyenID.value = id
    }

    fun profilDurumunuHazirla(targetUserId: String) {
        setYukleyenID(targetUserId)
        val currentUserId = UserSession.userId
        val isSelf = (targetUserId == currentUserId)

        _profileUiState.update { it.copy(isSelfProfile = isSelf) }

        if (!isSelf) {
            kullaniciTakipDurumunuKontrolEt(targetUserId)
        }
    }

    private fun kullaniciTakipDurumunuKontrolEt(targetUserId: String) {
        viewModelScope.launch {
            _profileUiState.update { it.copy(isLoadingFollowState = true) }

            val isFollowing = followRepository.isFollowing(targetUserId).getOrDefault(false)
            val isFollowed = followRepository.isFollowedBy(targetUserId).getOrDefault(false)

            _profileUiState.update {
                it.copy(isFollowing = isFollowing, isFollowed = isFollowed, isLoadingFollowState = false)
            }
        }
    }

    fun gonderileriGetir(userId: String) {
        if (userId.isBlank()) return

        viewModelScope.launch {
            val cachedPosts = profilePostsCache.get(userId)
            val cachedItems = profileKediItemsCache.get(userId)

            if (cachedPosts != null && cachedItems != null) {
                _gonderiSayisi.value = cachedItems.size
                _gonderilerState.value = UiState.Success(cachedPosts.toList())
                return@launch
            }

            _gonderilerState.value = UiState.Loading

            repository.getKullaniciGonderiIdListesi(userId)
                .onSuccess { fullIdList ->
                    val userItems = fullIdList.toMutableList()
                    profileKediItemsCache.put(userId, userItems)
                    _gonderiSayisi.value = userItems.size

                    if (userItems.isEmpty()) {
                        profileLastPageCache.put(userId, true)
                        profilePostsCache.put(userId, mutableListOf())
                        _gonderilerState.value = UiState.Success(emptyList())
                        return@launch
                    }

                    val firstBatch = userItems.take(PAGE_SIZE)
                    profileOffsetCache.put(userId, firstBatch.size)

                    if (firstBatch.size >= userItems.size) {
                        profileLastPageCache.put(userId, true)
                    } else {
                        profileLastPageCache.put(userId, false)
                    }

                    fetchAndEmitDetails(userId, firstBatch)
                }
                .onFailure { exception ->
                    _gonderilerState.value = UiState.Error(
                        exception.localizedMessage ?: "Gönderiler yüklenirken hata oluştu."
                    )
                }
        }
    }

    fun dahaFazlaGonderiGetir() {
        val userId = _yukleyenID.value
        if (userId.isBlank() || isLoadingMore || isLastPage) return

        val userItems = profileKediItemsCache.get(userId) ?: return
        val currentOffset = profileOffsetCache.get(userId) ?: 0

        if (currentOffset >= userItems.size) return

        isLoadingMore = true

        viewModelScope.launch {
            val nextOffset = (currentOffset + PAGE_SIZE).coerceAtMost(userItems.size)
            val nextBatch = userItems.subList(currentOffset, nextOffset)

            profileOffsetCache.put(userId, nextOffset)

            if (nextOffset >= userItems.size) {
                profileLastPageCache.put(userId, true)
            }

            repository.getGonderiDetaylariByIds(nextBatch)
                .onSuccess { newGonderiler ->
                    val userPosts = profilePostsCache.get(userId) ?: mutableListOf()
                    userPosts.addAll(newGonderiler)
                    profilePostsCache.put(userId, userPosts)

                    _gonderilerState.value = UiState.Success(userPosts.toList())
                    isLoadingMore = false
                }
                .onFailure {
                    isLoadingMore = false
                }
        }
    }

    private suspend fun fetchAndEmitDetails(userId: String, items: List<GonderilenKediItem>) {
        repository.getGonderiDetaylariByIds(items)
            .onSuccess { gonderiler ->
                val userPosts = profilePostsCache.get(userId) ?: mutableListOf()
                userPosts.addAll(gonderiler)
                profilePostsCache.put(userId, userPosts)

                _gonderilerState.value = UiState.Success(userPosts.toList())
            }
            .onFailure { exception ->
                _gonderilerState.value = UiState.Error(
                    exception.localizedMessage ?: "Gönderi detayları alınamadı."
                )
            }
    }

    fun gonderiSil(userId: String, kediId: String) {
        viewModelScope.launch {
            _islemSonucu.emit(UiState.Loading)

            repository.kullaniciGonderiSil(userId, kediId)
                .onSuccess {
                    _islemSonucu.emit(UiState.Success("Gönderi başarıyla silindi."))

                    val userItems = profileKediItemsCache.get(userId)
                    userItems?.removeAll { it.kediID == kediId }
                    _gonderiSayisi.value = userItems?.size ?: 0

                    val userPosts = profilePostsCache.get(userId)
                    userPosts?.removeAll { it.kediID == kediId }

                    _gonderilerState.value = UiState.Success(userPosts?.toList() ?: emptyList())
                }
                .onFailure { exception ->
                    _islemSonucu.emit(
                        UiState.Error(exception.localizedMessage ?: "Gönderi silinemedi.")
                    )
                }
        }
    }

    fun haritadanVeGonderilerdenSil(userId: String, kediId: String) {
        if (kediId.isBlank()) return
        viewModelScope.launch {
            _islemSonucu.emit(UiState.Loading)

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
                val userItems = profileKediItemsCache.get(userId)
                userItems?.removeAll { it.kediID == kediId }
                _gonderiSayisi.value = userItems?.size ?: 0

                val userPosts = profilePostsCache.get(userId)
                userPosts?.removeAll { it.kediID == kediId }

                _gonderilerState.value = UiState.Success(userPosts?.toList() ?: emptyList())

                _haritaSilindiEvent.emit(true)
                _islemSonucu.emit(UiState.Success("Haritadan silindi."))
            }.onFailure { exception ->
                _islemSonucu.emit(UiState.Error(exception.localizedMessage ?: "Hata oluştu."))
            }
        }
    }

    fun gonderiKaydet(userId: String, yeniGonderi: Gonderi) {
        viewModelScope.launch {
            _islemSonucu.emit(UiState.Loading)

            repository.kullaniciGonderiKaydet(userId, yeniGonderi.kediID ?: "")
                .onSuccess {
                    _islemSonucu.emit(UiState.Success("Gönderi başarıyla paylaşıldı."))

                    val yeniKediItem = GonderilenKediItem(
                        kediID = yeniGonderi.kediID ?: "",
                        tarih = yeniGonderi.tarih
                    )

                    val userItems = profileKediItemsCache.get(userId) ?: mutableListOf()
                    userItems.add(0, yeniKediItem)
                    profileKediItemsCache.put(userId, userItems)
                    _gonderiSayisi.value = userItems.size

                    val userPosts = profilePostsCache.get(userId) ?: mutableListOf()
                    userPosts.add(0, yeniGonderi)
                    profilePostsCache.put(userId, userPosts)

                    val currentOffset = profileOffsetCache.get(userId) ?: 0
                    profileOffsetCache.put(userId, currentOffset + 1)

                    _gonderilerState.value = UiState.Success(userPosts.toList())
                }
                .onFailure { exception ->
                    _islemSonucu.emit(
                        UiState.Error(exception.localizedMessage ?: "Gönderi paylaşılırken hata oluştu.")
                    )
                }
        }
    }
}