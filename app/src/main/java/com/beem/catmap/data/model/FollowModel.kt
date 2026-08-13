package com.beem.catmap.data.model

import com.google.firebase.firestore.DocumentSnapshot

data class PaginatedResult<T>(
    val items: List<T>,
    val lastDocument: DocumentSnapshot?,
    val isLastPage: Boolean = false
)

data class FollowResult(
    val currentFollowingCount: Long,
    val targetFollowerCount: Long
)

data class RemoveFollowerResult(
    val currentFollowerCount: Long,
    val followerFollowingCount: Long
)