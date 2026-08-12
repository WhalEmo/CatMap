package com.beem.catmap.domain.usecase

import android.util.Log
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.FullProfileData
import com.beem.catmap.data.model.UserBlockedException
import com.beem.catmap.data.repository.FollowRepository
import com.beem.catmap.data.repository.PostRepository
import com.beem.catmap.data.repository.UserBlockRepository
import com.beem.catmap.gonderi.ProfileRepository
import com.beem.catmap.gonderi.UiState
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.auth.exceptions.IsBlockedByException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

class GetProfileFullDataUseCase(
    private val profileRepository: ProfileRepository,
    private val postRepository: PostRepository,
    private val followRepository: FollowRepository,
    private val userManager: CurrentUserManager,
    private val userBlockRepository: UserBlockRepository
) {

    private data class FetchConditions(
        val isSelf: Boolean,
        val amIBlocking: Boolean,
        val cachedUser: Kullanici?,
        val hasValidLocalProfile: Boolean,
        val shouldFetchStats: Boolean
    )

    suspend operator fun invoke(
        targetUserId: String,
        forceRefresh: Boolean = false
    ): Result<FullProfileData> = withContext(Dispatchers.IO) {
        if (targetUserId.isBlank()) {
            return@withContext Result.failure(Exception("Geçersiz Kullanıcı ID"))
        }

        try {
            val conditions = prepareFetchConditions(targetUserId, forceRefresh)
            val resultData = fetchProfileAsyncData(targetUserId, forceRefresh, conditions)
            Result.success(resultData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun prepareFetchConditions(
        targetUserId: String,
        forceRefresh: Boolean
    ): FetchConditions {
        val isSelf = (targetUserId == UserSession.userId)
        Log.d("BLOCKK", targetUserId)
        val amIBlocking = if (!isSelf) {
            userBlockRepository.isUserBlocked(
                kisiId = UserSession.userId,
                targetUserId = targetUserId
            )
        } else false

        val cachedUser = if (isSelf) userManager.getCurrentUser() else null
        val hasValidLocalProfile = isSelf && !cachedUser?.kullaniciAdi.isNullOrBlank()
        val shouldFetchStats =
            hasValidLocalProfile && !forceRefresh && !userManager.isStatsCacheValid()

        return FetchConditions(
            isSelf = isSelf,
            amIBlocking = amIBlocking,
            cachedUser = cachedUser,
            hasValidLocalProfile = hasValidLocalProfile,
            shouldFetchStats = shouldFetchStats
        )
    }

    private suspend fun fetchProfileAsyncData(
        targetUserId: String,
        forceRefresh: Boolean,
        conditions: FetchConditions
    ): FullProfileData = coroutineScope {

        // 1. Önce SADECE Profil Bilgisini Çek (Eğer engellendiysek PERMISSION_DENIED alacağız)
        val profileState =
            profileRepository.getUserProfile(targetUserId, forceRefresh = forceRefresh)

        if (profileState is UiState.BlockedBy) {
            val publicProfile = profileRepository.getPublicUserProfile(targetUserId).getOrNull()
            throw IsBlockedByException(publicProfile = publicProfile)
        }

        var profileData = (profileState as? UiState.Success)?.data
            ?: throw Exception((profileState as? UiState.Error)?.message ?: "Profil yüklenemedi.")


        if (conditions.amIBlocking) {
            throw UserBlockedException(
                message = "Engellediğiniz kullanıcı",
                profile = profileData
            )
        }

        val isFollowingDeferred = if (!conditions.isSelf) {
            async { followRepository.isFollowing(targetUserId, forceRefresh).getOrDefault(false) }
        } else null

        val isFollowedDeferred = if (!conditions.isSelf) {
            async { followRepository.isFollowedBy(targetUserId, forceRefresh).getOrDefault(false) }
        } else null

        val statsDeferred = if (conditions.shouldFetchStats) {
            async { profileRepository.getUserStats(targetUserId).getOrNull() }
        } else null

        val isFollowing = isFollowingDeferred?.await() ?: false
        val isFollowed = isFollowedDeferred?.await() ?: false

        val accessDenied = !conditions.isSelf && !isFollowing

        val postsDeferred = if (!accessDenied) {
            async {
                postRepository.getKullaniciGonderileri(
                    userId = targetUserId,
                    lastDocument = null,
                    forceRefresh = forceRefresh
                )
            }
        } else null

        val freshStats = statsDeferred?.await()
        val postsResult = postsDeferred?.await()

        if (freshStats != null) {
            profileData = profileData.copy(
                takipciSayisi = freshStats.followerCount,
                takipEdilenSayisi = freshStats.followingCount
            )
            userManager.updateFollowCounts(
                takipciSayisi = freshStats.followerCount,
                takipEdilenSayisi = freshStats.followingCount
            )
        }

        val postPageResult = postsResult?.getOrNull()

        FullProfileData(
            profile = profileData,
            posts = postPageResult?.posts ?: emptyList(),
            lastDocument = postPageResult?.lastDocument,
            isLastPage = postPageResult?.isLastPage ?: true,
            followerCount = profileData.takipciSayisi ?: 0L,
            followingCount = profileData.takipEdilenSayisi ?: 0L,
            postCount = profileData.gonderiSayisi ?: 0L,
            isSelfProfile = conditions.isSelf,
            isFollowing = isFollowing,
            isFollowed = isFollowed,
            isAccessDenied = accessDenied,
        )
    }
}