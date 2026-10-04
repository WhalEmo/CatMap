package com.beem.catmap.ui.markersclick.components

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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.beem.catmap.ui.theme.CatMapColors
import kotlin.math.abs

@Composable
fun CatPhotoSlider(
    photos: List<String>,
    onPhotoClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    if (photos.isEmpty()) return

    val pagerState = rememberPagerState(pageCount = { photos.size })

    val density = LocalDensity.current
    // Yaklaşık 20dp'lik tolerans payı
    val dragThresholdPx = remember(density) { with(density) { 20.dp.toPx() } }

    val dampedVerticalScrollConnection = remember(dragThresholdPx) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val dx = abs(available.x)
                val dy = abs(available.y)

                // 1. Kullanıcı belirgin şekilde yatay kaydırıyorsa dikey kaçakları yut
                if (dx > dy) {
                    return Offset(0f, available.y)
                }

                // 2. Dikey hareket çok küçükse (titreme/ufak kayma) BottomSheet'i tetikletme
                if (dy < dragThresholdPx) {
                    return Offset(0f, available.y)
                }

                // 3. Kararlı bir dikey çekme varsa hareketi %45 oranında ilet (aşırı ani kapanmayı engeller)
                val consumedY = available.y * 0.55f
                return Offset(0f, consumedY)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(dampedVerticalScrollConnection),
        contentAlignment = Alignment.BottomCenter
    ) {
        // 1. Akıcı Compose Yatay Kaydırıcı
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            pageSpacing = 0.dp
        ) { pageIndex ->
            CatPhotoItem(
                url = photos[pageIndex],
                onOpenFullScreen = { onPhotoClick(pageIndex) }
            )
        }

        // 2. Sayfa İndikatör Kapsülü
        if (photos.size > 1) {
            Surface(
                modifier = Modifier
                    .padding(bottom = 20.dp)
                    .height(18.dp),
                shape = RoundedCornerShape(9.dp),
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
                            targetValue = if (isSelected) 14.dp else 6.dp,
                            label = "dotWidth"
                        )
                        val color by animateColorAsState(
                            targetValue = if (isSelected) CatMapColors.Accent else Color.White.copy(alpha = 0.5f),
                            label = "dotColor"
                        )

                        Box(
                            modifier = Modifier
                                .height(6.dp)
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