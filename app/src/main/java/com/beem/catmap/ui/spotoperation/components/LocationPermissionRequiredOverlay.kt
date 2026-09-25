package com.beem.catmap.ui.spotoperation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.beem.catmap.R
import com.beem.catmap.engine.location.LocationAccessState
import com.beem.catmap.ui.spotoperation.SpotOperationIntent
import com.beem.catmap.ui.theme.CatMapColors

@Composable
fun LocationPermissionRequiredOverlay(
    accessState: LocationAccessState,
    onIntent: (SpotOperationIntent) -> Unit,
    modifier: Modifier = Modifier
) {
    // 🎨 Duruma göre metin, ikon ve buton aksiyonlarını belirleyelim:
    val title = when (accessState) {
        LocationAccessState.GPS_DISABLED -> "Konum Servisi Kapalı"
        LocationAccessState.PERMISSION_PERMANENT -> "Konum İzni Devre Dışı"
        else -> "Konum Doğrulaması Gerekli"
    }

    val description = when (accessState) {
        LocationAccessState.GPS_DISABLED -> {
            "Çevrenizdeki besleme noktalarını radarda görebilmek ve noktaya olan 50m mesafenizi doğrulayabilmek için telefonunuzun konum servisini açmalısınız."
        }
        LocationAccessState.PERMISSION_PERMANENT -> {
            "Konum iznini daha önce kapattığınız için sistem pencereyi açamıyor. Devam edebilmek için uygulama ayarlarından konuma izin vermelisiniz."
        }
        else -> {
            "Sokaktaki canlarımızın beslenme haritasını güncel tutabilmek ve noktaya olan mesafenizi hesaplayabilmek için konum erişimine izin vermelisiniz."
        }
    }

    val buttonText = when (accessState) {
        LocationAccessState.GPS_DISABLED -> "Konumu Etkinleştir"
        LocationAccessState.PERMISSION_PERMANENT -> "Uygulama Ayarlarını Aç"
        else -> "Konum İzni Ver"
    }

    val buttonIcon = when (accessState) {
        LocationAccessState.GPS_DISABLED -> R.drawable.ic_location_off // veya varsa ic_gps
        LocationAccessState.PERMISSION_PERMANENT -> R.drawable.ic_settings
        else -> R.drawable.ic_shield
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(CatMapColors.SurfaceWhite)
            .padding(horizontal = 32.dp, vertical = 24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Şık İkon Alanı
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(CatMapColors.Accent.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_location_off),
                    contentDescription = null,
                    tint = CatMapColors.Accent,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = CatMapColors.TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = description,
                fontSize = 14.sp,
                color = CatMapColors.TextSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Normal
            )

            Spacer(modifier = Modifier.height(36.dp))

            // 🚀 Dinamik Aksiyon Butonu
            Button(
                onClick = {
                    when (accessState) {
                        LocationAccessState.GPS_DISABLED -> {
                            onIntent(SpotOperationIntent.ResolveGpsClicked)
                        }
                        LocationAccessState.PERMISSION_PERMANENT -> {
                            onIntent(SpotOperationIntent.OpenAppSettingsClicked)
                        }
                        else -> {
                            onIntent(SpotOperationIntent.RequestLocationPermissionClicked)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth(0.9f)
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = CatMapColors.Accent
                )
            ) {
                Icon(
                    painter = painterResource(buttonIcon),
                    contentDescription = null,
                    tint = CatMapColors.SurfaceWhite,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = CatMapColors.SurfaceWhite
                )
            }
        }
    }
}

@Preview
@Composable
private fun PreviewScreen() {
    LocationPermissionRequiredOverlay(
        accessState = LocationAccessState.GPS_DISABLED,
        onIntent = {}
    )
}