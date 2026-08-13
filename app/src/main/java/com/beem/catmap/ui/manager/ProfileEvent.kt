package com.beem.catmap.ui.manager

import com.beem.catmap.data.model.UserModel
import com.beem.catmap.data.model.Post

sealed class ProfileEvent {
    data class PostAdded(val post: Post) : ProfileEvent()
    data class PostDeleted(
        val postId: String? = null,
        val catId: String? = null
    ) : ProfileEvent()

    data class ProfileUpdated(
        val updatedUserModel: UserModel? = null
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

    data class BlockedUser(
        val userId: String,
        val kullaniciAdi: String,
        val fotoUrl: String,
        val operatorUserId: String
    ): ProfileEvent()

    data class UnblockedUser(
        val userId: String,
        val operatorUserId: String
    ): ProfileEvent()

}