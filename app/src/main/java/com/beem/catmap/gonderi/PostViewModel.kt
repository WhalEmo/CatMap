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
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class PostViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: PostRepository = PostRepository()
    private val userManager = CurrentUserManager.getInstance(application)

    private val _gonderilerState = MutableStateFlow<UiState<List<Gonderi>>>(UiState.Idle)
    val gonderilerState: StateFlow<UiState<List<Gonderi>>> = _gonderilerState.asStateFlow()

    private val _gonderiSayisi = MutableStateFlow<Int>(0)
    val gonderiSayisi: StateFlow<Int> = _gonderiSayisi.asStateFlow()

    // Single event'ler için replay = 0 yapıldı (Tekrarlayan Toast/SnackBar engellemek için)
    private val _islemSonucu = MutableSharedFlow<UiState<String>>(replay = 0)
    val islemSonucu: SharedFlow<UiState<String>> = _islemSonucu.asSharedFlow()

    private val _haritaSilindiEvent = MutableSharedFlow<Boolean>(replay = 0)
    val haritaSilindiEvent: SharedFlow<Boolean> = _haritaSilindiEvent.asSharedFlow()

    private val _yukleyenID = MutableStateFlow<String>("")
    val yukleyenID: StateFlow<String> = _yukleyenID.asStateFlow()

    private val profileCache = LruCache<String, ProfilePostCacheData>(3)

    private val PAGE_SIZE = 10
    var isLoadingMore = false

    val isLastPage: Boolean
        get() = profileCache.get(_yukleyenID.value)?.isLastPage ?: false

    fun setYukleyenID(id: String) {
        _yukleyenID.value = id
    }

    fun profilDurumunuHazirla(targetUserId: String) {
        setYukleyenID(targetUserId)
        val isSelf = (targetUserId == UserSession.userId)

        val cached = profileCache.get(targetUserId)
        if (cached != null) {
            Log.d("CACHED", "Cache dolu geldi: ${cached.idList.size}")
            _gonderiSayisi.value = cached.idList.size
            _gonderilerState.value = UiState.Success(cached.posts)
            if (isSelf) {
                userManager.updateGonderiSayisi(cached.idList.size.toLong())
            }
        } else {
            _gonderiSayisi.value = 0
            _gonderilerState.value = UiState.Idle
        }
    }

    fun gonderileriGetir(userId: String, forceRefresh: Boolean = false) {
        if (userId.isBlank()) return

        val cachedData = profileCache.get(userId)
        if (cachedData != null && !forceRefresh) {
            _gonderiSayisi.value = cachedData.idList.size
            _gonderilerState.value = UiState.Success(cachedData.posts)
            return
        }

        viewModelScope.launch {
            _gonderilerState.value = UiState.Loading

            repository.getKullaniciGonderiIdListesi(userId)
                .onSuccess { fullIdList ->
                    _gonderiSayisi.value = fullIdList.size

                    if (userId == UserSession.userId) {
                        userManager.updateGonderiSayisi(fullIdList.size.toLong())
                    }

                    if (fullIdList.isEmpty()) {
                        saveToCache(userId, emptyList(), emptyList(), 0, true)
                        _gonderilerState.value = UiState.Success(emptyList())
                        return@launch
                    }

                    val firstBatch = fullIdList.take(PAGE_SIZE)
                    val isLast = firstBatch.size >= fullIdList.size

                    repository.getGonderiDetaylariByIds(firstBatch)
                        .onSuccess { gonderiler ->
                            saveToCache(userId, gonderiler, fullIdList, firstBatch.size, isLast)
                            _gonderilerState.value = UiState.Success(gonderiler)
                        }
                        .onFailure { exception ->
                            _gonderilerState.value = UiState.Error(
                                exception.localizedMessage ?: "Gönderi detayları alınamadı."
                            )
                        }
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

        val cachedData = profileCache.get(userId) ?: return
        val currentOffset = cachedData.offset

        if (currentOffset >= cachedData.idList.size) return

        isLoadingMore = true

        viewModelScope.launch {
            val nextOffset = (currentOffset + PAGE_SIZE).coerceAtMost(cachedData.idList.size)
            val nextBatch = cachedData.idList.subList(currentOffset, nextOffset)
            val isLast = nextOffset >= cachedData.idList.size

            repository.getGonderiDetaylariByIds(nextBatch)
                .onSuccess { newGonderiler ->
                    val updatedPosts = cachedData.posts + newGonderiler
                    saveToCache(userId, updatedPosts, cachedData.idList, nextOffset, isLast)

                    _gonderilerState.value = UiState.Success(updatedPosts)
                    isLoadingMore = false
                }
                .onFailure {
                    isLoadingMore = false
                }
        }
    }

    fun gonderiSil(userId: String, kediId: String) {
        viewModelScope.launch {
            _islemSonucu.emit(UiState.Loading)

            repository.kullaniciGonderiSil(userId, kediId)
                .onSuccess {
                    _islemSonucu.emit(UiState.Success("Gönderi başarıyla silindi."))
                    removePostFromCacheAndUi(userId, kediId)
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
                removePostFromCacheAndUi(userId, kediId)
                _haritaSilindiEvent.emit(true)
                _islemSonucu.emit(UiState.Success("Haritadan silindi."))
            }.onFailure { exception ->
                _islemSonucu.emit(UiState.Error(exception.localizedMessage ?: "Hata oluştu."))
            }
        }
    }

    fun gonderiKaydet(userId: String, yeniGonderi: Gonderi) {
        val kediId = yeniGonderi.kediID ?: return

        viewModelScope.launch {
            _islemSonucu.emit(UiState.Loading)

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
                                _gonderiSayisi.value = updatedIdList.size

                                _gonderilerState.value =
                                    UiState.Success(yeniListe)

                                if(userId == UserSession.userId){ userManager.updateGonderiSayisi(updatedIdList.size.toLong()) }
                            }
                            .onFailure {
                                _islemSonucu.emit(UiState.Error("Gönderi yenilenemedi"))
                            }
                    }
                    else {
                        gonderileriGetir(userId,true)
                    }

                    _islemSonucu.emit(UiState.Success("Gönderi başarıyla paylaşıldı."))
                }
                .onFailure { exception ->
                    _islemSonucu.emit(
                        UiState.Error(exception.localizedMessage ?: "Gönderi paylaşılırken hata oluştu.")
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
        val activePosts = (_gonderilerState.value as? UiState.Success)?.data ?: emptyList()

        // Ekranda gösterilen listeden silinen gönderiyi çıkar
        val updatedPosts = activePosts.filterNot { it.kediID == kediId }

        // Cache'deki tüm ID listesinden silinen gönderiyi çıkar
        val updatedIdList = cachedData?.idList?.filterNot { it.kediID == kediId } ?: emptyList()
        val updatedOffset = ((cachedData?.offset ?: updatedPosts.size) - 1).coerceAtLeast(0)

        // FIX: userManager güncellenirken ekrandaki liste boyutu değil, TOPLAM ID listesinin boyutu verilmeli
        if (userId == UserSession.userId) {
            val yeniToplamSayi = updatedIdList.size.toLong()
            userManager.updateGonderiSayisi(yeniToplamSayi)
        }

        if (cachedData != null) {
            saveToCache(userId, updatedPosts, updatedIdList, updatedOffset, cachedData.isLastPage)
        }

        // UI'daki toplam sayıyı da doğru güncelliyoruz
        _gonderiSayisi.value = updatedIdList.size
        _gonderilerState.value = UiState.Success(updatedPosts)
    }
}