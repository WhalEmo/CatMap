package com.beem.catmap.domain.usecase

import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.FullProfileData
import com.beem.catmap.data.repository.FollowCounts
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.gonderi.ProfilePostCacheData
import com.beem.catmap.gonderi.ProfileRepository
import com.beem.catmap.gonderi.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
class GetProfileFullDataUseCase(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository
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
                val profileDeferred = async {
                    profileRepository.getUserProfile(targetUserId, forceRefresh)
                }

                val postsDeferred = async {
                    postRepository.getUserPosts(targetUserId, forceRefresh)
                }

                val profileState = profileDeferred.await()
                val postsResult = postsDeferred.await()

                val profileData = (profileState as? UiState.Success)?.data
                    ?: return@coroutineScope Result.failure(
                        Exception((profileState as? UiState.Error)?.message ?: "Profil yüklenemedi.")
                    )

                val postsCache = postsResult.getOrElse {
                    ProfilePostCacheData(emptyList(), emptyList(), 0, true)
                }

                Result.success(
                    FullProfileData(
                        profile = profileData,
                        postsCache = postsCache,
                        followerCount = profileData.takipciSayisi,
                        followingCount = profileData.takipEdilenSayisi,
                        postCount = profileData.gonderiSayisi,
                        isSelfProfile = isSelf
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}