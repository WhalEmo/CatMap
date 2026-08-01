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