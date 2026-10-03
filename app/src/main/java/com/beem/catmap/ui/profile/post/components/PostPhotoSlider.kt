package com.beem.catmap.ui.profile.post.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.beem.catmap.ui.theme.CatMapColors

@Composable
fun PostPhotoSlider(
    photos: List<String>,
    onPhotoClick: (Int) -> Unit
) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { photos.size })

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Yatay Pager (Akıcı kaydırma)
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { pageIndex ->
            PostPhotoItem(
                url = photos[pageIndex],
                onPhotoClick = { onPhotoClick(pageIndex) }
            )
        }

        // Nokta İndikatörü
        if (photos.size > 1) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 12.dp)
                    .height(16.dp),
                shape = RoundedCornerShape(8.dp),
                color = CatMapColors.PrimaryDark.copy(alpha = 0.45f)
            ) {
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .fillMaxHeight()
                        .padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(photos.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width by animateDpAsState(
                            targetValue = if (isSelected) 14.dp else 5.dp,
                            label = "dotWidth"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) CatMapColors.Accent else Color.White.copy(alpha = 0.5f),
                            label = "dotColor"
                        )

                        Box(
                            modifier = Modifier
                                .height(5.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }
            }
        }
    }
}