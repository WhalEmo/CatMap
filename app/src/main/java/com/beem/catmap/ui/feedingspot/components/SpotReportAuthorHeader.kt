package com.beem.catmap.ui.feedingspot.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.data.model.SpotReport
import com.beem.catmap.ui.theme.CatMapColors
import com.beem.catmap.utils.formatTimeAgo
import kotlin.text.ifEmpty

@Composable
fun SpotReportAuthorHeader(
    report: SpotReport,
    isViewingHistory: Boolean,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(CatMapColors.SurfaceWhite)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(CatMapColors.SurfaceTranslucent),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_person),
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = CatMapColors.TextMuted
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = report.reporterName.ifEmpty { "Gönüllü" },
                fontWeight = FontWeight.Bold,
                color = CatMapColors.TextPrimary,
                fontSize = 14.sp
            )
            Text(
                text = report.reportedAt.formatTimeAgo(),
                color = CatMapColors.TextSecondary,
                fontSize = 12.sp
            )
        }

        if (isViewingHistory) {
            Surface(
                color = CatMapColors.SurfaceTranslucent,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CatMapColors.Divider)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_clock),
                        contentDescription = null,
                        tint = CatMapColors.TextSecondary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Önceki Durum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CatMapColors.TextSecondary
                    )
                }
            }
        } else {
            Surface(
                color = CatMapColors.Accent.copy(alpha = 0.12f),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CatMapColors.Accent.copy(alpha = 0.35f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    // Yeşil/Canlı durum sinyali veren mini nokta
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(CatMapColors.Accent)
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Son Durum",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = CatMapColors.Accent
                    )
                }
            }
        }

        /*
        if (isViewingHistory) {
            Surface(
                color = CatMapColors.SurfaceWhite,
                shape = RoundedCornerShape(6.dp),
                border = BorderStroke(1.dp, CatMapColors.Divider)
            ) {
                Text(
                    text = "Geçmiş Kayıt",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CatMapColors.TextMuted,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }

         */
    }
}