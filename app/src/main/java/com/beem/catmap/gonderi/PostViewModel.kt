package com.beem.catmap.gonderi

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.models.Gonderi
import com.beem.catmap.models.GonderilenKediItem
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel() : ViewModel() {

    private val repository: PostRepository = PostRepository()
    private val followRepository: FollowRepository = FollowRepository()

    private val _gonderilerState = MutableStateFlow<UiState<List<Gonderi>>>(UiState.Idle)
    val gonderilerState: StateFlow<UiState<List<Gonderi>>> = _gonderilerState.asStateFlow()

    private val _gonderiSayisi = MutableStateFlow<Int>(0)
    val gonderiSayisi: StateFlow<Int> = _gonderiSayisi.asStateFlow()

    private val _islemSonucu = MutableSharedFlow<UiState<String>>()
    val islemSonucu: SharedFlow<UiState<String>> = _islemSonucu.asSharedFlow()

    // RAM Bellekte Tutulan Veriler
    private val allUserKediItems = mutableListOf<GonderilenKediItem>()
    private val loadedGonderiler = mutableListOf<Gonderi>()

    private var currentOffset = 0
    private val PAGE_SIZE = 12

    var isLoadingMore = false
    var isLastPage = false

    // 1. İLK YÜKLEME: User Dokümanı 1 Defa Okunur
    // PostViewModel.kt veya ProfileViewModel.kt
    private val _profileUiState = MutableStateFlow(ProfileUiState())
    val profileUiState: StateFlow<ProfileUiState> = _profileUiState.asStateFlow()

    fun profilDurumunuHazirla(targetUserId: String) {
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

            // Repository üzerinden takip durumu sorgusu (örnek)
            // repository.isFollowing(UserSession.userId, targetUserId)...
            val isFollowing = false // Veritabanından gelen durum

            _profileUiState.update {
                it.copy(isFollowing = isFollowing, isLoadingFollowState = false)
            }
        }
    }

    fun takipEtVeyaBip(targetUserId: String) {
        viewModelScope.launch {
            val currentIsFollowing = _profileUiState.value.isFollowing
            // Takip et / Takipten çık veritabanı işlemi...
            _profileUiState.update { it.copy(isFollowing = !currentIsFollowing) }
        }
    }
    fun gonderileriGetir(userId: String) {
        if (userId.isBlank()) return

        viewModelScope.launch {
            _gonderilerState.value = UiState.Loading

            // Sıfırlama işlemleri
            allUserKediItems.clear()
            loadedGonderiler.clear()
            currentOffset = 0
            isLastPage = false

            // Kullanıcı dokümanını 1 kere oku ve ID listesini al
            repository.getKullaniciGonderiIdListesi(userId)
                .onSuccess { fullIdList ->
                    allUserKediItems.addAll(fullIdList)
                    _gonderiSayisi.value = fullIdList.size

                    if (fullIdList.isEmpty()) {
                        isLastPage = true
                        _gonderilerState.value = UiState.Success(emptyList())
                        return@launch
                    }

                    // İlk 12 elemanı kes ve detaylarını iste
                    val firstBatch = allUserKediItems.take(PAGE_SIZE)
                    currentOffset = firstBatch.size

                    if (currentOffset >= allUserKediItems.size) {
                        isLastPage = true
                    }

                    fetchAndEmitDetails(firstBatch)
                }
                .onFailure { exception ->
                    _gonderilerState.value = UiState.Error(
                        exception.localizedMessage ?: "Gönderiler yüklenirken hata oluştu."
                    )
                }
        }
    }

    fun dahaFazlaGonderiGetir() {
        if (isLoadingMore || isLastPage || currentOffset >= allUserKediItems.size) return

        isLoadingMore = true

        viewModelScope.launch {
            val nextOffset = (currentOffset + PAGE_SIZE).coerceAtMost(allUserKediItems.size)
            val nextBatch = allUserKediItems.subList(currentOffset, nextOffset)

            currentOffset = nextOffset

            if (currentOffset >= allUserKediItems.size) {
                isLastPage = true
            }

            repository.getGonderiDetaylariByIds(nextBatch)
                .onSuccess { newGonderiler ->
                    loadedGonderiler.addAll(newGonderiler)
                    _gonderilerState.value = UiState.Success(loadedGonderiler.toList())
                    isLoadingMore = false
                }
                .onFailure {
                    isLoadingMore = false
                }
        }
    }

    private suspend fun fetchAndEmitDetails(items: List<GonderilenKediItem>) {
        repository.getGonderiDetaylariByIds(items)
            .onSuccess { gonderiler ->
                loadedGonderiler.addAll(gonderiler)
                _gonderilerState.value = UiState.Success(loadedGonderiler.toList())
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
                    allUserKediItems.removeAll { it.kediID == kediId }
                    _gonderiSayisi.value = allUserKediItems.size

                    loadedGonderiler.removeAll { it.kediID == kediId }

                    _gonderilerState.value = UiState.Success(loadedGonderiler.toList())
                }
                .onFailure { exception ->
                    _islemSonucu.emit(
                        UiState.Error(exception.localizedMessage ?: "Gönderi silinemedi.")
                    )
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
                    allUserKediItems.add(0, yeniKediItem)
                    _gonderiSayisi.value = allUserKediItems.size

                    currentOffset += 1

                    loadedGonderiler.add(0, yeniGonderi)
                    _gonderilerState.value = UiState.Success(loadedGonderiler.toList())
                }
                .onFailure { exception ->
                    _islemSonucu.emit(
                        UiState.Error(exception.localizedMessage ?: "Gönderi paylaşılırken hata oluştu.")
                    )
                }
        }
    }
}