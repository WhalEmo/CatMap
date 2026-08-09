package com.beem.catmap.models

import com.google.firebase.Timestamp

data class Gonderi(
    var fotoUrlListesi: List<String> = emptyList(),
    var aciklama: String? = null,
    var kediAdi: String? = null,
    var tarih: Timestamp? = null,
    var begeniSayisi: Long? = 0L,
    var kediID: String? = null
)
data class GonderilenKediItem(
    val kediID: String = "",
    val tarih: Timestamp? = null
)
