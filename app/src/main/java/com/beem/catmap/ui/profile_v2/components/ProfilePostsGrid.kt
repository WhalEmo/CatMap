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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.beem.catmap.R
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.profile.post.PostUiState

@Composable
fun ProfilePostsGrid(
    headerContent: @Composable () -> Unit,
    postUiState: PostUiState,
    onPostClick: (Post) -> Unit,
    onLoadMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        modifier = modifier.fillMaxSize()
    ) {
        item(span = { GridItemSpan(3) }) {
            headerContent()
        }

        item(span = { GridItemSpan(3) }) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE6E6E6))
                )
                Text(
                    text = "GÖNDERİLER",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF666666),
                    modifier = Modifier.padding(horizontal = 14.dp)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(1.dp)
                        .background(Color(0xFFE6E6E6))
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
                            .padding(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = null,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "🔒 Bu hesap gizli.\nGönderilerini görmek için takip et.",
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            postUiState.isEmpty -> {
                item(span = { GridItemSpan(3) }) {
                    Text(
                        text = "Henüz gönderi yok",
                        textAlign = TextAlign.Center,
                        fontSize = 15.sp,
                        color = Color(0xFF888888),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp)
                    )
                }
            }
            else -> {
                // items yerine itemsIndexed kullanılarak her öğeye garantili benzersiz key verildi
                itemsIndexed(
                    items = postUiState.posts,
                    key = { index, post -> "${post.catId ?: "cat"}_$index" }
                ) { index, post ->
                    // Son 2 öğeye gelindiğinde daha fazla veri yükle
                    if (index >= postUiState.posts.size - 2 && !postUiState.isLastPage && !postUiState.isMoreLoading) {
                        LaunchedEffect(postUiState.posts.size) {
                            onLoadMore()
                        }
                    }

                    val firstImageUrl = post.photoUrlList.firstOrNull()
                    AsyncImage(
                        model = firstImageUrl,
                        contentDescription = post.catName,
                        placeholder = painterResource(R.drawable.kullanici),
                        error = painterResource(R.drawable.kullanici),
                        contentScale = ContentScale.Crop,
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
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        }
                    }
                }
            }
        }
    }
}