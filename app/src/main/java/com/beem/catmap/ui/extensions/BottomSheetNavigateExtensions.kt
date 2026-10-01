package com.beem.catmap.ui.extensions

import android.os.SystemClock
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

private var lastBottomSheetClickTime = 0L

fun Fragment.navigateBottomSheetSafely(
    tag: String,
    debounceMs: Long = 500L,
    fragmentManager: FragmentManager = this.childFragmentManager,
    navigate: () -> Unit
) {
    val currentTime = SystemClock.elapsedRealtime()

    // 1. ZAMAN KİLİDİ (Debounce)
    if (currentTime - lastBottomSheetClickTime < debounceMs) {
        return
    }
    lastBottomSheetClickTime = currentTime

    // 2. STATE KONTROLÜ (İşlem kaydedilmişse veya Fragment uygun değilse çöküşü önle)
    if (isStateSaved || fragmentManager.isStateSaved) {
        return
    }

    // 3. FRAGMENT KONTROLÜ (Aynı tag'e sahip açık/görünür modal var mı?)
    val existing = fragmentManager.findFragmentByTag(tag)
    if (existing != null && (existing.isAdded || existing.isVisible)) {
        return
    }

    // Her şey temizse yönlendirmeyi çalıştır
    navigate()
}