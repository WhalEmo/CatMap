package com.beem.catmap.data.model

import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.models.Gonderi
import com.google.firebase.firestore.DocumentSnapshot

data class FullProfileData(
    val profile: Kullanici,
    val posts: List<Gonderi>,
    val lastDocument: DocumentSnapshot? = null,
    val isLastPage: Boolean = true,
    val followerCount: Long = 0,
    val followingCount: Long = 0,
    val postCount: Long = 0,
    val isSelfProfile: Boolean = false,
    val isFollowing: Boolean,
    val isFollowed: Boolean,
    val isAccessDenied: Boolean = false
)