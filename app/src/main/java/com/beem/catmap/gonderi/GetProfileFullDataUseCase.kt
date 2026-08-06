package com.beem.catmap.domain.usecase

import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.FullProfileData
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.gonderi.ProfileRepository
import com.beem.catmap.gonderi.UiState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class GetProfileFullDataUseCase(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
) {
    suspend operator fun invoke(
        targetUserId: String,
        isFollowing: Boolean = false,
        forceRefresh: Boolean = false
    ): Result<FullProfileData> = withContext(Dispatchers.IO) {
        if (targetUserId.isBlank()) {
            return@withContext Result.failure(Exception("Geçersiz Kullanıcı ID"))
        }

        try {

            val isSelf = (targetUserId == UserSession.userId)
            val accessDenied = !isSelf && !isFollowing

            coroutineScope {
                val profileDeferred = async {
                    profileRepository.getUserProfile(targetUserId, forceRefresh = forceRefresh)
                }

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
                        isAccessDenied = accessDenied
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}