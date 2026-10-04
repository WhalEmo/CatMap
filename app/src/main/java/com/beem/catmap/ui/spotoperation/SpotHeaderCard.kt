package com.beem.catmap.ui.spotoperation

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.ui.theme.CatMapColors

@Composable
fun SpotHeaderCard(
    spotName: String,
    spotLocation: String,
    shieldState: ShieldState,
    distance: Int?,
    onRetry: () -> Unit
) {
    val (shieldBgColor, shieldContentColor, shieldIcon, shieldTitle, shieldDesc) = when (shieldState) {
        ShieldState.SCANNING -> listOf(
            Color(0xFFF3F4F6), CatMapColors.TextPrimary, R.drawable.ic_radar,
            "Konum Aranıyor...",
            "Noktanın yakınlarında olduğun teyit ediliyor."
        )
        ShieldState.VERIFIED -> listOf(
            CatMapColors.Success.copy(alpha = 0.1f), CatMapColors.Success, R.drawable.ic_check_circle,
            "Konum Doğrulandı",
            "Noktaya ${distance ?: 0} metre mesafedesin. Gelişmeleri toplulukla paylaşabilirsin." // 🚀 Harika mesafe detayı!
        )
        ShieldState.TOO_FAR -> listOf(
            CatMapColors.Error.copy(alpha = 0.1f), CatMapColors.Error, R.drawable.ic_error_outline,
            "Biraz Daha Yaklaşmalısın",
            "Noktaya ${distance ?: "?"} metre uzaksın. Durum bildirimi yapabilmek için 50 metre yakınına gitmelisin."
        )
        ShieldState.ERROR -> listOf(
            CatMapColors.Badge.Gold.copy(alpha = 0.15f), CatMapColors.Badge.Gold, R.drawable.ic_warning,
            "GPS Sinyali Yok",
            "Konum servislerini açıp tekrar deneyebilirsin."
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = CatMapColors.SurfaceWhite,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, CatMapColors.Divider)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // ÜST KISIM: NOKTA KİMLİĞİ
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Nokta İkonu (Yuvarlak içinde)
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(CatMapColors.Accent.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_location_puck),
                        contentDescription = null,
                        tint = CatMapColors.Accent,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Nokta Bilgileri
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = spotName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = CatMapColors.TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = spotLocation,
                        fontSize = 13.sp,
                        color = CatMapColors.TextSecondary
                    )
                }
            }

            // AYIRICI ÇİZGİ
            Divider(color = CatMapColors.Divider, thickness = 1.dp)

            // ALT KISIM: GPS KALKANI
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(shieldBgColor as Color)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = shieldIcon as Int),
                    contentDescription = null,
                    tint = shieldContentColor as Color,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shieldTitle as String,
                        fontWeight = FontWeight.Bold,
                        color = shieldContentColor,
                        fontSize = 13.sp
                    )
                    Text(
                        text = shieldDesc as String,
                        color = if (shieldState == ShieldState.SCANNING) CatMapColors.TextSecondary else shieldContentColor,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }

                if (shieldState == ShieldState.TOO_FAR || shieldState == ShieldState.ERROR) {
                    IconButton(onClick = onRetry, modifier = Modifier.size(32.dp)) {
                        Icon(painterResource(id = R.drawable.ic_refresh_minimal), contentDescription = "Tekrar", tint = shieldContentColor)
                    }
                }
            }
        }
    }
}