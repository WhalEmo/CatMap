package com.beem.catmap.ui.profile.post

import com.beem.catmap.data.model.Post
import com.google.firebase.firestore.DocumentSnapshot

data class PostUiState(
    val posts: List<Post> = emptyList(),
    val isLoading: Boolean = false,
    val isMoreLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val isAccessDenied: Boolean = false,
    val isLastPage: Boolean = false,
    val lastDocument: DocumentSnapshot? = null,
)