package com.beem.catmap.ui.profile_v2.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.components.CatMapImage
import com.beem.catmap.ui.profile.post.PostUiState
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.ui.theme.Montserrat
import com.beem.catmap.ui.theme.PlusJakartaSans

@Composable
fun ProfilePostsGrid(
    headerContent: @Composable () -> Unit,
    postUiState: PostUiState,
    onPostClick: (Post) -> Unit,
    onLoadMore: () -> Unit,
    refreshKey: Any,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(3) }) {
            headerContent()
        }

        // Bölüm Ayırıcı Başlık ("GÖNDERİLER")
        item(span = { GridItemSpan(3) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(CatMapColors.Divider)
                )
                Text(
                    text = "GÖNDERİLER",
                    fontFamily = Montserrat,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.1.sp,
                    color = CatMapColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(CatMapColors.Divider)
                )
            }
        }

        when {
            postUiState.isAccessDenied -> {
                item(span = { GridItemSpan(3) }) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = CatMapColors.TextMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "Bu hesap gizli.",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = CatMapColors.TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Gönderilerini görmek için takip etmelisiniz.",
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.Center,
                            fontSize = 13.sp,
                            color = CatMapColors.TextMuted
                        )
                    }
                }
            }

            postUiState.isEmpty -> {
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = "Henüz gönderi yok",
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium,
                        textAlign = TextAlign.Center,
                        fontSize = 14.sp,
                        color = CatMapColors.TextMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp)
                    )
                }
            }

            else -> {
                itemsIndexed(
                    items = postUiState.posts,
                    key = { index, post -> "${post.catId ?: "cat"}_$index" }
                ) { index, post ->
                    if (index >= postUiState.posts.size - 2 && !postUiState.isLastPage && !postUiState.isMoreLoading) {
                        LaunchedEffect(postUiState.posts.size) {
                            onLoadMore()
                        }
                    }

                    val firstImageUrl = post.photoUrlList.firstOrNull()
                    CatMapImage(
                        data = firstImageUrl,
                        contentDescription = post.catName,
                        refreshKey = refreshKey,
                        modifier = Modifier
                            .aspectRatio(1f)
                            .padding(1.dp)
                            .clickable { onPostClick(post) }
                    )
                }

                if (postUiState.isMoreLoading) {
                    item(span = { GridItemSpan(3) }) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = CatMapColors.Accent,
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            }
        }
    }
}