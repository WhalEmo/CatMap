package com.beem.catmap.ui.profile_v2.myprofile

import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.appcompat.widget.AppCompatImageButton
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.beem.catmap.R
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.profile.post.PostUiState
import com.beem.catmap.ui.profile_v2.components.ProfileHeaderSection
import com.beem.catmap.ui.profile_v2.components.ProfilePostsGrid
import androidx.core.graphics.toColorInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    uiState: MyProfileUiState,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onMenuClick: (View) -> Unit,
    onEditProfileClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBadgeClick: (EquippedBadgeModel) -> Unit,
    onPostClick: (Post) -> Unit,
    onLoadMorePosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.user?.username.orEmpty().ifBlank { "Profilim" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = Color.Black
                        )
                    }
                },
                actions = {
                    AndroidView(
                        factory = { context ->
                            AppCompatImageButton(
                                context,
                                null,
                                androidx.appcompat.R.attr.actionButtonStyle
                            ).apply {
                                setImageResource(R.drawable.baseline_more_vert_24)
                                setColorFilter("#888888".toColorInt())

                                setOnClickListener { view ->
                                    onMenuClick(view)
                                }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 4.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color.White,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = uiState.isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFFFF9800))
                    }
                }
                uiState.user != null -> {
                    val user = uiState.user

                    // PostUiState köprüsü (Mevcut ProfilePostsGrid bileşeni için)
                    val postUiState = PostUiState(
                        posts = uiState.posts,
                        isLoading = uiState.isPostsLoading,
                        isMoreLoading = uiState.isMoreLoading,
                        isLastPage = uiState.isLastPage,
                        isEmpty = uiState.posts.isEmpty() && !uiState.isPostsLoading,
                        isAccessDenied = false
                    )

                    ProfilePostsGrid(
                        headerContent = {
                            ProfileHeaderSection(
                                user = user,
                                followerCount = user.followersCount,
                                followingCount = user.followingCount,
                                onFollowersClick = onFollowersClick,
                                onFollowingClick = onFollowingClick,
                                onBadgeClick = onBadgeClick,
                                actionButtons = {
                                    Button(
                                        onClick = onEditProfileClick,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(42.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = Color(0xFFFFE0B2)
                                        ),
                                        border = BorderStroke(1.dp, Color(0xFFFF9800))
                                    ) {
                                        Text(
                                            text = "Profili düzenle",
                                            color = Color.Black,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            )
                        },
                        postUiState = postUiState,
                        onPostClick = onPostClick,
                        onLoadMore = onLoadMorePosts
                    )
                }
                uiState.errorMessage != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}