package com.beem.catmap.notification

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager
import com.beem.catmap.notification.managers.FcmTokenManager
import com.beem.catmap.notification.managers.NotificationChannelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RequestNotificationPermission(
    onPermissionGranted: () -> Unit = {}
) {
    val context = LocalContext.current

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            NotificationChannelManager.ensureChannelsExist(context)

            CoroutineScope(Dispatchers.IO).launch {
                FcmTokenManager().syncCurrentToken()
            }
            onPermissionGranted()
        }
    }

    LaunchedEffect(Unit) {
        NotificationChannelManager.ensureChannelsExist(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                // Zaten izinli, token'ı doğrula
                CoroutineScope(Dispatchers.IO).launch {
                    FcmTokenManager().syncCurrentToken()
                }
            }
        } else {
            // Android 12 ve altında izin otomatik verilir
            CoroutineScope(Dispatchers.IO).launch {
                FcmTokenManager().syncCurrentToken()
            }
        }
    }
}