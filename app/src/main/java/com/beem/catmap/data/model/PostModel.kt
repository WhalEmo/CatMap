package com.beem.catmap.data.model

import com.google.firebase.Timestamp

data class Post(
    var photoUrlList: List<String> = emptyList(),
    var bio: String? = null,
    var catName: String? = null,
    var date: Timestamp? = null,
    var likeCount: Long? = 0L,
    var catId: String? = null
)
data class SendCatItem(
    val catId: String = "",
    val date: Timestamp? = null
)
