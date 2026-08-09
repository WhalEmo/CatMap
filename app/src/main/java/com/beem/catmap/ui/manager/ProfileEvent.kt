package com.beem.catmap.ui.manager

import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.models.Gonderi

sealed class ProfileEvent {
    data class PostAdded(val post: Gonderi) : ProfileEvent()
    data class PostDeleted(
        val postId: String? = null,
        val catId: String? = null
    ) : ProfileEvent()

    data class ProfileUpdated(
        val updatedUser: Kullanici? = null
    ) : ProfileEvent()

    data class FollowUser(
        val userId: String,
        val kullaniciAdi: String,
        val fotoUrl: String,
        val operatorUserId: String
    ) : ProfileEvent()

    data class UnFollowUser(
        val userId: String,
        val operatorUserId: String
    ) : ProfileEvent()

    data class UnFollowerUser(
        val userId: String,
        val operatorUserId: String
    ): ProfileEvent()

}