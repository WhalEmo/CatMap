package com.beem.catmap.ui.markersclick

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.Maps.mapkedi.Kediler
import com.beem.catmap.Profil.Gonderiler.CacheHelperGonderiBegeni
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.repository.CatRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class CatDetailViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CatRepository()

    private val _selectedCat = MutableStateFlow<Kediler?>(null)
    val selectedCat: StateFlow<Kediler?> = _selectedCat.asStateFlow()

    private val _likeCount = MutableStateFlow(0)
    val likeCount: StateFlow<Int> = _likeCount.asStateFlow()

    private val _isLiked = MutableStateFlow(false)
    val isLiked: StateFlow<Boolean> = _isLiked.asStateFlow()
    private val _ownerInfo = MutableStateFlow<Pair<String, String?>?>(null)
    val ownerInfo = _ownerInfo.asStateFlow()

    private val _isMyCat = MutableStateFlow(false)
    val isMyCat = _isMyCat.asStateFlow()

    private val _catDeletedSuccess = MutableSharedFlow<Boolean>()
    val catDeletedSuccess = _catDeletedSuccess.asSharedFlow()
    private val _postCount = MutableStateFlow(0)
    val postCount: StateFlow<Int> = _postCount.asStateFlow()

    private val _postsList = MutableStateFlow<List<Gonderi>>(emptyList())
    val postsList: StateFlow<List<Gonderi>> = _postsList.asStateFlow()

    private val _isAlreadyAdded = MutableStateFlow(false)
    val isAlreadyAdded: StateFlow<Boolean> = _isAlreadyAdded.asStateFlow()

    fun setCatData(cat: Kediler) {
        _selectedCat.value = cat

        val liked = CacheHelperGonderiBegeni.getInstance().begenmisMi(cat.id)
        _isLiked.value = liked

        fetchLikeCount(cat.id)
    }

    private fun fetchLikeCount(catId: String) {
        viewModelScope.launch {
            val count = repository.getCatLikeCount(catId)
            _likeCount.value = count.toInt()
        }
    }

    fun toggleLike() {
        val currentCat = _selectedCat.value ?: return
        val userId = UserSession.userId ?: return
        val currentlyLiked = _isLiked.value

        if (currentlyLiked) {
            _isLiked.value = false
            _likeCount.value = (_likeCount.value - 1).coerceAtLeast(0)

            viewModelScope.launch {
                val success = repository.removeLike(userId, currentCat.id)
                if (!success) {
                    _isLiked.value = true
                    _likeCount.value += 1
                }
            }
        } else {
            _isLiked.value = true
            _likeCount.value += 1

            viewModelScope.launch {
                val success = repository.addLike(userId, currentCat.id)
                if (!success) {
                    _isLiked.value = false
                    _likeCount.value -= 1
                }
            }
        }
    }

    fun markAsAddedLocal() {
        _isAlreadyAdded.value = true
    }

    fun loadOwnerInfo(ownerId: String) {
        viewModelScope.launch {
            val currentUserId = UserSession.userId
            _isMyCat.value = (ownerId == currentUserId)

            val userData = repository.getUserInfo(ownerId)
            userData?.let { data ->
                val username = data["KullaniciAdi"] as? String ?: "Bilinmeyen"
                val photoUrl = data["profilFotoUrl"] as? String
                _ownerInfo.value = Pair(username, photoUrl)

                val currentCatId = _selectedCat.value?.id
                if (currentCatId != null) {
                    val posts = data["GonderilenKediler"] as? List<Map<String, Any>>
                    val exists = posts?.any { it["kediID"] == currentCatId } ?: false
                    _isAlreadyAdded.value = exists
                }
            }
        }
    }

    fun deleteCatFromUserAndMap() {
        val currentCat = _selectedCat.value ?: return
        val userId = UserSession.userId ?: return

        viewModelScope.launch {
            val isRemovedFromUser = repository.removeCatFromUserPosts(userId, currentCat.id)

            val isDeletedFromMap = repository.deleteCatFromMap(currentCat.id)

            if (isRemovedFromUser || isDeletedFromMap) {
                val updatedList = _postsList.value.filterNot { it.kediID == currentCat.id }
                _postsList.value = updatedList
                _postCount.value = (_postCount.value - 1).coerceAtLeast(0)

                ProfileEventBus.emitEvent(
                    event = ProfileEvent.PostDeleted(
                        catId = currentCat.id
                    )
                )
                CatEventBus.emitEvent(
                    event = CatMapEvent.Deleted(
                        catId = currentCat.id
                    )
                )

                _catDeletedSuccess.emit(true)
            }
        }
    }

    fun deleteCatFromMap() {
        val catId = _selectedCat.value?.id ?: return
        viewModelScope.launch {
            val success = repository.deleteCatFromMap(catId)
            if (success) {
                _catDeletedSuccess.emit(true)
            }
        }
    }

}