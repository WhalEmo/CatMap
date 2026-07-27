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
        val likedYanitSet = _begenilenYanitIDs.value

        return comments.map { yorum ->
            val isLiked = yorum.yorumID != null && likedSet.contains(yorum.yorumID)

            // Yanıtların da beğeni durumunu set ediyoruz
            yorum.yanitlar?.let { yanitListesi ->
                for (yanit in yanitListesi) {
                    val yanitLiked = yanit.yanitId != null && likedYanitSet.contains(yanit.yanitId)
                    yanit.setBegenildiMi(yanitLiked) // Yanıt Modelinde bu metodun olduğuna emin olun
                }
            }

            yorum.copy().apply {
                setBegenildiMi(isLiked)
            }
        }
    }

    fun fetchInitialComments() {
        if (catId.isEmpty()) return

        viewModelScope.launch {
            _isLoading.value = true
            isLastPage = false
            lastVisibleDoc = null

            val (initialList, lastDoc) = repository.getInitialComments(catId, pageSize)

            // Beğeni durumunu lokal set ile eşleştiriyoruz
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
        if (_isLoading.value || isLastPage || catId.isEmpty()) return
        val currentLastDoc = lastVisibleDoc ?: return

        viewModelScope.launch {
            _isLoading.value = true
            val (newComments, newLastDoc) = repository.loadMoreComments(catId, currentLastDoc, pageSize)

            if (newComments.isNotEmpty()) {
                val processedNewComments = applyLikedState(newComments)
                val updatedList = _comments.value.toMutableList().apply { addAll(processedNewComments) }
                _comments.value = updatedList
                lastVisibleDoc = newLastDoc
                if (newComments.size < pageSize) isLastPage = true
            } else {
                isLastPage = true
            }
            _isLoading.value = false
        }
    }

    fun toggleYanitlarGorunurluk(yorumId: String) {
        val currentList = _comments.value.map { yorum ->
            if (yorum.yorumID == yorumId) {
                yorum.copy().apply {
                    val yeniDurum = !isYanitlarGorunuyor
                    setYanitlarGorunuyor(yeniDurum)
                    if (yeniDurum && (yanitlar == null || yanitlar.isEmpty())) {
                        yanitlariYukle(yorumId, 10, true)
                    }
                }
            } else {
                yorum
            }
        }
        _comments.value = currentList
    }

    fun yanitlariYukle(yorumId: String, limit: Int, clearList: Boolean) {
        if (catId.isEmpty() || yorumId.isEmpty()) return

        val lastDoc = if (clearList) null else yanitLastDocMap[yorumId]

        viewModelScope.launch {
            val result = yanitRepository.yanitlariCek(catId, yorumId, limit, lastDoc)

            result.onSuccess { (yeniYanitlar, yeniLastDoc) ->
                yanitLastDocMap[yorumId] = yeniLastDoc

                val liste = yanitCacheMap.getOrPut(yorumId) { mutableListOf() }
                if (clearList) {
                    liste.clear()
                }
                liste.addAll(yeniYanitlar)

                val dahaFazlaVar = yeniYanitlar.size >= limit
                yanitDahaFazlaMap[yorumId] = dahaFazlaVar

                updateCommentYanitState(yorumId, liste, true, dahaFazlaVar)
            }.onFailure {
                _hataMesaji.emit("Yanıtlar yüklenirken hata oluştu.")
            }
        }
    }

    private fun updateCommentYanitState(yorumId: String, yeniYanitlar: List<Yanit_Model>, gorunur: Boolean, dahaFazla: Boolean) {
        val currentList = _comments.value.map { yorum ->
            if (yorum.yorumID == yorumId) {
                yorum.copy().apply {
                    setYanitlar(ArrayList(yeniYanitlar))
                    setYanitlarGorunuyor(gorunur)
                    setDahafazlaGozukuyorMu(dahaFazla)
                }
            } else {
                yorum
            }
        }
        _comments.value = currentList
    }


    fun updateYanit(yorumId: String, yanitId: String, yeniIcerik: String) {
        if (catId.isEmpty() || yorumId.isEmpty() || yanitId.isEmpty() || yeniIcerik.trim().isEmpty()) return

        viewModelScope.launch {
            val success = yanitRepository.yanitGuncelle(catId, yorumId, yanitId, yeniIcerik)
            if (success) {
                val eskiListe = yanitCacheMap[yorumId] ?: mutableListOf()
                val guncelListe = eskiListe.map { yanit ->
                    if (yanit.yanitId == yanitId) {
                        Yanit_Model().apply {
                            this.yanitId = yanit.yanitId
                            this.yorumId = yanit.yorumId
                            this.adi = yanit.adi
                            this.yaniticerik = yeniIcerik
                            this.tarih = yanit.tarih
                            this.yanitiYukleyen = yanit.yanitiYukleyen
                            this.begeniSayisiYanit = yanit.begeniSayisiYanit
                        }
                    } else {
                        yanit
                    }
                }.toMutableList()

                yanitCacheMap[yorumId] = guncelListe
                val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false
                updateCommentYanitState(yorumId, guncelListe, true, dahaFazla)
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
                updateCommentYanitState(yorumId, guncelYanitlar, true, dahaFazla)
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

            val mevcutListe = _comments.value.toMutableList()
            mevcutListe.add(0, yeniYorum)
            _comments.value = mevcutListe
            _isEmpty.value = false

            val realCommentId = repository.addComment(catId, content, username, currentUserId)

            if (realCommentId != null) {
                _actionSuccess.emit(true)
                _commentCount.value = _commentCount.value + 1

                val guncelListe = _comments.value.map { yorum ->
                    if (yorum.yorumID == tempId) {
                        yorum.copy().apply {
                            this.yorumID = realCommentId
                            this.sending = false
                        }
                    } else {
                        yorum
                    }
                }
                _comments.value = guncelListe
            } else {
                val hataListesi = _comments.value.toMutableList()
                hataListesi.removeAll { it.yorumID == tempId }
                _comments.value = hataListesi
                _isEmpty.value = hataListesi.isEmpty()

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
            }
            val eskiListe = yanitCacheMap[commentId] ?: mutableListOf()
            val yeniListe = ArrayList(eskiListe).apply {
                add(0, yeniYanit)
            }
            yanitCacheMap[commentId] = yeniListe
            val dahaFazla = yanitDahaFazlaMap[commentId] ?: false
            updateCommentYanitState(commentId, yeniListe, true, dahaFazla)
            val replyId = yanitRepository.addReply(
                catId = catId,
                commentId = commentId,
                content = content,
                username = username,
                userId = currentUserId
            )
            if (replyId != null) {
                _actionSuccess.emit(true)
                val guncelYanitListesi = (yanitCacheMap[commentId] ?: mutableListOf()).map { yanit ->
                    if (yanit.yanitId == tempYanitId) {
                        yanit.copy().apply {
                            this.yanitId = replyId
                            this.isSending = false
                        }
                    } else {
                        yanit
                    }
                }.toMutableList()
                yanitCacheMap[commentId] = guncelYanitListesi
                updateCommentYanitState(commentId, guncelYanitListesi, true, dahaFazla)
                onComplete(replyId)
            } else {
                val hataListesi = (yanitCacheMap[commentId] ?: mutableListOf()).toMutableList()
                hataListesi.removeAll { it.yanitId == tempYanitId }
                yanitCacheMap[commentId] = hataListesi
                updateCommentYanitState(commentId, hataListesi, hataListesi.isNotEmpty(), dahaFazla)

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

        val currentList = _comments.value.map { item ->
            if (item.yorumID == yorumId) {
                item.copy().apply {
                    val yeniBegeniSayisi = if (durumBegenilmis) (item.begeniSayisi - 1).coerceAtLeast(0) else item.begeniSayisi + 1
                    this.begeniSayisi = yeniBegeniSayisi
                    setBegenildiMi(!durumBegenilmis)
                }
            } else item
        }
        _comments.value = currentList

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

    fun toggleYanitBegeni(catId: String,yorumId: String, yanit: Yanit_Model?, kullaniciId: String?) {
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

        var guncellenenYanit: Yanit_Model? = null

        val yanitListesi = yanitCacheMap[yorumId] ?: mutableListOf()
        val guncelYanitlar = yanitListesi.map { y ->
            if (y.yanitId == yanitId) {
                val yeniYanit = y.copy().apply {
                    val yeniSayi = if (durumBegenilmis) (begeniSayisiYanit - 1).coerceAtLeast(0) else begeniSayisiYanit + 1
                    begeniSayisiYanit = yeniSayi
                    setBegenildiMi(!durumBegenilmis)
                }
                guncellenenYanit = yeniYanit
                yeniYanit
            } else {
                y
            }
        }.toMutableList()

        yanitCacheMap[yorumId] = guncelYanitlar
        val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false

        updateCommentSingleYanitState(yorumId, guncellenenYanit, guncelYanitlar, true, dahaFazla)

        viewModelScope.launch {
            val success = if (durumBegenilmis) {
                yanitRepository.yanitBegeniKaldir(catId, yorumId, yanitId, kullaniciId)
            } else {
                yanitRepository.yanitBegen(catId, yorumId, yanitId, kullaniciId)
            }

            if (!success) {
                rollbackYanitBegeniState(yorumId, yanitId, durumBegenilmis)
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

        var guncellenenYanit: Yanit_Model? = null
        val yanitListesi = yanitCacheMap[yorumId] ?: mutableListOf()
        val guncelYanitlar = yanitListesi.map { y ->
            if (y.yanitId == yanitId) {
                val yeniYanit = y.copy().apply {
                    val geriAlinanSayi = if (eskiDurumBegenilmisMi) begeniSayisiYanit + 1 else (begeniSayisiYanit - 1).coerceAtLeast(0)
                    begeniSayisiYanit = geriAlinanSayi
                    setBegenildiMi(eskiDurumBegenilmisMi)
                }
                guncellenenYanit = yeniYanit
                yeniYanit
            } else {
                y
            }
        }.toMutableList()

        yanitCacheMap[yorumId] = guncelYanitlar
        val dahaFazla = yanitDahaFazlaMap[yorumId] ?: false
        updateCommentSingleYanitState(yorumId, guncellenenYanit, guncelYanitlar, true, dahaFazla)
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

        val currentList = _comments.value.map { item ->
            if (item.yorumID == yorumId) {
                item.copy().apply {
                    val geriAlinanSayisi = if (eskiDurumBegenilmisMi) item.begeniSayisi + 1 else (item.begeniSayisi - 1).coerceAtLeast(0)
                    this.begeniSayisi = geriAlinanSayisi
                    setBegenildiMi(eskiDurumBegenilmisMi)
                }
            } else item
        }
        _comments.value = currentList
    }

    fun deleteComment(yorumId: String) {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val success = repository.deleteComment(catId, yorumId)
            if (success) {
                val currentList = _comments.value.toMutableList()
                currentList.removeAll { it.yorumID == yorumId }
                _comments.value = currentList
                _isEmpty.value = currentList.isEmpty()
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
                val currentList = _comments.value.map { yorum ->
                    if (yorum.yorumID == yorumId) {
                        yorum.copy().apply { setYorumicerik(yeniIcerik) }
                    } else yorum
                }
                _comments.value = currentList
            } else {
                _hataMesaji.emit("Yorum güncellenemedi.")
            }
        }
    }
    // Sadece TEK BİR yanıt değiştiğinde çalışan yüksek performanslı güncelleyici
    private fun updateCommentSingleYanitState(
        yorumId: String,
        guncellenenYanit: Yanit_Model?,
        yeniYanitlar: List<Yanit_Model>,
        gorunur: Boolean,
        dahaFazla: Boolean
    ) {
        val currentList = _comments.value.map { yorum ->
            if (yorum.yorumID == yorumId) {
                val yeniYorum = if (guncellenenYanit != null) {
                    yorum.copyWithUpdatedYanit(guncellenenYanit)
                } else {
                    yorum.copy().apply { setYanitlar(ArrayList(yeniYanitlar)) }
                }

                yeniYorum.apply {
                    setYanitlarGorunuyor(gorunur)
                    setDahafazlaGozukuyorMu(dahaFazla)
                }
            } else {
                yorum
            }
        }
        _comments.value = currentList
    }

    fun fetchCommentCount() {
        if (catId.isEmpty()) return
        viewModelScope.launch {
            val count = repository.getCommentCount(catId)
            _commentCount.value = count
        }
    }
}
