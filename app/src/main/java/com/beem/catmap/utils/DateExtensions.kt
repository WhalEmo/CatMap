package com.beem.catmap.utils


import com.google.firebase.Timestamp
import java.util.Date

fun Date?.toFirebaseTimestamp(): Timestamp {
    return this?.let { Timestamp(it) } ?: Timestamp.now()
}