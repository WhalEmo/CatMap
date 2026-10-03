package com.beem.catmap.notification

import android.app.PendingIntent
import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.Person
import androidx.core.content.ContextCompat
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.core.net.toUri
import coil.Coil
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.transform.CircleCropTransformation
import com.beem.catmap.R
import com.beem.catmap.main.MainActivity
import com.beem.catmap.notification.managers.FcmTokenManager
import com.beem.catmap.notification.managers.NotificationChannelManager
import com.beem.catmap.notification.models.NotificationType
import com.beem.catmap.notification.models.ParsedNotification
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.core.graphics.createBitmap
import com.beem.catmap.notification.models.ParsedNotification.*

class CatMapMessagingService : FirebaseMessagingService() {

    companion object {
        private const val TAG = "CatMapMessagingService"
    }

    private val soundUri: Uri by lazy {
        "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${packageName}/${R.raw.catmap_meow}".toUri()
    }

    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            FcmTokenManager().onNewTokenReceived(token)
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        NotificationChannelManager.ensureChannelsExist(this)

        val data = message.data
        if (data.isEmpty()) return

        when (val parsed = parsePayload(data)) {
            is Broadcast -> {
                serviceScope.launch {
                    showBroadcastNotification(parsed)
                }
            }
            is Chat -> {
                if (ActiveChatTracker.isChatActive(parsed.chatId)) {
                    Log.d(TAG, "Sohbet ekranı açık, bildirim bastırıldı: ${parsed.chatId}")
                    return
                }
                serviceScope.launch {
                    showChatNotification(parsed)
                }
            }
            is Follow -> {
                serviceScope.launch { showFollowNotification(parsed) }
            }
        }
    }

    private fun parsePayload(data: Map<String, String>): ParsedNotification {
        val type = NotificationType.fromRaw(data["type"])

        return when (type) {
            NotificationType.FOLLOW -> {
                Follow(
                    senderId = data["sender_id"] ?: "",
                    title = data["title"] ?: "Yeni Takipçi! 🐾",
                    body = data["body"] ?: "Biri seni takip etmeye başladı.",
                    photoUrl = data["sender_photo"]
                )
            }

            NotificationType.CHAT_MESSAGE -> {
                Chat(
                    senderId = data["sender_id"] ?: "",
                    chatId = data["chat_id"] ?: "",
                    title = data["title"] ?: "CatMap",
                    body = data["body"] ?: "Yeni bir mesajınız var",
                    photoUrl = data["sender_photo"]
                )
            }
            NotificationType.ADMIN_BROADCAST -> {
                Broadcast(
                    title = data["title"] ?: "CatMap Duyuru",
                    body = data["body"] ?: "",
                    targetRoute = data["target_route"],
                    imageUrl = data["image_url"]
                )
            }

            NotificationType.REMINDER -> {
                Broadcast(
                    title = data["title"] ?: "CatMap Duyuru",
                    body = data["body"] ?: "",
                    targetRoute = data["target_route"],
                    imageUrl = data["image_url"]
                )
            }
        }
    }


    private suspend fun showChatNotification(chat: Chat) {
        if (chat.senderId.isEmpty()) return

        val avatarBitmap: Bitmap = loadAvatarBitmap(chat.photoUrl, isCircle = true)
            ?: ContextCompat.getDrawable(this, R.drawable.ic_cat)?.toBitmap(96, 96)
            ?: createBitmap(96, 96)

        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "catmap://chat/${chat.senderId}".toUri()
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            chat.senderId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val senderIcon = IconCompat.createWithBitmap(avatarBitmap)
        val senderPerson = Person.Builder()
            .setKey(chat.senderId)
            .setName(chat.title)
            .setIcon(senderIcon)
            .setImportant(true)
            .build()

        val userPerson = Person.Builder()
            .setKey("current_user_me")
            .setName("Ben")
            .build()

        val shortcut = ShortcutInfoCompat.Builder(this, chat.senderId)
            .setShortLabel(chat.title)
            .setLongLabel(chat.title)
            .setIcon(senderIcon)
            .setIntent(intent)
            .setPerson(senderPerson)
            .setLongLived(true)
            .build()

        try {
            ShortcutManagerCompat.pushDynamicShortcut(this, shortcut)
        } catch (e: Exception) {
            Log.e(TAG, "Shortcut eklenemedi: ${e.message}")
        }

        // 6. WhatsApp Tarzı MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle(userPerson)
            .setConversationTitle(null)
            .addMessage(
                chat.body,
                System.currentTimeMillis(),
                senderPerson
            )

        // 7. 🎯 WhatsApp'taki "Sohbete Git" veya "Okundu" Butonu (Action)
        val openChatAction = NotificationCompat.Action.Builder(
            R.drawable.ic_cat,
            "Sohbete Git",
            pendingIntent
        ).build()

        // 8. Bildirimi İnşa Et
        val builder = NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_ID_CHAT)
            .setSmallIcon(R.drawable.ic_cat)          // Avatarın sağ altındaki minik CatMap rozeti
            .setStyle(messagingStyle)
            .setShortcutId(chat.senderId)                  // 🎯 MIUI'a 'Bu bir WhatsApp tarzı sohbettir' diyen anahtar
            .addAction(openChatAction)                // Alttaki profesyonel aksiyon butonu
            .setCategory(NotificationCompat.CATEGORY_MESSAGE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setAutoCancel(true)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)

        dispatchNotification(chat.senderId.hashCode(), builder)
    }

    private suspend fun showBroadcastNotification(broadcast: ParsedNotification.Broadcast) {
        val notificationId = System.currentTimeMillis().toInt()

        // Derin link rotası varsa MapsActivity'ye yönlendir, yoksa varsayılan başlatıcıyı aç
        val intent = if (!broadcast.targetRoute.isNullOrEmpty()) {
            Intent(this, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                data = "catmap://route/${broadcast.targetRoute}".toUri()
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
        } else {
            packageManager.getLaunchIntentForPackage(packageName)?.apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            } ?: Intent(this, MainActivity::class.java)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.drawable.ic_cat)
            .setColor(ContextCompat.getColor(this, R.color.catmap_accent))
            .setContentTitle(broadcast.title)
            .setContentText(broadcast.body)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)

        // Eğer admin duyurusunda görsel varsa BigPictureStyle, yoksa BigTextStyle uygula
        val bannerBitmap = loadAvatarBitmap(broadcast.imageUrl, isCircle = false)
        if (bannerBitmap != null) {
            builder.setStyle(
                NotificationCompat.BigPictureStyle()
                    .bigPicture(bannerBitmap)
                    .setSummaryText(broadcast.body)
            )
        } else {
            builder.setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(broadcast.body)
            )
        }

        dispatchNotification(notificationId, builder)
    }


    private suspend fun showFollowNotification(follow: Follow) {
        if (follow.senderId.isEmpty()) return

        val notificationId = follow.senderId.hashCode()

        // Tıklanınca MapsActivity açılır ve catmap://profile/{uid} rotasına yönlenir
        val intent = Intent(this, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            data = "catmap://profile/${follow.senderId}".toUri()
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Takip edenin yuvarlak avatarını yükle, yoksa kedi ikonu
        val avatarBitmap: Bitmap? = loadAvatarBitmap(follow.photoUrl, isCircle = true)
            ?: ContextCompat.getDrawable(this, R.drawable.ic_cat)?.toBitmap(96, 96)

        val builder = NotificationCompat.Builder(this, NotificationChannelManager.CHANNEL_ID_SOCIAL)
            .setSmallIcon(R.drawable.ic_cat)
            .setColor(ContextCompat.getColor(this, R.color.catmap_accent))
            .setContentTitle(follow.title)
            .setContentText(follow.body)
            .setAutoCancel(true)
            .setShowWhen(true)
            .setWhen(System.currentTimeMillis())
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_SOCIAL)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOnlyAlertOnce(false)
            .setContentIntent(pendingIntent)

        if (avatarBitmap != null) {
            builder.setLargeIcon(avatarBitmap)
        }

        dispatchNotification(notificationId, builder)
    }

    private suspend fun loadAvatarBitmap(url: String?, isCircle: Boolean): Bitmap? {
        if (url.isNullOrEmpty()) return null
        return try {
            val requestBuilder = ImageRequest.Builder(this)
                .data(url)
                .allowHardware(false)

            if (isCircle) {
                requestBuilder.transformations(CircleCropTransformation())
            }

            val result = Coil.imageLoader(this).execute(requestBuilder.build())
            (result as? SuccessResult)?.drawable?.toBitmap()
        } catch (e: Exception) {
            Log.e(TAG, "Avatar yükleme hatası: ${e.message}")
            null
        }
    }

    private fun dispatchNotification(id: Int, builder: NotificationCompat.Builder) {
        val manager = NotificationManagerCompat.from(this)
        try {
            manager.notify(id, builder.build())
        } catch (e: SecurityException) {
            Log.e(TAG, "Bildirim gönderme yetkisi yok: ${e.message}")
        }
    }
}