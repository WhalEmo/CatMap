package com.beem.catmap.gonderi

import com.beem.catmap.models.Gonderi
import com.google.firebase.firestore.DocumentSnapshot

data class ProfilePostUiState(
    val posts: List<Gonderi> = emptyList(),
    val isLoading: Boolean = false,
    val isMoreLoading: Boolean = false,
    val isEmpty: Boolean = false,
    val isAccessDenied: Boolean = false,
    val isLastPage: Boolean = false,
    val lastDocument: DocumentSnapshot? = null,

)