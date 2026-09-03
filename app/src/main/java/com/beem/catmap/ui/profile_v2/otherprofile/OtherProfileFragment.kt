package com.beem.catmap.ui.profile_v2.otherprofile

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.beem.catmap.R
import com.beem.catmap.ui.badge.BadgeStoryBottomSheet
import com.beem.catmap.ui.components.CatMapPopupMenu
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.profile.common.ProfileDialogHelper
import com.beem.catmap.ui.profile.follow.fragment.FollowFragment
import com.beem.catmap.ui.report.ReportType

class OtherProfileFragment : Fragment() {

    private var targetUserId: String = ""

    private val viewModel: OtherProfileViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return OtherProfileViewModel(requireActivity().application, targetUserId) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        targetUserId = arguments?.getString(ARG_USER_ID).orEmpty()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(requireContext()).apply {
            setContent {
                val uiState by viewModel.uiState.collectAsState()

                OtherProfileScreen(
                    uiState = uiState,
                    onRefresh = { viewModel.loadProfile(forceRefresh = true) },
                    onBackClick = { SmartNavigationEngine.navigateBack() },
                    onMenuClick = { anchorView -> showOptionMenu(anchorView, uiState) },
                    onFollowClick = { viewModel.followUser() },
                    onUnfollowClick = { viewModel.unfollowUser() },
                    onUnblockClick = { viewModel.unblockUser() },
                    onChatClick = { NavigationHelper.navigateToChat(targetUserId) },
                    onFollowersClick = {
                        val args = FollowFragment.newArgs(
                            userId = targetUserId,
                            username = uiState.user?.username.orEmpty(),
                            startPage = FollowFragment.PAGE_FOLLOWERS
                        )
                        SmartNavigationEngine.navigateTo(Screen.FOLLOWERS, args, "${targetUserId}_${FollowFragment.PAGE_FOLLOWERS}")
                    },
                    onFollowingClick = {
                        val args = FollowFragment.newArgs(
                            userId = targetUserId,
                            username = uiState.user?.username.orEmpty(),
                            startPage = FollowFragment.PAGE_FOLLOWING
                        )
                        SmartNavigationEngine.navigateTo(Screen.FOLLOWERS, args, "${targetUserId}_${FollowFragment.PAGE_FOLLOWING}")
                    },
                    onBadgeClick = { badge ->
                        BadgeStoryBottomSheet.show(childFragmentManager, badge)
                    },
                    onPostClick = { post ->
                        val args = bundleOf(
                            "kediid" to post.catId,
                            "yukleyenId" to targetUserId
                        )
                        SmartNavigationEngine.navigateTo(Screen.POST, args, post.catId)
                    },
                    onLoadMorePosts = { viewModel.loadMorePosts() }
                )
            }
        }
    }

    private fun showOptionMenu(view: View, state: OtherProfileUiState) {
        val redColor = ContextCompat.getColor(requireContext(), R.color.catmap_error)
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.catmap_text_primary)
        val isBlocked = state.isBlockedByMe

        CatMapPopupMenu.Builder(requireContext())
            .addItem(
                id = R.id.profiltakipciCikar,
                title = "Takipçiden Çıkar",
                iconRes = R.drawable.ic_unfollow_user,
                textColor = textPrimaryColor,
                iconTint = textPrimaryColor,
                isVisible = state.isMyFollower
            ) {
                ProfileDialogHelper.showUnfollowerDialog(
                    fragmentManager = childFragmentManager,
                    kullaniciAdi = state.user?.username,
                    onConfirm = { viewModel.removeFollower() }
                )
            }
            .addItem(
                id = if (isBlocked) R.id.profilmenu_engeliKaldir else R.id.profilmenu_engelle,
                title = if (isBlocked) "Engeli Kaldır" else "Kullanıcıyı Engelle",
                iconRes = if (isBlocked) R.drawable.ic_lock_open else R.drawable.ic_lock,
                textColor = if (isBlocked) textPrimaryColor else redColor,
                iconTint = if (isBlocked) textPrimaryColor else redColor,
                isVisible = true
            ) {
                if (isBlocked) {
                    ProfileDialogHelper.showUnblockDialog(
                        fragmentManager = childFragmentManager,
                        kullaniciAdi = state.user?.username,
                        onConfirm = { viewModel.unblockUser() }
                    )
                } else {
                    ProfileDialogHelper.showBlockDialog(
                        fragmentManager = childFragmentManager,
                        kullaniciAdi = state.user?.username,
                        onConfirm = { viewModel.blockUser() }
                    )
                }
            }
            .addItem(
                id = 3,
                title = "Profili Bildir",
                iconRes = R.drawable.ic_error_outline,
                textColor = redColor,
                iconTint = redColor,
                isVisible = !state.isBlockedByThem
            ) {
                NavigationHelper.showReportBottomSheet(
                    childFragmentManager,
                    targetId = targetUserId,
                    reportType = ReportType.PROFILE
                )
            }
            .build()
            .show(anchorView = view)
    }

    companion object {
        private const val ARG_USER_ID = "USER_ID"

        fun newArgs(userId: String): Bundle {
            return bundleOf(ARG_USER_ID to userId)
        }
    }
}