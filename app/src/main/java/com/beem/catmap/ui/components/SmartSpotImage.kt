package com.beem.catmap.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.beem.catmap.R
import com.beem.catmap.ui.theme.CatMapColors

@Composable
fun SmartSpotImage(
    imageUrl: String?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Retry butonu tetikleyicisi (Değer değiştiğinde Coil resmi baştan çeker)
    var retryTrigger by remember { mutableIntStateOf(0) }

    val imageRequest = remember(imageUrl, retryTrigger) {
        ImageRequest.Builder(context)
            .data(imageUrl)
            .crossfade(true)
            .crossfade(300)
            .memoryCachePolicy(if (retryTrigger > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED) // Hata sonrası denerken bozuk cache'i del
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    SubcomposeAsyncImage(
        model = imageRequest,
        contentDescription = "Saha Raporu",
        modifier = modifier.background(CatMapColors.SurfaceTranslucent),
        contentScale = ContentScale.Crop
    ) {
        when (val state = painter.state) {

            // 1. YÜKLENİYOR DURUMU
            is AsyncImagePainter.State.Loading -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    // Ortada şeffaf kedi ikonu, etrafında dönen loading
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cat),
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = CatMapColors.TextMuted.copy(alpha = 0.3f)
                    )
                    CircularProgressIndicator(
                        color = CatMapColors.Accent,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(64.dp)
                    )
                }
            }

            // 2. BAŞARILI DURUM
            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }

            // 3. HATA DURUMU
            is AsyncImagePainter.State.Error -> {
                val errorMsg = state.result.throwable.message ?: ""
                // 401 Unauthorized veya 403 Forbidden kontrolü
                val isAuthError = errorMsg.contains("401") || errorMsg.contains("403") ||
                        errorMsg.contains("Unauthorized") || errorMsg.contains("Forbidden")

                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cat),
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = CatMapColors.Error.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    if (isAuthError) {
                        Text(
                            text = "Görsele erişim yetkiniz yok.",
                            color = CatMapColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    } else {
                        Text(
                            text = "Bağlantı koptu veya görsel bulunamadı.",
                            color = CatMapColors.TextSecondary,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        OutlinedButton(
                            onClick = { retryTrigger++ },
                            shape = RoundedCornerShape(20.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CatMapColors.Accent),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CatMapColors.Accent)
                        ) {
                            Icon(imageVector = Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tekrar Dene", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // 4. BOŞ DURUM (URL null ise)
            is AsyncImagePainter.State.Empty -> {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_cat),
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = CatMapColors.TextMuted.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}