package com.beem.catmap.ui.camera

data class GalleryPage(
    val images: List<String>,
    val lastDateAdded: Long?,
    val lastImageId: Long?,
    val hasMore: Boolean
)