package com.beem.catmap.ui.feedingspot.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.data.model.FeedingSpot
import com.beem.catmap.ui.theme.CatMapColors
import kotlin.text.ifEmpty

@Composable
fun SpotDetailTopBar(
    spot: FeedingSpot,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(CatMapColors.SurfaceWhite)
            .padding(top = 10.dp, bottom = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .width(36.dp)
                .height(3.dp)
                .clip(CircleShape)
                .background(CatMapColors.Divider)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = buildAnnotatedString {
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.ExtraBold,
                        color = CatMapColors.TextPrimary
                    )
                ) {
                    append(spot.spotName.ifEmpty { "İsimsiz Nokta" })
                }
                withStyle(
                    style = SpanStyle(
                        fontWeight = FontWeight.Medium,
                        color = CatMapColors.TextMuted,
                        fontSize = 14.sp
                    )
                ) {
                    append(" • Besleme Noktası")
                }
            },
            fontSize = 18.sp
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_location_puck),
                contentDescription = null,
                tint = CatMapColors.Accent,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "${spot.district} · ${spot.neighborhood}",
                fontSize = 12.sp,
                color = CatMapColors.TextSecondary
            )
        }
    }
}