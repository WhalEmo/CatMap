package com.beem.catmap.ui.components

import android.util.Log
import androidx.annotation.DrawableRes
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.beem.catmap.R

@Composable
fun CatMapImage(
    data: Any?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop,
    @DrawableRes placeholderRes: Int = R.drawable.kullanici,
    @DrawableRes errorRes: Int = R.drawable.ic_error_outline,
    crossfadeDurationMs: Int = 250,
    refreshKey: Any? = null
) {
    val context = LocalContext.current

    // Görselin hata verip vermediğini yerel olarak tutuyoruz
    var isFailed by remember { mutableStateOf(false) }

    // refreshKey değiştiğinde, SADECE daha önce hata aldıysa retry için tetikleyici üret
    var retryAttempt by remember { mutableIntStateOf(0) }

    LaunchedEffect(refreshKey) {
        // Eğer görsel daha önce patladıysa ve kullanıcı refresh attıysa tekrar dene
        if (isFailed && refreshKey != null) {
            isFailed = false
            retryAttempt++
        }
    }

    val imageRequest = remember(data, retryAttempt) {
        ImageRequest.Builder(context)
            .data(data)
            .placeholder(placeholderRes)
            .error(errorRes)
            .crossfade(true)
            .crossfade(crossfadeDurationMs)
            // Sadece retry durumundaysa (daha önce patlamışsa) hatalı cache'i del,
            // Normalde ve başarılı olanlarda RAM önbelleğini tam kapasite kullan
            .memoryCachePolicy(if (retryAttempt > 0) CachePolicy.WRITE_ONLY else CachePolicy.ENABLED)
            .diskCachePolicy(CachePolicy.ENABLED)
            .networkCachePolicy(CachePolicy.ENABLED)
            .listener(
                onStart = {
                    Log.d("CatMapImage", "Yükleme başladı -> $data")
                },
                onSuccess = { _, result ->
                    isFailed = false
                    Log.d("CatMapImage", "Yükleme başarılı -> $data | Kaynak: ${result.dataSource}")
                },
                onError = { _, result ->
                    isFailed = true // Hata durumunu işaretle
                    Log.e("CatMapImage", "Yükleme hatası -> $data | Sebep: ${result.throwable.message}")
                }
            )
            .build()
    }

    AsyncImage(
        model = imageRequest,
        contentDescription = contentDescription,
        placeholder = painterResource(placeholderRes),
        error = painterResource(errorRes),
        contentScale = contentScale,
        modifier = modifier
    )
}