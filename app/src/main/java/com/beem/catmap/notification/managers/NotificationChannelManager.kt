package com.beem.catmap.notification.managers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.media.AudioAttributes
import android.os.Build
import com.beem.catmap.R
import androidx.core.net.toUri
import java.util.concurrent.atomic.AtomicBoolean

object NotificationChannelManager {

    const val CHANNEL_VERSION = "v4"
    const val CHANNEL_ID_CHAT = "catmap_chat_messages_$CHANNEL_VERSION"
    const val CHANNEL_ID_REMINDERS = "catmap_reminders_broadcast_$CHANNEL_VERSION"
    const val CHANNEL_ID_SOCIAL = "catmap_social_interactions_$CHANNEL_VERSION"

    private val isInitialized = AtomicBoolean(false)

    fun ensureChannelsExist(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        if (isInitialized.get()) return

        synchronized(this) {
            if (isInitialized.get()) return

            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            cleanLegacyChannels(notificationManager)

            val soundUri =
                "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/${R.raw.catmap_meow}".toUri()

            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()

            val chatChannel = NotificationChannel(
                CHANNEL_ID_CHAT,
                "Mesaj Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Kullanıcılar arası sohbet mesajları"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 200, 100, 200)
                enableLights(true)
                lightColor = 0xFFFF8C00.toInt()
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val socialChannel = NotificationChannel(
                CHANNEL_ID_SOCIAL,
                "Takip ve Etkileşim Bildirimleri",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Yeni takipçiler ve profil etkileşimleri"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 150, 100, 150)
                enableLights(true)
                lightColor = 0xFFFF8C00.toInt()
                setSound(soundUri, audioAttributes)
                lockscreenVisibility = android.app.Notification.VISIBILITY_PUBLIC
            }

            val reminderChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Hatırlatmalar ve Etkinlikler",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Kedi keşif ve besleme hatırlatmaları"
                setSound(soundUri, audioAttributes)
            }

            notificationManager.createNotificationChannels(listOf(chatChannel, socialChannel, reminderChannel))

            isInitialized.set(true)
        }
    }

    private fun cleanLegacyChannels(manager: NotificationManager) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val legacyIds = listOf(
            "catmap_chat_messages",
            "catmap_chat_messages_v1",
            "catmap_chat_messages_v2",
            "catmap_reminders_broadcast",
            "catmap_reminders_broadcast_v1",
            "catmap_reminders_broadcast_v2"
        )
        for (channelId in legacyIds) {
            try {
                manager.deleteNotificationChannel(channelId)
            } catch (_: Exception) {}
        }
    }

    fun createNotificationChannels(context: Context) {
        ensureChannelsExist(context)
    }
}