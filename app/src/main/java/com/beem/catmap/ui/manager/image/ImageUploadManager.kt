package com.beem.catmap.ui.manager.image

import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

object ImageUploadManager {
    private val sessionFlows = mutableMapOf<UploadSession, MutableStateFlow<List<Uri>>>()

    fun observeSession(session: UploadSession): StateFlow<List<Uri>> {
        return sessionFlows.getOrPut(session) { MutableStateFlow(emptyList()) }.asStateFlow()
    }

    fun addImage(session: UploadSession, uri: Uri) {
        val flow = sessionFlows.getOrPut(session) { MutableStateFlow(emptyList()) }
        if (flow.value.size < session.maxImageCount) {
            flow.update { it + uri }
        }
    }

    fun addImages(session: UploadSession, newUris: List<Uri>) {
        val flow = sessionFlows.getOrPut(session) { MutableStateFlow(emptyList()) }
        val remainingSpace = session.maxImageCount - flow.value.size
        if (remainingSpace > 0) {
            val toAdd = newUris.take(remainingSpace)
            flow.update { it + toAdd }
        }
    }

    fun removeImage(session: UploadSession, uri: Uri) {
        val flow = sessionFlows[session] ?: return
        if (flow.value.contains(uri)) {
            flow.update { it - uri }
        }
    }

    fun clearSession(session: UploadSession) {
        sessionFlows[session]?.value = emptyList()
    }

    fun getImages(session: UploadSession): List<Uri> {
        return sessionFlows[session]?.value ?: emptyList()
    }


}