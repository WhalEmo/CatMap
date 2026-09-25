package com.beem.catmap.ui.feedingspot

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.rememberNestedScrollInteropConnection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.data.model.ReportAction
import com.beem.catmap.data.model.SpotReport
import com.beem.catmap.data.model.SpotState
import com.beem.catmap.ui.components.SmartSpotImage
import com.beem.catmap.ui.feedingspot.components.SpotDetailActionButton
import com.beem.catmap.ui.feedingspot.components.SpotDetailTopBar
import com.beem.catmap.ui.feedingspot.components.SpotReportAuthorHeader
import com.beem.catmap.ui.feedingspot.components.SpotReportPhotoPager
import com.beem.catmap.ui.feedingspot.components.SpotReportStatusCard
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.utils.formatExactTime
import com.beem.catmap.utils.formatTimeAgo
import java.util.concurrent.TimeUnit
import kotlin.collections.getOrNull

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun FeedingSpotDetailScreenHybrid(
    spot: FeedingSpot,
    uiState: FeedingSpotUiState,
    reports: List<SpotReport>,
    onUpdateClick: () -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { reports.size })
    val currentReport = reports.getOrNull(pagerState.currentPage)
    val isViewingHistory = pagerState.currentPage > 0

    val reportAgeMinutes = remember(currentReport?.reportedAt) {
        if (currentReport == null) 0L
        else TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - currentReport.reportedAt)
    }

    val decayBadge = remember(reportAgeMinutes) {
        getDecayLevel(reportAgeMinutes).toUiBadge()
    }

    // ANA İSKELET (Tüm ekranın yüksekliğini kaplayabilmesi için)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(CatMapColors.Background)
    ) {
        SpotDetailTopBar(spot = spot)

        HorizontalDivider(color = CatMapColors.Divider, thickness = 1.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = false)
        ) {
            when (uiState) {
                is FeedingSpotUiState.Error -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_error_outline),
                            contentDescription = null,
                            tint = CatMapColors.Error,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = uiState.message, color = CatMapColors.TextSecondary, fontSize = 14.sp)
                    }
                }
                is FeedingSpotUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator(color = CatMapColors.Accent, modifier = Modifier.size(36.dp))
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(text = uiState.message, color = CatMapColors.TextSecondary, fontSize = 14.sp)
                    }
                }
                else -> {
                    if (currentReport == null) {
                        Box(
                            modifier = Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Henüz hiç rapor girilmemiş.", color = CatMapColors.TextMuted)
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .nestedScroll(rememberNestedScrollInteropConnection())
                                .verticalScroll(rememberScrollState())
                        ) {

                            AnimatedContent(
                                targetState = pagerState.currentPage,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally(animationSpec = tween(350)) { width -> width } + fadeIn(animationSpec = tween(350)))
                                            .togetherWith(
                                                slideOutHorizontally(animationSpec = tween(350)) { width -> -width } + fadeOut(animationSpec = tween(250))
                                            )
                                    } else {
                                        (slideInHorizontally(animationSpec = tween(350)) { width -> -width } + fadeIn(animationSpec = tween(350)))
                                            .togetherWith(
                                                slideOutHorizontally(animationSpec = tween(350)) { width -> width } + fadeOut(animationSpec = tween(250))
                                            )
                                    }
                                },
                                label = "AuthorHeaderTransition"
                            ) { targetPage ->
                                val pageReport = reports.getOrNull(targetPage)
                                if (pageReport != null) {
                                    SpotReportAuthorHeader(
                                        report = pageReport,
                                        isViewingHistory = targetPage > 0
                                    )
                                }
                            }
                            SpotReportPhotoPager(
                                reports = reports,
                                pagerState = pagerState
                            )

                            AnimatedContent(
                                targetState = pagerState.currentPage,
                                transitionSpec = {
                                    if (targetState > initialState) {
                                        (slideInHorizontally(animationSpec = tween(350)) { width -> width } + fadeIn(animationSpec = tween(350)))
                                            .togetherWith(
                                                slideOutHorizontally(animationSpec = tween(350)) { width -> -width } + fadeOut(animationSpec = tween(250))
                                            )
                                    } else {
                                        (slideInHorizontally(animationSpec = tween(350)) { width -> -width } + fadeIn(animationSpec = tween(350)))
                                            .togetherWith(
                                                slideOutHorizontally(animationSpec = tween(350)) { width -> width } + fadeOut(animationSpec = tween(250))
                                            )
                                    }
                                },
                                label = "ReportCardPageTransition"
                            ) { targetPage ->
                                val pageReport = reports.getOrNull(targetPage)
                                if (pageReport != null) {
                                    val pageIsHistory = targetPage > 0
                                    val pageAgeMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - pageReport.reportedAt)
                                    val pageDecayBadge = getDecayLevel(pageAgeMinutes).toUiBadge()

                                    SpotReportStatusCard(
                                        spot = spot,
                                        report = pageReport,
                                        isViewingHistory = pageIsHistory,
                                        decayBadgeText = pageDecayBadge.text,
                                        decayBadgeContentColor = pageDecayBadge.contentColor,
                                        decayBadgeContainerColor = pageDecayBadge.containerColor,
                                        decayBadgeBorderColor = pageDecayBadge.borderColor
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }

        SpotDetailActionButton(onClick = onUpdateClick)
    }
}