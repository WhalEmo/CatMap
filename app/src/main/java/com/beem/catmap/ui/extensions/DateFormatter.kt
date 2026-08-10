package com.beem.catmap.ui.extensions

import com.google.firebase.Timestamp
import java.util.Date;
import java.text.SimpleDateFormat
import java.util.Locale
fun getFormattedDate(date: Date): String {
    val currentDate = date ?: return "şimdi"
    val fark = System.currentTimeMillis() - currentDate.time

    return when {
        fark < 60_000 -> "şimdi"
        fark < 3_600_000 -> "${fark / 60_000} dk önce"
        else -> {
            val sdf = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
            sdf.format(currentDate)
        }
    }
}
fun getFormattedTimestamp(timestamp: Timestamp?): String {
    if (timestamp == null) return "şimdi"
    return getFormattedDate(timestamp.toDate())
}