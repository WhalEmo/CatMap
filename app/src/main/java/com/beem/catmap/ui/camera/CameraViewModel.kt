package com.beem.catmap.ui.camera

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.beem.catmap.ui.manager.image.ImageUploadManager
import com.beem.catmap.ui.manager.image.UploadSession
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class CameraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<CameraUiEvent>()
    val uiEvent: SharedFlow<CameraUiEvent> = _uiEvent.asSharedFlow()

    private var activeSession = UploadSession.GENERAL

    private var sessionObserveJob: kotlinx.coroutines.Job? = null

    fun initializeSession(session: UploadSession) {
        this.activeSession = session

        // 🚀 KRİTİK FİX: Eğer önceden başlatılmış bir dinleyici varsa onu iptal et.
        // Bu sayede Fragment'a kaç kere gir-çık yapılırsa yapılsın ASLA çoklu dinleyici (spam) oluşmaz!
        sessionObserveJob?.cancel()

        sessionObserveJob = viewModelScope.launch {
            val session = activeSession.name
            Log.d("CameraVM_Log", "🟢 [OTURUM BAŞLADI] Dinlenen Session: ${activeSession.name}")

            ImageUploadManager.observeSession(activeSession).collect { uris ->
                Log.d("CameraVM_Log", "🔥 [FLOW TETİKLENDİ] [$session] Manager'dan gelen güncel liste boyutu: ${uris.size}")
                uris.forEachIndexed { index, uri ->
                    Log.d("CameraVM_Log", "   -> Uri[$index]: $uri")
                }

                val images = uris.map { uri ->
                    val source = if (uri.toString().contains("CatMap_Temp")) {
                        ImageSource.TEMP_CACHE
                    } else {
                        ImageSource.GALERI
                    }
                    CapturedImage(uri = uri, source = source).also {
                        Log.d("CameraVM_Log", "   -> Dönüştürüldü: Source=${it.source}, ID=${it.id}")
                    }
                }

                _uiState.update { state ->
                    val currentPreview = state.previewedImage
                    val isPreviewStillExists = currentPreview != null && uris.contains(currentPreview.uri)

                    Log.d("CameraVM_Log", "🧐 [ÖNİZLEME KONTROLÜ] [$session]")
                    Log.d("CameraVM_Log", "   -> Şu anki Preview: ${currentPreview?.uri}")
                    Log.d("CameraVM_Log", "   -> Yeni listede var mı?: $isPreviewStillExists")

                    val newState = state.copy(
                        capturedImages = images,
                        previewedImage = if (isPreviewStillExists) currentPreview else null
                    )

                    // Logcat'te state'in son halini açıkça görelim
                    Log.d("CameraVM_Log", "✅ [STATE GÜNCELLENDİ] [$session]")
                    Log.d("CameraVM_Log", "   -> isCapturing: ${newState.isCapturing}")
                    Log.d("CameraVM_Log", "   -> isProcessing: ${newState.isProcessing}")
                    Log.d("CameraVM_Log", "   -> currentMode: ${newState.currentMode}")
                    Log.d("CameraVM_Log", "   -> Listedeki Resim Sayısı: ${newState.capturedImages.size}")
                    Log.d("CameraVM_Log", "---------------------------------------------------")

                    newState // update bloğu yeni state'i döndürmelidir
                }
            }
        }
    }

    fun setCapturing(isCapturing: Boolean) {
        _uiState.update { it.copy(isCapturing = isCapturing) }
    }

    fun selectImageForPreview(image: CapturedImage) {
        _uiState.update { it.copy(previewedImage = image) }
    }

    fun exitPreviewMode() {
        _uiState.update { it.copy(previewedImage = null) }
    }

    fun removeImageFromStrip(image: CapturedImage) {
        ImageUploadManager.removeImage(activeSession, image.uri)
    }



    fun deleteImage(contentResolver: ContentResolver, image: CapturedImage) {
        _uiState.update { it.copy(isProcessing = true) } // Loader'ı aç

        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (image.source == ImageSource.TEMP_CACHE) {
                    File(image.uri.path ?: "").delete()
                } else {
                    contentResolver.delete(image.uri, null, null)
                }

                withContext(Dispatchers.Main) {
                    ImageUploadManager.removeImage(activeSession, image.uri)
                    _uiState.update { it.copy(isProcessing = false, previewedImage = null) }
                    _uiEvent.emit(CameraUiEvent.ShowToast("Fotoğraf tamamen silindi.", true))
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isProcessing = false) }
                    _uiEvent.emit(CameraUiEvent.ShowToast("Silme işlemi başarısız!", false))
                }
            }
        }
    }

    fun saveTempImageToGallery(context: Context, image: CapturedImage, shouldKeepInStrip: Boolean) {
        _uiState.update { it.copy(isProcessing = true) } // Loader'ı aç

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val timeStamp = System.currentTimeMillis()
                val filename = "CatMap_$timeStamp.jpg"

                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CatMap")
                }

                val galleryUri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                    ?: throw Exception("MediaStore kaydı başlatılamadı.")

                contentResolver.openInputStream(image.uri)?.use { inputStream ->
                    contentResolver.openOutputStream(galleryUri)?.use { outputStream ->
                        inputStream.copyTo(outputStream)
                    }
                }

                withContext(Dispatchers.Main) {
                    // Eski Temp Cache verisini komple çöpe at
                    ImageUploadManager.removeImage(activeSession, image.uri)
                    File(image.uri.path ?: "").delete()

                    if (shouldKeepInStrip) {
                        ImageUploadManager.addImage(activeSession, galleryUri)
                        _uiEvent.emit(CameraUiEvent.ShowToast("Fotoğraf şeride ve galeriye eklendi!", true))
                    } else {
                        _uiEvent.emit(CameraUiEvent.ShowToast("Fotoğraf cihaz galerisine kaydedildi.", true))
                    }

                    _uiState.update { it.copy(isProcessing = false, previewedImage = null) }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(isProcessing = false) }
                    _uiEvent.emit(CameraUiEvent.ShowToast("Galeriye kaydetme başarısız oldu!", false))
                }
            }
        }
    }

    fun onPhotoCaptured(uri: Uri) {
        ImageUploadManager.addImage(activeSession, uri)

        _uiState.update {
            it.copy(
                previewedImage = CapturedImage(uri = uri, source = ImageSource.TEMP_CACHE),
                isCapturing = false // Kilidi açıyoruz
            )
        }
    }
}