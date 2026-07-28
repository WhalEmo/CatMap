package com.beem.catmap.Maps.markersclick.comments

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.YorumYanit.CacheHelperYorum
import com.beem.catmap.YorumYanit.Yanit_Model
import com.beem.catmap.YorumYanit.Yorum_Model
import com.beem.catmap.data.repository.UserRepository
import com.beem.catmap.yorumyanit.data.repository.YanitRepository
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CommentViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = CommentsRepo()
    private val yanitRepository = YanitRepository()
    private val userRepository = UserRepository.getInstance(application)

    private val _comments = MutableStateFlow<List<Yorum_Model>>(emptyList())
    val comments: StateFlow<List<Yorum_Model>> = _comments.asStateFlow()

    private val yanitCacheMap = mutableMapOf<String, MutableList<Yanit_Model>>()
    private val yanitLastDocMap = mutableMapOf<String, DocumentSnapshot?>()
    private val yanitDahaFazlaMap = mutableMapOf<String, Boolean>()

    private val _isLoading = MutableStateFlow(true)
    val isLoading = _isLoading.asStateFlow()

    private val _isEmpty = MutableStateFlow(false)
    val isEmpty = _isEmpty.asStateFlow()

    private val _actionSuccess = MutableSharedFlow<Boolean>()
    val actionSuccess = _actionSuccess.asSharedFlow()

    private val _commentCount = MutableStateFlow(0)
    val commentCount: StateFlow<Int> = _commentCount.asStateFlow()

    private val _begenilenYorumIDs = MutableStateFlow<Set<String>>(emptySet())
    val begenilenYorumIDs: StateFlow<Set<String>> = _begenilenYorumIDs.asStateFlow()

    private val _hataMesaji = MutableSharedFlow<String>()
    val hataMesaji: SharedFlow<String> = _hataMesaji.asSharedFlow()

    private val _begenilenYanitIDs = MutableStateFlow<Set<String>>(emptySet())
    val begenilenYanitIDs: StateFlow<Set<String>> = _begenilenYanitIDs.asStateFlow()

    private val _isPaginationLoading = MutableStateFlow(false)
    val isPaginationLoading: StateFlow<Boolean> = _isPaginationLoading.asStateFlow()
    private val yanitYuklendiMap = HashMap<String, Boolean>()

    init {
        _begenilenYorumIDs.value = CacheHelperYorum.loadBegenilenSet(getApplication()) ?: emptySet()
        _begenilenYanitIDs.value = CacheHelperYorum.loadBegenilenYanitSet(getApplication()) ?: emptySet()
    }

    private var catId: String = ""
    private var lastVisibleDoc: DocumentSnapshot? = null
    private var isLastPage = false
    private val pageSize = 10L

    fun initCatId(catId: String) {
        if (this.catId == catId) return
        this.catId = catId
        fetchInitialComments()
    }

    private fun applyLikedState(comments: List<Yorum_Model>): List<Yorum_Model> {
        val likedSet = _begenilenYorumIDs.value
        comments.forEach { yorum ->
            yorum.setBegenildiMi(yorum.yorumID != null && likedSet.contains(yorum.yorumID))
        }
        return comments
    }

    private fun applyLikedReplyState(replies: List<Yanit_Model>): List<Yanit_Model> {
        val likedSet = _begenilenYanitIDs.value
        replies.forEach { yanit ->
            yanit.setBegenildiMi(yanit.yanitId != null && likedSet.contains(yanit.yanitId))
        }
        return replies
    }

    fun fetchInitialComments() {
        if (catId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            isLastPage = false
            lastVisibleDoc = null

            val (initialList, lastDoc) = repository.getInitialComments(catId, pageSize)
            val processedList = applyLikedState(initialList)

            _comments.value = processedList
            lastVisibleDoc = lastDoc
            _isEmpty.value = processedList.isEmpty()
            if (processedList.size < pageSize) {
                isLastPage = true
            }
            _isLoading.value = false
        }
    }

    fun loadMoreComments() {
        if (_isLoading.value || _isPaginationLoading.value || isLastPage || catId.isEmpty()) return
        val currentLastDoc = lastVisibleDoc ?: return

        viewModelScope.launch {
            _isPaginationLoading.value = true
            val (newComments, newLastDoc) = repository.loadMoreComments(catId, currentLastDoc, pageSize)

            if (newComments.isNotEmpty()) {
                val processedNewComments = applyLikedState(newComments)
                _comments.update { current ->
                    current.toMutableList().apply { addAll(processedNewComments) }
                }
                lastVisibleDoc = newLastDoc
                if (newComments.size < pageSize) isLastPage = true
            } else {
                isLastPage = true
            }
            _isPaginationLoading.value = false
        }
    }

    fun toggleYanitlarGorunurluk(yorumId: String) {
        _comments.update { current ->
            current.map { yorum ->
                if (yorum.yorumID == yorumId) {
                    yorum.copy().apply {
                        val yeniDurum = !isYanitlarGorunuyor

                        if (!yeniDurum) {
                            setYanitlarGorunuyor(false)

                            val liste = yanitCacheMap[yorumId]

                            liste?.removeAll {
                                it.isLocalOnly && !it.isSending
                            }

                            setYanitlar(ArrayList(liste ?: emptyList()))
                        }
                        setYanitlarGorunuyor(yeniDurum)
                        val yuklendiMi = yanitYuklendiMap[yorumId] ?: false
                        if (yeniDurum && !yuklendiMi) {
                            yanitlariYukle(yorumId, 3, true)
                        }
                    }
                } else {
                    yorum
                }
            }
        }
    }

    fun yanitlariYukle(yorumId: String, limit: Int, clearList: Boolean) {
        if (catId.isEmpty() || yorumId.isEmpty()) return

        val lastDoc = if (clearList) null else yanitLastDocMap[yorumId]

        viewModelScope.launch {
            val result = yanitRepository.yanitlariCek(catId, yorumId, limit, lastDoc)

            result.onSuccess { (yeniYanitlar, yeniLastDoc) ->
                yanitYuklendiMap[yorumId] = true
                val processedReplies = applyLikedReplyState(yeniYanitlar)
                yanitLastDocMap[yorumId] = yeniLastDoc

                val liste = yanitCacheMap.getOrPut(yorumId) { mutableListOf() }

                if (clearList) {
                    //val lokalYanitlar = liste.filter { it.isLocalOnly }
                    liste.clear()
                    //liste.addAll(lokalYanitlar)
                }
                processedReplies.forEach { firebaseYanit ->
                    val varMi = liste.any {
                        it.yanitId == firebaseYanit.yanitId
                    }
                    if (!varMi) {
                        liste.add(firebaseYanit)
                    }
                }
                val dahaFazlaVar = processedReplies.size >= limit
                yanitDahaFazlaMap[yorumId] = dahaFazlaVar

                val mevcutDurum = _comments.value
                    .find { it.yorumID == yorumId }
                    ?.isYanitlarGorunuyor ?: false

                updateCommentYanitState(
                    yorumId = yorumId,
                    yeniYanitlar = liste,
                    gorunur = mevcutDurum,
                    dahaFazla = dahaFazlaVar
                )
            }.onFailure {
                _hataMesaji.emit("Yanıtlar yüklenirken hata oluştu.")
            }
        }
    }

    private fun updateCommentYanitState(
        yorumId: String,
        yeniYanitlar: List<Yanit_Model>,
        gorunur: Boolean? = null,
        dahaFazla: Boolean
    ) {
        _comments.update { currentList ->
            val index = currentList.indexOfFirst { it.yorumID == yorumId }
            if (index == -1) return@update currentList
            currentList.toMutableList().apply {
                val eskiYorum = this[index]
                this[index] = eskiYorum.copy().apply {
                    setYanitlar(ArrayList(yeniYanitlar))
                    setYanitlarGorunuyor(gorunur ?: eskiYorum.isYanitlarGorunuyor())
                    setDahafazlaGozukuyorMu(dahaFazla)
                }
            }
        }
    }

    fun updateYanit(yorumId: String, yanitId: String, yeniIcerik: String) {
        if (catId.isEmpty() || yorumId.isEmpty() || yanitId.isEmpty() || yeniIcerik.trim().isEmpty()) return

        viewModelScope.launch {
            val success = yanitRepository.yanitGuncelle(catId, yorumId, yanitId, yeniIcerik)
            if (success) {
                val eskiListe = yanitCacheMap[yorumId] ?: mutableListOf()
                val index = eskiListe.indexOfFirst { it.yanitId == yanitId }
                if (index != -1) {
                    val yeniYanit = eskiListe[index].copy().apply {
                        setYaniticerik(yeniIcerik)
                    }
                    val guncelListe = eskiListe.toMutableList()
                    guncelListe[index] = yeniYanit

                    yanitCacheMap[yorumId] = guncelListe
                    val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false

                    updateCommentYanitState(yorumId, guncelListe, null, dahaFazla)
                }
            } else {
                _hataMesaji.emit("Yanıt güncellenemedi.")
            }
        }
    }

    fun deleteYanit(yorumId: String, yanitId: String) {
        if (catId.isEmpty() || yorumId.isEmpty() || yanitId.isEmpty()) return

        viewModelScope.launch {
            val success = yanitRepository.yanitSil(catId, yorumId, yanitId)
            if (success) {
                val liste = yanitCacheMap[yorumId]
                liste?.removeAll { it.yanitId == yanitId }

                val guncelYanitlar = liste ?: emptyList()
                val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false

                updateCommentYanitState(yorumId, guncelYanitlar, null, dahaFazla)
            } else {
                _hataMesaji.emit("Yanıt silinemedi.")
            }
        }
    }

    fun sendComment(content: String) {
        if (content.trim().isEmpty() || catId.isEmpty()) return
        val currentUser = userRepository.getCurrentUser() ?: return

        val currentUserId = userRepository.getCurrentUserId() ?: ""
        val username = currentUser.kullaniciAdi ?: ""

        val tempId = "temp_${System.currentTimeMillis()}"

        viewModelScope.launch {
            val yeniYorum = Yorum_Model().apply {
                this.yorumID = tempId
                this.yorumicerik = content
                this.kullaniciAdi = username
                this.yukleyenId = currentUserId
                this.tarih = java.util.Date()
                this.begeniSayisi = 0
                this.isBegenildiMi = false
                this.isYanitlarGorunuyor = false
                this.yanitlar = ArrayList()
                this.sending = true
            }

            _comments.update { current ->
                current.toMutableList().apply { add(0, yeniYorum) }
            }
            _isEmpty.value = false

            val realCommentId = repository.addComment(catId, content, username, currentUserId)

            if (realCommentId != null) {
                _actionSuccess.emit(true)
                _commentCount.value = _commentCount.value + 1

                _comments.update { current ->
                    current.map { yorum ->
                        if (yorum.yorumID == tempId) {
                            yorum.copy().apply {
                                this.yorumID = realCommentId
                                this.sending = false
                            }
                        } else {
                            yorum
                        }
                    }
                }
            } else {
                _comments.update { current ->
                    current.toMutableList().apply { removeAll { it.yorumID == tempId } }
                }
                _isEmpty.value = _comments.value.isEmpty()
                _hataMesaji.emit("Yorum gönderilemedi.")
            }
        }
    }
    fun sendReply(commentId: String, content: String, onComplete: (String?) -> Unit) {
        if (content.trim().isEmpty() || catId.isEmpty()) {
            onComplete(null)
            return
        }
        val currentUser = userRepository.getCurrentUser() ?: run {
            onComplete(null)
            return
        }

        val currentUserId = userRepository.getCurrentUserId() ?: ""
        val username = currentUser.kullaniciAdi ?: ""
        val tempYanitId = "temp_${System.currentTimeMillis()}"

        viewModelScope.launch {
            val yeniYanit = Yanit_Model().apply {
                this.yanitId = tempYanitId
                this.yorumId = commentId
                this.yaniticerik = content
                this.adi = username
                this.yanitiYukleyen = currentUserId
                this.tarih = java.util.Date()
                this.begeniSayisiYanit = 0
                this.isBegenildiMi = false
                this.isSending = true
                this.isLocalOnly = true
            }

            val mevcutYorum = _comments.value.find { it.yorumID == commentId }
            val zatenGorunuyorMu = mevcutYorum?.isYanitlarGorunuyor == true

            val eskiListe = yanitCacheMap[commentId] ?: mutableListOf()
            val gösterilecekListe = eskiListe.toMutableList().apply { add(0, yeniYanit) }

            yanitCacheMap[commentId] = gösterilecekListe
            val dahaFazla = yanitDahaFazlaMap[commentId] ?: false

            updateCommentYanitState(commentId, gösterilecekListe, gorunur = zatenGorunuyorMu, dahaFazla = dahaFazla)

            val replyId = yanitRepository.addReply(
                catId = catId,
                commentId = commentId,
                content = content,
                username = username,
                userId = currentUserId
            )

            if (replyId != null) {
                _actionSuccess.emit(true)

                val guncelYanitListesi = (yanitCacheMap[commentId] ?: mutableListOf()).toMutableList()
                val tempIndex = guncelYanitListesi.indexOfFirst { it.yanitId == tempYanitId }

                if (tempIndex != -1) {
                    val guncellenenGercekYanit = guncelYanitListesi[tempIndex].copy().apply {
                        this.yanitId = replyId
                        this.isSending = false
                        this.isLocalOnly = true // YENİ: Firebase başarılı döndü, artık sunucuda var
                    }
                    guncelYanitListesi[tempIndex] = guncellenenGercekYanit
                    yanitCacheMap[commentId] = guncelYanitListesi
                }

                updateCommentYanitState(commentId, guncelYanitListesi, gorunur = zatenGorunuyorMu, dahaFazla = dahaFazla)
                onComplete(replyId)
            } else {
                val hataListesi = (yanitCacheMap[commentId] ?: mutableListOf()).toMutableList()
                hataListesi.removeAll { it.yanitId == tempYanitId }
                yanitCacheMap[commentId] = hataListesi

                updateCommentYanitState(commentId, hataListesi, gorunur = zatenGorunuyorMu, dahaFazla = dahaFazla)

                _hataMesaji.emit("Yanıt gönderilemedi.")
                onComplete(null)
            }
        }
    }
    fun toggleBegeni(catId: String?, yorum: Yorum_Model?, kullaniciId: String?) {
        if (catId == null || yorum == null || kullaniciId == null) return
        val yorumId = yorum.yorumID ?: return

        val mevcutBegeniler = _begenilenYorumIDs.value.toMutableSet()
        val durumBegenilmis = mevcutBegeniler.contains(yorumId)

        if (durumBegenilmis) {
            mevcutBegeniler.remove(yorumId)
        } else {
            mevcutBegeniler.add(yorumId)
        }

        _begenilenYorumIDs.value = mevcutBegeniler
        CacheHelperYorum.saveBegenilenSet(getApplication(), mevcutBegeniler)

        _comments.update { current ->
            current.map { item ->
                if (item.yorumID == yorumId) {
                    item.copy().apply {
                        val yeniBegeniSayisi = if (durumBegenilmis) (item.begeniSayisi - 1).coerceAtLeast(0) else item.begeniSayisi + 1
                        this.begeniSayisi = yeniBegeniSayisi
                        setBegenildiMi(!durumBegenilmis)
                    }
                } else item
            }
        }

        if (durumBegenilmis) {
            repository.yorumBegeniKaldir(catId, yorumId, kullaniciId).addOnFailureListener {
                rollbackBegeniState(yorumId, true)
            }
        } else {
            repository.yorumBegen(catId, yorumId, kullaniciId).addOnFailureListener {
                rollbackBegeniState(yorumId, false)
            }
        }
    }

    fun toggleYanitBegeni(catId: String, yorumId: String, yanit: Yanit_Model?, kullaniciId: String?) {
        if (catId.isEmpty() || yorumId.isEmpty() || yanit == null || kullaniciId == null) return
        val yanitId = yanit.yanitId ?: return

        val mevcutBegeniler = _begenilenYanitIDs.value.toMutableSet()
        val durumBegenilmis = mevcutBegeniler.contains(yanitId)

        if (durumBegenilmis) {
            mevcutBegeniler.remove(yanitId)
        } else {
            mevcutBegeniler.add(yanitId)
        }

        _begenilenYanitIDs.value = mevcutBegeniler
        CacheHelperYorum.saveBegenilenYanitSet(getApplication(), mevcutBegeniler)

        val yanitListesi = yanitCacheMap[yorumId] ?: mutableListOf()
        val index = yanitListesi.indexOfFirst { it.yanitId == yanitId }

        if (index != -1) {
            val hedefYanit = yanitListesi[index]
            val yeniSayi = if (durumBegenilmis) (hedefYanit.begeniSayisiYanit - 1).coerceAtLeast(0) else hedefYanit.begeniSayisiYanit + 1

            val guncellenenYanit = hedefYanit.copy().apply {
                begeniSayisiYanit = yeniSayi
                setBegenildiMi(!durumBegenilmis)
            }

            val guncelYanitlar = yanitListesi.toMutableList()
            guncelYanitlar[index] = guncellenenYanit

            yanitCacheMap[yorumId] = guncelYanitlar
            val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false

            updateCommentYanitState(yorumId, guncelYanitlar, null, dahaFazla)

            viewModelScope.launch {
                if (durumBegenilmis) {
                    yanitRepository.yanitBegeniKaldir(catId, yorumId, yanitId, kullaniciId).addOnFailureListener {
                        rollbackYanitBegeniState(yorumId, yanitId, true)
                    }
                } else {
                    yanitRepository.yanitBegen(catId, yorumId, yanitId, kullaniciId).addOnFailureListener {
                        rollbackYanitBegeniState(yorumId, yanitId, false)
                    }
                }
            }
        }
    }

    private fun rollbackYanitBegeniState(yorumId: String, yanitId: String, eskiDurumBegenilmisMi: Boolean) {
        viewModelScope.launch { _hataMesaji.emit("Yanıt beğenilemedi.") }
        val mevcutBegeniler = _begenilenYanitIDs.value.toMutableSet()
        if (eskiDurumBegenilmisMi) {
            mevcutBegeniler.add(yanitId)
        } else {
            mevcutBegeniler.remove(yanitId)
        }
        _begenilenYanitIDs.value = mevcutBegeniler
        CacheHelperYorum.saveBegenilenYanitSet(getApplication(), mevcutBegeniler)

        val yanitListesi = yanitCacheMap[yorumId] ?: mutableListOf()
        val index = yanitListesi.indexOfFirst { it.yanitId == yanitId }

        if (index != -1) {
            val hedefYanit = yanitListesi[index]
            val geriAlinanSayi = if (eskiDurumBegenilmisMi) hedefYanit.begeniSayisiYanit + 1 else (hedefYanit.begeniSayisiYanit - 1).coerceAtLeast(0)

            val guncellenenYanit = hedefYanit.copy().apply {
                begeniSayisiYanit = geriAlinanSayi
                setBegenildiMi(eskiDurumBegenilmisMi)
            }
            val guncelYanitlar = yanitListesi.toMutableList()
            guncelYanitlar[index] = guncellenenYanit
            yanitCacheMap[yorumId] = guncelYanitlar
            val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false

            updateCommentYanitState(yorumId, guncelYanitlar, null, dahaFazla)
        }
    }

    private fun rollbackBegeniState(yorumId: String, eskiDurumBegenilmisMi: Boolean) {
        viewModelScope.launch { _hataMesaji.emit("İşlem başarısız oldu.") }

        val mevcutBegeniler = _begenilenYorumIDs.value.toMutableSet()
        if (eskiDurumBegenilmisMi) {
            mevcutBegeniler.add(yorumId)
        } else {
            mevcutBegeniler.remove(yorumId)
        }
        _begenilenYorumIDs.value = mevcutBegeniler
        CacheHelperYorum.saveBegenilenSet(getApplication(), mevcutBegeniler)

        _comments.update { current ->
            current.map { item ->
                if (item.yorumID == yorumId) {
                    item.copy().apply {
                        val geriAlinanSayisi = if (eskiDurumBegenilmisMi) item.begeniSayisi + 1 else (item.begeniSayisi - 1).coerceAtLeast(0)
                        this.begeniSayisi = geriAlinanSayisi
                        setBegenildiMi(eskiDurumBegenilmisMi)
                    }
                } else item
            }
        }
    }

    fun deleteComment(yorumId: String) {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val success = repository.deleteComment(catId, yorumId)
            if (success) {
                _comments.update { current ->
                    current.toMutableList().apply { removeAll { it.yorumID == yorumId } }
                }
                _isEmpty.value = _comments.value.isEmpty()
                _commentCount.value = (_commentCount.value - 1).coerceAtLeast(0)
            } else {
                _hataMesaji.emit("Yorum silinemedi.")
            }
        }
    }

    fun updateComment(yorumId: String, yeniIcerik: String) {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val success = repository.updateCommentContent(catId, yorumId, yeniIcerik)
            if (success) {
                _comments.update { current ->
                    current.map { yorum ->
                        if (yorum.yorumID == yorumId) {
                            yorum.copy().apply { setYorumicerik(yeniIcerik) }
                        } else yorum
                    }
                }
            } else {
                _hataMesaji.emit("Yorum güncellenemedi.")
            }
        }
    }

    fun fetchCommentCount() {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val count = repository.getCommentCount(catId)
            _commentCount.value = count
        }
    }
}