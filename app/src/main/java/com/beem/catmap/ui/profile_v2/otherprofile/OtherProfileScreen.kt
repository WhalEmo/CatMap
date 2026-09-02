package com.beem.catmap.ui.profile_v2.otherprofile

import android.view.View
import androidx.appcompat.widget.AppCompatImageButton
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.graphics.toColorInt
import coil.compose.AsyncImage
import com.beem.catmap.R
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.profile.post.PostUiState
import com.beem.catmap.ui.profile_v2.components.ProfileHeaderSection
import com.beem.catmap.ui.profile_v2.components.ProfilePostsGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreen(
    uiState: OtherProfileUiState,
    onRefresh: () -> Unit,
    onBackClick: () -> Unit,
    onMenuClick: (View) -> Unit,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onUnblockClick: () -> Unit,
    onChatClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBadgeClick: (EquippedBadgeModel) -> Unit,
    onPostClick: (Post) -> Unit,
    onLoadMorePosts: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pullToRefreshState = rememberPullToRefreshState()
    val isRefreshing = uiState.isLoading && uiState.user != null

    val textPrimary = colorResource(R.color.catmap_text_primary)
    val accentColor = colorResource(R.color.catmap_accent)
    val surfaceColor = colorResource(R.color.catmap_surface_white)

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.user?.username.orEmpty().ifBlank { "Profil" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Geri",
                            tint = textPrimary
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
                                setColorFilter("#0F172A".toColorInt())
                                setOnClickListener { view -> onMenuClick(view) }
                            }
                        },
                        modifier = Modifier
                            .size(48.dp)
                            .padding(end = 4.dp)
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = surfaceColor)
            )
        },
        containerColor = surfaceColor,
        modifier = modifier.fillMaxSize()
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                // İlk açılış shimmer/yükleme
                uiState.isLoading && uiState.user == null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = accentColor)
                    }
                }

                // 1. DURUM: Karşı taraf bizi engelledi (Scrollable yapıldı -> Refresh artık çalışır)
                uiState.isBlockedByThem -> {
                    BlockedByThemContent(uiState = uiState)
                }

                // 2. DURUM: Ben engellediysem (Grid çağrılmaz, iç içe geçme tamamen önlendi)
                uiState.isBlockedByMe && uiState.user != null -> {
                    BlockedByMeContent(
                        uiState = uiState,
                        onUnblockClick = onUnblockClick,
                        onFollowersClick = onFollowersClick,
                        onFollowingClick = onFollowingClick,
                        onBadgeClick = onBadgeClick
                    )
                }

                // 3. DURUM: Normal Profil Akışı (Takip ediyor veya gizli hesap)
                uiState.user != null -> {
                    val user = uiState.user

                    val postUiState = PostUiState(
                        posts = uiState.posts,
                        isLoading = uiState.isPostsLoading,
                        isMoreLoading = uiState.isMoreLoading,
                        isLastPage = uiState.isLastPage,
                        isEmpty = uiState.posts.isEmpty() && !uiState.isPostsLoading && !uiState.isAccessDenied,
                        isAccessDenied = uiState.isAccessDenied
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
                                    OtherProfileActionButtons(
                                        followStatus = uiState.followStatus,
                                        isActionLoading = uiState.isActionLoading,
                                        onFollowClick = onFollowClick,
                                        onUnfollowClick = onUnfollowClick,
                                        onUnblockClick = onUnblockClick,
                                        onChatClick = onChatClick
                                    )
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
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = uiState.errorMessage,
                            color = colorResource(R.color.catmap_error),
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

/**
 * Kullanıcıyı ben engellediğimde çizilecek temiz ekran.
 * Grid çağrılmaz, böylece "bu hesap gizli" veya boş kutularla iç içe geçmez.
 */
@Composable
private fun BlockedByMeContent(
    uiState: OtherProfileUiState,
    onUnblockClick: () -> Unit,
    onFollowersClick: () -> Unit,
    onFollowingClick: () -> Unit,
    onBadgeClick: (EquippedBadgeModel) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        val user = uiState.user ?: return

        ProfileHeaderSection(
            user = user,
            followerCount = user.followersCount,
            followingCount = user.followingCount,
            onFollowersClick = onFollowersClick,
            onFollowingClick = onFollowingClick,
            onBadgeClick = onBadgeClick,
            actionButtons = {
                Button(
                    onClick = onUnblockClick,
                    enabled = !uiState.isActionLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colorResource(R.color.catmap_divider),
                        contentColor = colorResource(R.color.catmap_text_primary)
                    )
                ) {
                    if (uiState.isActionLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = colorResource(R.color.catmap_primary),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = "Engeli Kaldır",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        )

        Spacer(modifier = Modifier.height(60.dp))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🚫",
                fontSize = 40.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Bu kullanıcıyı engellediniz",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = colorResource(R.color.catmap_text_primary)
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Paylaşımlarını, takipçilerini ve hareketlerini görmek için engeli kaldırmalısınız.",
                fontSize = 13.sp,
                color = colorResource(R.color.catmap_text_muted),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )
        }
    }
}

/**
 * Takip, Engeli Kaldır, Mesaj butonlarının durum bazlı render edildiği alan
 */
@Composable
private fun OtherProfileActionButtons(
    followStatus: OtherFollowStatus,
    isActionLoading: Boolean,
    onFollowClick: () -> Unit,
    onUnfollowClick: () -> Unit,
    onUnblockClick: () -> Unit,
    onChatClick: () -> Unit
) {
    val accentColor = colorResource(R.color.catmap_accent)
    val dividerColor = colorResource(R.color.catmap_divider)
    val textPrimary = colorResource(R.color.catmap_text_primary)

    if (isActionLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = accentColor,
                strokeWidth = 2.dp
            )
        }
        return
    }

    when (followStatus) {
        OtherFollowStatus.LOADING -> {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = accentColor,
                    strokeWidth = 2.dp
                )
            }
        }

        OtherFollowStatus.BLOCKED_BY_ME -> {
            Button(
                onClick = onUnblockClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = dividerColor,
                    contentColor = textPrimary
                )
            ) {
                Text(text = "Engeli Kaldır", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        OtherFollowStatus.NOT_FOLLOWING -> {
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Takip Et", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        OtherFollowStatus.FOLLOW_BACK -> {
            Button(
                onClick = onFollowClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                Text(text = "Sen de Takip Et", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        OtherFollowStatus.FOLLOWING -> {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onUnfollowClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dividerColor,
                        contentColor = textPrimary
                    )
                ) {
                    Text(text = "Takip Ediliyor", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = onChatClick,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = dividerColor,
                        contentColor = textPrimary
                    )
                ) {
                    Text(text = "Mesaj", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}

/**
 * Karşı taraf bizi engellediğinde gösterilecek kilitli profil sayfası.
 * Dikey scroll eklendiğinden PullToRefresh hareketi artık engellenmez.
 */
@Composable
private fun BlockedByThemContent(uiState: OtherProfileUiState) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = 60.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        AsyncImage(
            model = uiState.user?.photoUrl,
            contentDescription = null,
            placeholder = painterResource(R.drawable.kullanici),
            error = painterResource(R.drawable.kullanici),
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .border(2.dp, colorResource(R.color.catmap_divider), CircleShape)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = uiState.user?.username.orEmpty().ifBlank { "Kullanıcı" },
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.catmap_text_primary)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Kullanıcı Bulunamadı",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = colorResource(R.color.catmap_text_primary)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Bu hesap kullanılamıyor veya profile erişim izniniz bulunmuyor.",
            fontSize = 14.sp,
            color = colorResource(R.color.catmap_text_muted),
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )
    }
}