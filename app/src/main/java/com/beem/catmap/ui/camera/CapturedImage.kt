package com.beem.catmap.ui.camera

import android.net.Uri
import java.util.UUID

data class CapturedImage(
    val id: String = UUID.randomUUID().toString(),
    val uri: Uri,
    val source: ImageSource
)