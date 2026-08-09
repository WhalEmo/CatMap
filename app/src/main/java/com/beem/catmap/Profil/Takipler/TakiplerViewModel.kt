package com.beem.catmap.Profil.Takipler

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TakiplerViewModel() : ViewModel() {
    private val repository: TakiplerRepository = TakiplerRepository.getInstance()
    private val PAGE_LIMIT = 10L
    private var targetUserId: String? = null
    val isMyOwnList: Boolean
        get() = targetUserId == UserSession.userId

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

    init {
        observeEvents()
    }private fun observeEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                val currentUserId = UserSession.userId
                when (event) {
                    is ProfileEvent.FollowUser -> {
                        if (event.operatorUserId == currentUserId && isMyOwnList) {
                            yeniTakipEdilenEkle(
                                userId = event.userId,
                                kullaniciAdi = event.kullaniciAdi,
                                fotoUrl = event.fotoUrl
                            )
                        }
                    }
                    is ProfileEvent.UnFollowUser -> {
                        if (event.operatorUserId == currentUserId && isMyOwnList) {
                            takipEdilenCikar(event.userId)
                        }
                    }
                    is ProfileEvent.UnFollowerUser -> {
                        if (event.operatorUserId == currentUserId && isMyOwnList) {
                            takipciCikar(event.userId)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    fun yeniTakipEdilenEkle(userId: String, kullaniciAdi: String, fotoUrl: String) {
        if (currentTakipEdilenler.none { it.id == userId }) {
            val yeniKullanici = Kullanici().apply {
                id = userId
                this.kullaniciAdi = kullaniciAdi
                this.fotoUrl = fotoUrl
                takipEdiyorMuyum = 2
            }
            currentTakipEdilenler.add(0, yeniKullanici)
            _takipEdilenlerState.value = TakiplerUiState.Success(
                kullanicilar = currentTakipEdilenler.toList(),
                isLastPage = isTakipEdilenlerLastPage,
                isLoadingMore = false
            )
        }
    }
    fun takipEdilenCikar(userId: String) {
        val removed = currentTakipEdilenler.removeAll { it.id == userId }
        if (removed) {
            _takipEdilenlerState.value = TakiplerUiState.Success(
                kullanicilar = currentTakipEdilenler.toList(),
                isLastPage = isTakipEdilenlerLastPage,
                isLoadingMore = false
            )
        }
    }

    fun takipciCikar(userId: String) {
        val removed = currentTakipciler.removeAll { it.id == userId }
        if (removed) {
            _takipcilerState.value = TakiplerUiState.Success(
                kullanicilar = currentTakipciler.toList(),
                isLastPage = isTakipcilerLastPage,
                isLoadingMore = false
            )
        }
    }
    fun fetchTakipciler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false
    ) {
        if (userId.isNullOrBlank() || isLoadingTakipciler) return
        this.targetUserId = userId

        if (!isNextPage && !isRefresh && currentTakipciler.isNotEmpty()) {
            _takipcilerState.value = TakiplerUiState.Success(
                kullanicilar = currentTakipciler.toList(),
                isLastPage = isTakipcilerLastPage,
                isLoadingMore = false
            )
            return
        }
        if (isNextPage && isTakipcilerLastPage) return
        isLoadingTakipciler = true

        viewModelScope.launch {
            try {
                if (!isNextPage) {
                    lastTakipciDoc = null
                    isTakipcilerLastPage = false

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
                    forceRefresh = isRefresh
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
    fun fetchTakipEdilenler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false
    ) {
        if (userId.isNullOrBlank() || isLoadingTakipEdilenler) return
        this.targetUserId = userId

        if (!isNextPage && !isRefresh && currentTakipEdilenler.isNotEmpty()) {
            _takipEdilenlerState.value = TakiplerUiState.Success(
                kullanicilar = currentTakipEdilenler.toList(),
                isLastPage = isTakipEdilenlerLastPage,
                isLoadingMore = false
            )
            return
        }
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
                    lastDocument = lastTakipEdilenDoc,
                    forceRefresh = isRefresh
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