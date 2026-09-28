package com.beem.catmap.ui.feedingspot.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.load
import coil.request.CachePolicy
import com.beem.catmap.ui.components.SmartSpotImage
import com.beem.catmap.ui.feedingspot.model.SpotReportUiModel
import com.beem.catmap.ui.theme.CatMapColors
import com.bumptech.glide.Glide
import com.stfalcon.imageviewer.StfalconImageViewer


@Composable
fun SpotReportPhotoPager(
    reports: List<SpotReportUiModel>,
    pagerState: PagerState,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val validPhotoUrls = remember(reports) {
        reports.mapNotNull { it.report.photoUrl.takeIf { url -> url.isNotBlank() } }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .background(CatMapColors.SurfaceTranslucent)
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val currentPhotoUrl = reports.getOrNull(page)?.report?.photoUrl

            SmartSpotImage(
                imageUrl = reports.getOrNull(page)?.report?.photoUrl,
                modifier = Modifier.fillMaxSize()
                    .clickable(enabled = !currentPhotoUrl.isNullOrBlank()) {
                        val targetIndex = validPhotoUrls.indexOf(currentPhotoUrl).coerceAtLeast(0)
                        openFullScreenImageViewer(
                            context = context,
                            urls = validPhotoUrls,
                            startPosition = targetIndex
                        )
                    }
            )
        }

        if (reports.size > 1) {
            Surface(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(12.dp),
                color = CatMapColors.Black.copy(alpha = 0.6f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "${pagerState.currentPage + 1}/${reports.size}",
                    color = CatMapColors.White,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp)
                    .background(CatMapColors.IndicatorCapsule, RoundedCornerShape(8.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(reports.size) { iteration ->
                    val color = if (pagerState.currentPage == iteration) {
                        CatMapColors.Accent
                    } else {
                        CatMapColors.SurfaceWhiteTrans60
                    }
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(color)
                    )
                }
            }
        }
    }
}

private fun openFullScreenImageViewer(
    context: Context,
    urls: List<String>,
    startPosition: Int = 0
) {
    if (urls.isEmpty()) return

    StfalconImageViewer.Builder<String>(context, urls) { imageView, url ->
        imageView.load(url) {
            crossfade(true)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
        }
    }
        .withStartPosition(startPosition)
        .withHiddenStatusBar(false)
        .allowSwipeToDismiss(true)
        .show()
}