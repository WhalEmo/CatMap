package com.beem.catmap.ui.markersclick.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun CatPhotoItem(
    url: String,
    onOpenFullScreen: () -> Unit
) {
    val context = LocalContext.current
    var retryTrigger by remember { mutableIntStateOf(0) }

    val imageRequest = remember(url, retryTrigger) {
        ImageRequest.Builder(context)
            .data(url)
            .crossfade(true)
            .crossfade(300)
            .memoryCachePolicy(if (retryTrigger > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .build()
    }

    // ESKİ CardView'ın (cardUseCompatPadding + 8dp elevation + 24dp corner) BİREBİR KARŞILIĞI:
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp), // Eski compat padding mesafesi
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(24.dp), // Eski app:cardCornerRadius="24dp"
            shadowElevation = 8.dp,            // Eski app:cardElevation="8dp"
            color = CatMapColors.SurfaceWhite
        ) {
            SubcomposeAsyncImage(
                model = imageRequest,
                contentDescription = "Kedi Fotoğrafı",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp)),
                contentScale = ContentScale.Crop
            ) {
                when (val state = painter.state) {

                    // 1. YÜKLENİYOR
                    is AsyncImagePainter.State.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CatMapColors.SurfaceTranslucent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cat),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = CatMapColors.TextMuted.copy(alpha = 0.25f)
                            )
                            CircularProgressIndicator(
                                color = CatMapColors.Accent,
                                strokeWidth = 2.5.dp,
                                modifier = Modifier.size(64.dp)
                            )
                        }
                    }

                    // 2. BAŞARILI (Tıklama tam ekran açar)
                    is AsyncImagePainter.State.Success -> {
                        SubcomposeAsyncImageContent(
                            modifier = Modifier
                                .fillMaxSize()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = onOpenFullScreen
                                )
                        )
                    }

                    // 3. HATA DURUMU
                    is AsyncImagePainter.State.Error -> {
                        val errorMsg = state.result.throwable.message ?: ""
                        val isAuthError = errorMsg.contains("401") || errorMsg.contains("403") ||
                                errorMsg.contains("Unauthorized") || errorMsg.contains("Forbidden")

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CatMapColors.SurfaceTranslucent)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { if (!isAuthError) retryTrigger++ }
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cat),
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = CatMapColors.Error.copy(alpha = 0.5f)
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            if (isAuthError) {
                                Text(
                                    text = "Görsele erişim yetkisi bulunamadı.",
                                    color = CatMapColors.TextSecondary,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            } else {
                                Text(
                                    text = "Fotoğraf yüklenemedi",
                                    color = CatMapColors.TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Tekrar denemek için dokunun",
                                    color = CatMapColors.TextMuted,
                                    fontSize = 12.sp
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { retryTrigger++ },
                                    shape = RoundedCornerShape(20.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(
                                        contentColor = CatMapColors.Accent
                                    ),
                                    border = BorderStroke(1.dp, CatMapColors.Accent),
                                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Tekrar Dene",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }

                    // 4. BOŞ DURUM
                    is AsyncImagePainter.State.Empty -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(CatMapColors.SurfaceTranslucent),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_cat),
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = CatMapColors.TextMuted.copy(alpha = 0.4f)
                            )
                        }
                    }
                }
            }
        }
    }
}