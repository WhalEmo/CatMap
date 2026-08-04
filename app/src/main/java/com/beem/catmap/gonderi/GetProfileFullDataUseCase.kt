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
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository
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

            val result: Result<FullProfileData> = coroutineScope {
                // 1. Profil Bilgilerini Async Çek
                val profileDeferred = async {
                    profileRepository.getUserProfile(targetUserId, forceRefresh)
                }

                // 2. Post Bilgilerini Async Çek
                val postsDeferred = async {
                    postRepository.getUserPosts(targetUserId, forceRefresh)
                }

                // 3. Takip/Takipçi Sayılarını Async Çek
                val followCountsDeferred = async {
                    followRepository.fetchAndCacheFollowCounts(
                        userId = targetUserId,
                        isMyProfile = isSelf,
                        forceRefresh = forceRefresh
                    )
                }

                // Tüm async işlemlerin tamamlanmasını bekle
                val profileState = profileDeferred.await()
                val postsResult = postsDeferred.await()
                val followCountsResult = followCountsDeferred.await()

                // Profil Verisini Doğrula
                val profileData = (profileState as? UiState.Success)?.data
                    ?: return@coroutineScope Result.failure(
                        Exception((profileState as? UiState.Error)?.message ?: "Profil yüklenemedi.")
                    )

                // Post Verisini Doğrula (Ağ hatası alsa bile boş liste ile devam etsin, ekran çökmesin)
                val postsCache = postsResult.getOrElse {
                    ProfilePostCacheData(emptyList(), emptyList(), 0, true)
                }

                // Takip Sayılarını Doğrula
                val followCounts = followCountsResult.getOrDefault(FollowCounts(0L, 0L))

                Result.success(
                    FullProfileData(
                        profile = profileData,
                        postsCache = postsCache,
                        followerCount = followCounts.followerCount,
                        followingCount = followCounts.followingCount,
                        isSelfProfile = isSelf,
                    )
                )
            }

            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}