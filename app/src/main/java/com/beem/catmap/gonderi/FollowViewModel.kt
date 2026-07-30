import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.gonderi.ProfileState
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class FollowViewModel(application: Application) : AndroidViewModel(application){
    private val repository: FollowRepository = FollowRepository()
    private val userManager = CurrentUserManager.getInstance(application)
    val profileState: StateFlow<ProfileState> = userManager.profileState

    private val _benimEngellediklerim = MutableLiveData<List<String>>(emptyList())
    val benimEngellediklerim: LiveData<List<String>> get() = _benimEngellediklerim

    private val _beniEngelleyenler = MutableLiveData<List<String>>(emptyList())
    val beniEngelleyenler: LiveData<List<String>> get() = _beniEngelleyenler

    private val _takipDurumu = MutableLiveData<Boolean>()
    val takipDurumu: LiveData<Boolean> get() = _takipDurumu

    private val _beniTakipEdiyor = MutableLiveData<Boolean>()
    val beniTakipEdiyor: LiveData<Boolean> get() = _beniTakipEdiyor

    private val _takipEdilenSayisi = MutableLiveData<Long>()
    val takipEdilenSayisi: LiveData<Long> get() = _takipEdilenSayisi

    private val _takipciSayisi = MutableLiveData<Long>()
    val takipciSayisi: LiveData<Long> get() = _takipciSayisi

    fun takipTakipciSayisiGetir(userId: String, context: Context) {
        viewModelScope.launch {
            val result = repository.fetchAndCacheFollowCounts(context, userId)

            result.onSuccess { counts ->
                _takipEdilenSayisi.postValue(counts.followingCount)
                _takipciSayisi.postValue(counts.followerCount)
            }.onFailure {
                // İsteğe bağlı olarak hata durumunda varsayılan değerler set edilebilir
                _takipEdilenSayisi.postValue(0L)
                _takipciSayisi.postValue(0L)
            }
        }

        // Takip Et
        fun takipEt(takipEttiginId: String, currentUserId: String) {
            viewModelScope.launch {
                val result = repository.takipet(
                    currentUserId = currentUserId,
                    targetUserId = takipEttiginId,
                    myBlockedList = _benimEngellediklerim.value,
                    blockedMeList = _beniEngelleyenler.value
                )

                result.onSuccess {
                    _takipDurumu.postValue(true)
                }.onFailure {
                    _takipDurumu.postValue(false)
                }
            }
        }

        // Takipten Çık (Unfollow)
        fun takiptenCikar(takiptenCiktiginId: String, currentUserId: String) {
            viewModelScope.launch {
                val result = repository.unfollowUser(
                    currentUserId = currentUserId,
                    targetUserId = takiptenCiktiginId
                )

                result.onSuccess {
                    _takipDurumu.postValue(false)
                }.onFailure {
                    // Hata durumunda UI gerektiği şekilde ele alınabilir
                }
            }
        }

        // Takipçiyi Çıkar (Remove Follower)
        fun takipcidenCikar(takipciId: String, currentUserId: String) {
            viewModelScope.launch {
                val result = repository.removeFollower(
                    currentUserId = currentUserId,
                    followerId = takipciId
                )

                result.onSuccess {
                    _beniTakipEdiyor.postValue(false)
                }.onFailure {
                    // Hata durumunda UI gerektiği şekilde ele alınabilir
                }
            }
        }
    }
}