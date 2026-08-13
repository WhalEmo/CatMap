package com.beem.catmap.ui.profile.follow.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.profile.follow.state.FollowUiState
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FollowListViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FollowRepository = FollowRepository.getInstance(application)
    private val PAGE_LIMIT = 10L
    private var targetUserId: String? = null
    val isMyOwnList: Boolean
        get() = targetUserId == UserSession.userId

    private val _followersState = MutableStateFlow<FollowUiState>(FollowUiState.Idle)//takıpcıler
    val followersState: StateFlow<FollowUiState> = _followersState.asStateFlow()

    private val currentFollowers = mutableListOf<UserModel>()
    private var lastFollowersDoc: DocumentSnapshot? = null
    private var isFollowersLastPage = false
    private var isLoadingFollowers = false

    private val _followingState = MutableStateFlow<FollowUiState>(FollowUiState.Idle)//takp edılenler
    val followingState: StateFlow<FollowUiState> = _followingState.asStateFlow()

    private val currentFollowing = mutableListOf<UserModel>()
    private var lastFollowingDoc: DocumentSnapshot? = null
    private var isFollowingLastPage = false
    private var isLoadingFollowing = false

    init {
        observeEvents()
    }
    private fun observeEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                val currentUserId = UserSession.userId
                when (event) {
                    is ProfileEvent.FollowUser -> {
                        if (event.operatorUserId == currentUserId) {
                            repository.clearUserCache(currentUserId)
                            if (isMyOwnList) {
                                yeniTakipEdilenEkle(
                                    userId = event.userId,
                                    kullaniciAdi = event.kullaniciAdi,
                                    fotoUrl = event.fotoUrl
                                )
                            }
                        }
                    }
                    is ProfileEvent.UnFollowUser -> {
                        if (event.operatorUserId == currentUserId) {
                            repository.clearUserCache(currentUserId)
                            if (isMyOwnList) {
                                takipEdilenCikar(event.userId)
                            }
                        }
                    }
                    is ProfileEvent.UnFollowerUser -> {
                        if (event.operatorUserId == currentUserId) {
                            repository.clearUserCache(currentUserId)
                            if (isMyOwnList) {
                                takipciCikar(event.userId)
                            }
                        }
                    }
                    is ProfileEvent.BlockedUser -> {
                        if (event.operatorUserId == currentUserId && isMyOwnList) {
                            takipEdilenCikar(event.userId)
                            takipciCikar(event.userId)
                        }
                    }
                    else -> {}
                }
            }
        }
    }
    fun yeniTakipEdilenEkle(userId: String, kullaniciAdi: String, fotoUrl: String) {
        if (currentFollowing.none { it.id == userId }) {
            val yeniUserModel = UserModel().apply {
                id = userId
                this.username = kullaniciAdi
                this.photoUrl = fotoUrl
                isFollowing = 2
            }
            currentFollowing.add(0, yeniUserModel)
            _followingState.value = FollowUiState.Success(
                userModels = currentFollowing.toList(),
                isLastPage = isFollowingLastPage,
                isLoadingMore = false
            )
        }
    }
    fun takipEdilenCikar(userId: String) {
        val removed = currentFollowing.removeAll { it.id == userId }
        if (removed) {
            _followingState.value = FollowUiState.Success(
                userModels = currentFollowing.toList(),
                isLastPage = isFollowingLastPage,
                isLoadingMore = false
            )
        }
    }

    fun takipciCikar(userId: String) {
        val removed = currentFollowers.removeAll { it.id == userId }
        if (removed) {
            _followersState.value = FollowUiState.Success(
                userModels = currentFollowers.toList(),
                isLastPage = isFollowersLastPage,
                isLoadingMore = false
            )
        }
    }
    fun fetchTakipciler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false
    ) {
        if (userId.isNullOrBlank() || isLoadingFollowers) return
        this.targetUserId = userId

        if (!isNextPage && !isRefresh && currentFollowers.isNotEmpty()) {
            _followersState.value = FollowUiState.Success(
                userModels = currentFollowers.toList(),
                isLastPage = isFollowersLastPage,
                isLoadingMore = false
            )
            return
        }
        if (isNextPage && isFollowersLastPage) return
        isLoadingFollowers = true

        viewModelScope.launch {
            try {
                if (!isNextPage) {
                    lastFollowersDoc = null
                    isFollowersLastPage = false

                    if (!isRefresh) {
                        _followersState.value = FollowUiState.Loading
                    }
                } else {
                    _followersState.value = FollowUiState.Success(
                        userModels = currentFollowers.toList(),
                        isLastPage = false,
                        isLoadingMore = true
                    )
                }

                repository.getTakipciler(
                    userId = userId,
                    limit = PAGE_LIMIT,
                    lastDocument = lastFollowersDoc,
                    forceRefresh = isRefresh
                ).onSuccess { result ->
                    isFollowersLastPage = result.isLastPage
                    lastFollowersDoc = result.lastDocument

                    if (!isNextPage) {
                        currentFollowers.clear()
                    }
                    currentFollowers.addAll(result.items)

                    _followersState.value = FollowUiState.Success(
                        userModels = currentFollowers.toList(),
                        isLastPage = isFollowersLastPage,
                        isLoadingMore = false
                    )
                }.onFailure { exception ->
                    if (!isNextPage && !isRefresh && currentFollowers.isEmpty()) {
                        _followersState.value = FollowUiState.Error(
                            exception.localizedMessage ?: "Takipçiler yüklenirken hata oluştu."
                        )
                    } else {
                        _followersState.value = FollowUiState.Success(
                            userModels = currentFollowers.toList(),
                            isLastPage = isFollowersLastPage,
                            isLoadingMore = false
                        )
                    }
                }
            } finally {
                isLoadingFollowers = false
            }
        }
    }
    fun fetchTakipEdilenler(
        userId: String?,
        isNextPage: Boolean = false,
        isRefresh: Boolean = false
    ) {
        if (userId.isNullOrBlank() || isLoadingFollowing) return
        this.targetUserId = userId

        if (!isNextPage && !isRefresh && currentFollowing.isNotEmpty()) {
            _followingState.value = FollowUiState.Success(
                userModels = currentFollowing.toList(),
                isLastPage = isFollowingLastPage,
                isLoadingMore = false
            )
            return
        }
        if (isNextPage && isFollowingLastPage) return
        isLoadingFollowing = true

        viewModelScope.launch {
            try {
                if (!isNextPage) {
                    lastFollowingDoc = null
                    isFollowingLastPage = false

                    if (!isRefresh) {
                        _followingState.value = FollowUiState.Loading
                    }
                } else {
                    _followingState.value = FollowUiState.Success(
                        userModels = currentFollowing.toList(),
                        isLastPage = false,
                        isLoadingMore = true
                    )
                }

                repository.getTakipEdilenler(
                    userId = userId,
                    limit = PAGE_LIMIT,
                    lastDocument = lastFollowingDoc,
                    forceRefresh = isRefresh
                ).onSuccess { result ->
                    isFollowingLastPage = result.isLastPage
                    lastFollowingDoc = result.lastDocument

                    if (!isNextPage) {
                        currentFollowing.clear()
                    }
                    currentFollowing.addAll(result.items)

                    _followingState.value = FollowUiState.Success(
                        userModels = currentFollowing.toList(),
                        isLastPage = isFollowingLastPage,
                        isLoadingMore = false
                    )
                }.onFailure { exception ->
                    if (!isNextPage && !isRefresh && currentFollowing.isEmpty()) {
                        _followingState.value = FollowUiState.Error(
                            exception.localizedMessage ?: "Takip edilenler yüklenirken hata oluştu."
                        )
                    } else {
                        _followingState.value = FollowUiState.Success(
                            userModels = currentFollowing.toList(),
                            isLastPage = isFollowingLastPage,
                            isLoadingMore = false
                        )
                    }
                }
            } finally {
                isLoadingFollowing = false
            }
        }
    }
}