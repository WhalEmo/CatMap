package com.beem.catmap.data.model

sealed class ProfileUpdateResult {
    object Idle : ProfileUpdateResult()
    data class Success(
        val newPhotoUrl: String?,
        val newUsername: String,
        val newName: String,
        val newSurname: String,
        val newBio: String
    ) : ProfileUpdateResult()
    object UsernameAlreadyTaken : ProfileUpdateResult()
    data class Error(val message: String) : ProfileUpdateResult()
    object Loading : ProfileUpdateResult()
}
data class UserStats(
    val followerCount: Long = 0L,
    val followingCount: Long = 0L,
)
data class ProfileState(
    val name: String? = null,
    val surname: String? = null,
    val username: String? = null,
    val followersCount: Long = 0L,
    val followingCount: Long = 0L,
    val postCount: Long = 0L,
    val bio: String? = null,
    val photoUrl: String? = null
)