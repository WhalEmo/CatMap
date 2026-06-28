package com.beem.catmap.models

import java.util.Date

data class CommentModel(
    val id: String = "",
    val kullanici_adi: String = "",
    val icerik: String = "",
    val Yukleyen_ID: String = "",
    val zaman: Date? = null
)