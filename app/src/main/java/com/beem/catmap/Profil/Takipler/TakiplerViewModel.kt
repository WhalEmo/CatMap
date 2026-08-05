package com.beem.catmap.Profil.Takipler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TakiplerViewModel() : ViewModel() {

    private val repository: TakiplerRepository = TakiplerRepository.getInstance()
    private val PAGE_LIMIT = 10L

    // ==========================================
    // TAKİPÇİLER (FOLLOWERS) STATE VE DEĞİŞKENLERİ
    // ==========================================
    private val _takipcilerState = MutableStateFlow<TakiplerUiState>(TakiplerUiState.Idle)
    val takipcilerState: StateFlow<TakiplerUiState> = _takipcilerState.asStateFlow()

    private val currentTakipciler = mutableListOf<Kullanici>()
    private var lastTakipciDoc: DocumentSnapshot? = null
    private var isTakipcilerLastPage = false
    private var isLoadingTakipciler = false

    // ==========================================
    // TAKİP EDİLENLER (FOLLOWING) STATE VE DEĞİŞKENLERİ
    // ==========================================
    private val _takipEdilenlerState = MutableStateFlow<TakiplerUiState>(TakiplerUiState.Idle)
    val takipEdilenlerState: StateFlow<TakiplerUiState> = _takipEdilenlerState.asStateFlow()

    private val currentTakipEdilenler = mutableListOf<Kullanici>()
    private var lastTakipEdilenDoc: DocumentSnapshot? = null
    private var isTakipEdilenlerLastPage = false
    private var isLoadingTakipEdilenler = false

    // ==========================================
    // TAKİPÇİLERİ GETİR (FETCH FOLLOWERS)
    // ==========================================
    fun fetchTakipciler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false
    ) {
        if (userId.isNullOrBlank() || isLoadingTakipciler) return

        // 1. Sekme Değişimi Kontrolü: Liste zaten RAM'de varsa ve yenileme istenmiyorsa istek atma
        if (!isNextPage && !isRefresh && currentTakipciler.isNotEmpty()) {
            _takipcilerState.value = TakiplerUiState.Success(
                kullanicilar = currentTakipciler.toList(),
                isLastPage = isTakipcilerLastPage,
                isLoadingMore = false
            )
            return
        }

        // 2. Sayfa Sonu Kontrolü
        if (isNextPage && isTakipcilerLastPage) return

        isLoadingTakipciler = true

        viewModelScope.launch {
            try {
                if (!isNextPage) {
                    lastTakipciDoc = null
                    isTakipcilerLastPage = false

                    // Swipe-to-refresh yapılmıyorsa ve ilk açılışsa Shimmer göster
                    if (!isRefresh) {
                        _takipcilerState.value = TakiplerUiState.Loading
                    }
                } else {
                    // Alt tarafa yeni eleman eklenirken sayfalama yükleniyor moduna geç
                    _takipcilerState.value = TakiplerUiState.Success(
                        kullanicilar = currentTakipciler.toList(),
                        isLastPage = false,
                        isLoadingMore = true
                    )
                }

                repository.getTakipciler(
                    userId = userId,
                    limit = PAGE_LIMIT,
                    lastDocument = lastTakipciDoc
                ).onSuccess { result ->
                    isTakipcilerLastPage = result.isLastPage
                    lastTakipciDoc = result.lastDocument

                    if (!isNextPage) {
                        currentTakipciler.clear()
                    }
                    currentTakipciler.addAll(result.items)

                    _takipcilerState.value = TakiplerUiState.Success(
                        kullanicilar = currentTakipciler.toList(),
                        isLastPage = isTakipcilerLastPage,
                        isLoadingMore = false
                    )
                }.onFailure { exception ->
                    if (!isNextPage && !isRefresh && currentTakipciler.isEmpty()) {
                        _takipcilerState.value = TakiplerUiState.Error(
                            exception.localizedMessage ?: "Takipçiler yüklenirken hata oluştu."
                        )
                    } else {
                        _takipcilerState.value = TakiplerUiState.Success(
                            kullanicilar = currentTakipciler.toList(),
                            isLastPage = isTakipcilerLastPage,
                            isLoadingMore = false
                        )
                    }
                }
            } finally {
                isLoadingTakipciler = false
            }
        }
    }

    // ==========================================
    // TAKİP EDİLENLERİ GETİR (FETCH FOLLOWING)
    // ==========================================
    fun fetchTakipEdilenler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false
    ) {
        if (userId.isNullOrBlank() || isLoadingTakipEdilenler) return

        // 1. Sekme Değişimi Kontrolü: Liste zaten RAM'de varsa istek atma
        if (!isNextPage && !isRefresh && currentTakipEdilenler.isNotEmpty()) {
            _takipEdilenlerState.value = TakiplerUiState.Success(
                kullanicilar = currentTakipEdilenler.toList(),
                isLastPage = isTakipEdilenlerLastPage,
                isLoadingMore = false
            )
            return
        }

        // 2. Sayfa Sonu Kontrolü
        if (isNextPage && isTakipEdilenlerLastPage) return

        isLoadingTakipEdilenler = true

        viewModelScope.launch {
            try {
                if (!isNextPage) {
                    lastTakipEdilenDoc = null
                    isTakipEdilenlerLastPage = false

                    if (!isRefresh) {
                        _takipEdilenlerState.value = TakiplerUiState.Loading
                    }
                } else {
                    _takipEdilenlerState.value = TakiplerUiState.Success(
                        kullanicilar = currentTakipEdilenler.toList(),
                        isLastPage = false,
                        isLoadingMore = true
                    )
                }

                repository.getTakipEdilenler(
                    userId = userId,
                    limit = PAGE_LIMIT,
                    lastDocument = lastTakipEdilenDoc
                ).onSuccess { result ->
                    isTakipEdilenlerLastPage = result.isLastPage
                    lastTakipEdilenDoc = result.lastDocument

                    if (!isNextPage) {
                        currentTakipEdilenler.clear()
                    }
                    currentTakipEdilenler.addAll(result.items)

                    _takipEdilenlerState.value = TakiplerUiState.Success(
                        kullanicilar = currentTakipEdilenler.toList(),
                        isLastPage = isTakipEdilenlerLastPage,
                        isLoadingMore = false
                    )
                }.onFailure { exception ->
                    if (!isNextPage && !isRefresh && currentTakipEdilenler.isEmpty()) {
                        _takipEdilenlerState.value = TakiplerUiState.Error(
                            exception.localizedMessage ?: "Takip edilenler yüklenirken hata oluştu."
                        )
                    } else {
                        _takipEdilenlerState.value = TakiplerUiState.Success(
                            kullanicilar = currentTakipEdilenler.toList(),
                            isLastPage = isTakipEdilenlerLastPage,
                            isLoadingMore = false
                        )
                    }
                }
            } finally {
                isLoadingTakipEdilenler = false
            }
        }
    }
}