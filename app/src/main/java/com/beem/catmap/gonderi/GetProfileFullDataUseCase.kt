package com.beem.catmap.domain.usecase

import android.util.Log
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.FullProfileData
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.gonderi.ProfileRepository
import com.beem.catmap.gonderi.UiState
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.session.CurrentUserManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class GetProfileFullDataUseCase(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
    private val userManager: CurrentUserManager
) {
    suspend operator fun invoke(
        targetUserId: String,
        forceRefresh: Boolean = false
    ): Result<FullProfileData> = withContext(Dispatchers.IO) {
        if (targetUserId.isBlank()) {
            return@withContext Result.failure(Exception("Geçersiz Kullanıcı ID"))
        }

        try {
            val isSelf = (targetUserId == UserSession.userId)

            coroutineScope {
                val isFollowingDeferred = if (!isSelf) {
                    async { followRepository.isFollowing(targetUserId, forceRefresh).getOrDefault(false) }
                } else null

                val isFollowedDeferred = if (!isSelf) {
                    async { followRepository.isFollowedBy(targetUserId, forceRefresh).getOrDefault(false) }
                } else null

                val profileDeferred = async {
                    if (isSelf && !forceRefresh) {
                        val cachedUser = userManager.getCurrentUser()
                        if (!cachedUser.kullaniciAdi.isNullOrBlank()) {
                            Log.d("CACHE","CACHEDENNN GELDIII")
                            UiState.Success(cachedUser)
                        } else {
                            profileRepository.getUserProfile(targetUserId, forceRefresh = forceRefresh)
                        }
                    } else {
                        profileRepository.getUserProfile(targetUserId, forceRefresh = forceRefresh)
                    }
                }

                val isFollowing = isFollowingDeferred?.await() ?: false
                val isFollowed = isFollowedDeferred?.await() ?: false
                val accessDenied = !isSelf && !isFollowing

                val postsDeferred = if (!accessDenied) {
                    async {
                        postRepository.getKullaniciGonderileri(
                            userId = targetUserId,
                            lastDocument = null,
                            forceRefresh = forceRefresh
                        )
                    }
                } else null

                val profileState = profileDeferred.await()
                val postsResult = postsDeferred?.await()

                val profileData = (profileState as? UiState.Success)?.data
                    ?: return@coroutineScope Result.failure(
                        Exception((profileState as? UiState.Error)?.message ?: "Profil yüklenemedi.")
                    )

                val postPageResult = postsResult?.getOrNull()

                Result.success(
                    FullProfileData(
                        profile = profileData,
                        posts = postPageResult?.posts ?: emptyList(),
                        lastDocument = postPageResult?.lastDocument,
                        isLastPage = postPageResult?.isLastPage ?: true,
                        followerCount = profileData.takipciSayisi ?: 0L,
                        followingCount = profileData.takipEdilenSayisi ?: 0L,
                        postCount = profileData.gonderiSayisi ?: 0L,
                        isSelfProfile = isSelf,
                        isFollowing = isFollowing,
                        isFollowed = isFollowed,
                        isAccessDenied = accessDenied
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}