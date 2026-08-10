package com.beem.catmap.utils

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

fun Long.toFormattedLastSeen(): String {
    if (this <= 0L) return "Bilinmiyor"

    val lastSeenCalendar = Calendar.getInstance().apply { timeInMillis = this@toFormattedLastSeen }

    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(this))

    val lastSeenDateOnly = Calendar.getInstance().apply {
        timeInMillis = this@toFormattedLastSeen
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val nowDateOnly = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffInMillis = nowDateOnly.timeInMillis - lastSeenDateOnly.timeInMillis
    val dayDiff = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        dayDiff == 0L -> "Bugün $formattedTime"
        dayDiff == 1L -> "Dün $formattedTime"
        dayDiff in 2..5 -> "$dayDiff gün önce $formattedTime"
        else -> {
            val fullFormat = SimpleDateFormat("dd MMMM yyyy HH:mm", Locale("tr", "TR"))
            fullFormat.format(Date(this))
        }
    }
}


fun Long.toFormattedMessageTime(): String {
    if (this <= 0L) return ""

    val messageDate = Date(this)
    val messageCalendar = Calendar.getInstance().apply { timeInMillis = this@toFormattedMessageTime }
    val nowCalendar = Calendar.getInstance()

    // 1. Sadece Saat (Örn: 14:30)
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(messageDate)

    // Gün farkı hesabı için saatleri sıfırlıyoruz
    val msgDateOnly = Calendar.getInstance().apply {
        timeInMillis = this@toFormattedMessageTime
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val nowDateOnly = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }

    val diffInMillis = nowDateOnly.timeInMillis - msgDateOnly.timeInMillis
    val dayDiff = TimeUnit.MILLISECONDS.toDays(diffInMillis)

    return when {
        // 🟢 Bugün atılan mesaj: Sadece saat gösterilir ("14:30")
        dayDiff == 0L -> formattedTime

        // 🟡 Dün atılan mesaj: "Dün" gösterilir
        dayDiff == 1L -> "Dün"

        // 🟠 Bu hafta (2-6 gün önce): Günün adı gösterilir ("Pazartesi", "Salı")
        dayDiff in 2..6L -> {
            val dayFormat = SimpleDateFormat("EEEE", Locale("tr", "TR"))
            dayFormat.format(messageDate)
        }

        // 🔵 Aynı yıl içindeyse: "12 May" gösterilir
        messageCalendar.get(Calendar.YEAR) == nowCalendar.get(Calendar.YEAR) -> {
            val monthFormat = SimpleDateFormat("dd MMM", Locale("tr", "TR"))
            monthFormat.format(messageDate)
        }

        // 🟣 Farklı yıldaysa: "12.05.2025" gösterilir
        else -> {
            val fullFormat = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
            fullFormat.format(messageDate)
        }
    }
}