package com.beem.catmap.ui.profile_v2.myprofile

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.Post
import com.beem.catmap.data.model.UserProfileData
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.managers.OnlinePresenceManager
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyProfileViewModel(application: Application) : AndroidViewModel(application) {

    private val firestore = FirebaseFirestore.getInstance()
    private val postRepository = PostRepository.getInstance(application)
    private val userManager = CurrentUserManager.getInstance(application)

    private val _uiState = MutableStateFlow(MyProfileUiState(isLoading = true))
    val uiState: StateFlow<MyProfileUiState> = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyProfileEvent>()
    val event = _eventChannel.receiveAsFlow()

    private var lastPostDocument: DocumentSnapshot? = null

    private val currentUserId: String
        get() = UserSession.userId.orEmpty()

    init {
        loadMyProfile()
        loadPosts(isRefresh = false)
        observeProfileEvents()
        observeSessionState()
    }


    private fun observeSessionState() {
        viewModelScope.launch {
            userManager.profileState.collect { sessionState ->
                _uiState.update { current ->
                    if (current.user != null) {
                        current.copy(
                            user = current.user.copy(
                                followersCount = sessionState.followersCount,
                                followingCount = sessionState.followingCount,
                                postCount = sessionState.postCount
                            )
                        )
                    } else current
                }
            }
        }
    }

    /**
     * Kedi eklendiğinde veya silindiğinde UI State'i ve sayaçları anında günceller.
     */
    private fun observeProfileEvents() {
        viewModelScope.launch {
            ProfileEventBus.profileEvent.collect { event ->
                when (event) {
                    is ProfileEvent.PostAdded -> {
                        _uiState.update { currentState ->
                            if (currentState.posts.any { it.catId == event.post.catId }) {
                                return@update currentState
                            }

                            val updatedPosts = listOf(event.post) + currentState.posts
                            val updatedUser = currentState.user?.copy(
                                postCount = currentState.user.postCount + 1
                            )

                            currentState.copy(
                                posts = updatedPosts,
                                user = updatedUser
                            )
                        }
                    }

                    is ProfileEvent.PostDeleted -> {
                        _uiState.update { currentState ->
                            val updatedPosts = currentState.posts.filterNot { it.catId == event.catId }
                            val updatedUser = currentState.user?.copy(
                                postCount = (currentState.user.postCount - 1).coerceAtLeast(0L)
                            )
                            currentState.copy(
                                posts = updatedPosts,
                                user = updatedUser
                            )
                        }
                    }

                    is ProfileEvent.ProfileUpdated -> {
                        event.updatedUserModel?.let { updated ->
                            _uiState.update { currentState ->
                                currentState.copy(
                                    user = currentState.user?.copy(
                                        name = updated.name,
                                        surname = updated.surname,
                                        username = updated.username,
                                        bio = updated.bio,
                                        photoUrl = updated.photoUrl
                                    )
                                )
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    /**
     * Kullanıcı profil bilgilerini Firestore'dan çeker.
     */
    fun loadMyProfile(isRefresh: Boolean = false) {
        val uid = currentUserId
        if (uid.isBlank()) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Oturum bulunamadı.") }
            return
        }

        viewModelScope.launch {
            if (isRefresh) {
                _uiState.update { it.copy(isRefreshing = true) }
            } else {
                _uiState.update { it.copy(isLoading = true) }
            }

            try {
                val snapshot = firestore.collection("users").document(uid).get().await()
                val profileData = snapshot.toObject(UserProfileData::class.java)

                if (profileData != null) {
                    syncWithLocalSession(profileData)
                    _uiState.update {
                        it.copy(
                            user = profileData,
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = null
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isRefreshing = false,
                            errorMessage = "Profil bilgisi bulunamadı."
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isRefreshing = false,
                        errorMessage = e.localizedMessage ?: "Profil yüklenemedi."
                    )
                }
            }
        }
    }

    /**
     * Kullanıcının yüklediği kedi gönderilerini PostRepository üzerinden çeker.
     */
    fun loadPosts(isRefresh: Boolean = false) {
        val uid = currentUserId
        if (uid.isBlank()) return

        if (isRefresh) {
            lastPostDocument = null
            postRepository.invalidateUserCache(uid)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isPostsLoading = true) }

            val result = postRepository.getUsersPost(
                userId = uid,
                lastDocument = null,
                forceRefresh = isRefresh
            )

            result.onSuccess { pageResult ->
                lastPostDocument = pageResult.lastDocument
                _uiState.update {
                    it.copy(
                        posts = pageResult.posts.distinctBy { post -> post.catId },
                        isPostsLoading = false,
                        isLastPage = pageResult.isLastPage
                    )
                }
            }.onFailure { error ->
                _uiState.update {
                    it.copy(
                        isPostsLoading = false,
                        errorMessage = error.localizedMessage
                    )
                }
            }
        }
    }

    /**
     * Sayfalama (Pagination) ile daha fazla gönderi çeker.
     */
    fun loadMorePosts() {
        val uid = currentUserId
        val currentState = _uiState.value

        if (currentState.isMoreLoading || currentState.isLastPage || currentState.isPostsLoading || uid.isBlank()) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isMoreLoading = true) }

            val result = postRepository.getUsersPost(
                userId = uid,
                lastDocument = lastPostDocument,
                forceRefresh = false
            )

            result.onSuccess { pageResult ->
                lastPostDocument = pageResult.lastDocument
                _uiState.update { current ->
                    val combined = (current.posts + pageResult.posts).distinctBy { it.catId }
                    current.copy(
                        posts = combined,
                        isMoreLoading = false,
                        isLastPage = pageResult.isLastPage
                    )
                }
            }.onFailure {
                _uiState.update { it.copy(isMoreLoading = false) }
            }
        }
    }

    private fun syncWithLocalSession(data: UserProfileData) {
        userManager.updateProfileDetails(
            ad = data.name,
            soyad = data.surname,
            kullaniciAdi = data.username,
            takipci = data.followersCount,
            takipEdilen = data.followingCount,
            gonderiSayisi = data.postCount,
            biyografi = data.bio,
            fotoUrl = data.photoUrl
        )
    }

    fun logout() {
        viewModelScope.launch {
            try {
                OnlinePresenceManager.setUserOffline()
                UserSession.logout()
                _eventChannel.send(MyProfileEvent.NavigateToAuth)
            } catch (e: Exception) {
                _eventChannel.send(MyProfileEvent.ShowToast("Çıkış yapılırken bir hata oluştu."))
            }
        }
    }
}