package com.beem.catmap.Profil.Takipler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TakiplerViewModel(
    private val repository: TakiplerRepository = TakiplerRepository()
) : ViewModel() {

    private val PAGE_LIMIT = 10L
    private val _takipcilerState = MutableStateFlow<TakiplerUiState>(TakiplerUiState.Idle)
    val takipcilerState: StateFlow<TakiplerUiState> = _takipcilerState.asStateFlow()

    private val currentTakipciler = mutableListOf<Kullanici>()
    private var lastTakipciDoc: DocumentSnapshot? = null
    private var isTakipcilerLastPage = false
    private var isLoadingTakipciler = false

    private val _takipEdilenlerState = MutableStateFlow<TakiplerUiState>(TakiplerUiState.Idle)
    val takipEdilenlerState: StateFlow<TakiplerUiState> = _takipEdilenlerState.asStateFlow()

    private val currentTakipEdilenler = mutableListOf<Kullanici>()
    private var lastTakipEdilenDoc: DocumentSnapshot? = null
    private var isTakipEdilenlerLastPage = false
    private var isLoadingTakipEdilenler = false

    fun fetchTakipciler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false // <-- EKLENDİ
    ) {
        if (userId.isNullOrBlank() || isLoadingTakipciler) return
        if (isNextPage && isTakipcilerLastPage) return

        isLoadingTakipciler = true

        viewModelScope.launch {
            try {
                if (!isNextPage) {
                    currentTakipciler.clear()
                    lastTakipciDoc = null
                    isTakipcilerLastPage = false

                    // Ekran ilk defa açılıyorsa Shimmer göster, Refresh yapılıyorsa liste kalsın
                    if (!isRefresh) {
                        _takipcilerState.value = TakiplerUiState.Loading
                    }
                } else {
                    _takipcilerState.value = TakiplerUiState.Success(
                        kullanicilar = currentTakipciler.toList(),
                        isLastPage = false,
                        isLoadingMore = true
                    )
                }

                repository.getTakipciler(
                    userId = userId,
                    limit = PAGE_LIMIT,
                    lastDocument = lastTakipciDoc,
                    forceRefresh = isRefresh // <-- Repository'ye iletildi
                )
                    .onSuccess { result ->
                        isTakipcilerLastPage = result.isLastPage
                        lastTakipciDoc = result.lastDocument

                        if (!isNextPage) {
                            currentTakipciler.clear()
                            currentTakipciler.addAll(result.items)
                        } else {
                            currentTakipciler.addAll(result.items)
                        }

                        _takipcilerState.value = TakiplerUiState.Success(
                            kullanicilar = currentTakipciler.toList(),
                            isLastPage = isTakipcilerLastPage,
                            isLoadingMore = false
                        )
                    }
                    .onFailure { exception ->
                        if (!isNextPage && !isRefresh) {
                            _takipcilerState.value =
                                TakiplerUiState.Error(exception.localizedMessage ?: "Hata oluştu")
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

    fun fetchTakipEdilenler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false // <-- EKLENDİ
    ) {
        if (userId.isNullOrBlank() || isLoadingTakipEdilenler) return
        if (isNextPage && isTakipEdilenlerLastPage) return

        isLoadingTakipEdilenler = true

        viewModelScope.launch {
            try {
                if (!isNextPage) {
                    currentTakipEdilenler.clear()
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
                    lastDocument = lastTakipEdilenDoc,
                    forceRefresh = isRefresh
                )
                    .onSuccess { result ->
                        isTakipEdilenlerLastPage = result.isLastPage
                        lastTakipEdilenDoc = result.lastDocument

                        if (!isNextPage) {
                            currentTakipEdilenler.clear()
                            currentTakipEdilenler.addAll(result.items)
                        } else {
                            currentTakipEdilenler.addAll(result.items)
                        }

                        _takipEdilenlerState.value = TakiplerUiState.Success(
                            kullanicilar = currentTakipEdilenler.toList(),
                            isLastPage = isTakipEdilenlerLastPage,
                            isLoadingMore = false
                        )
                    }
                    .onFailure { exception ->
                        if (!isNextPage && !isRefresh) {
                            _takipEdilenlerState.value =
                                TakiplerUiState.Error(exception.localizedMessage ?: "Hata oluştu")
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