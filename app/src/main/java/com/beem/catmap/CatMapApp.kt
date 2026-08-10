package com.beem.catmap

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import com.beem.catmap.managers.OnlinePresenceManager

class CatMapApp : Application(), Application.ActivityLifecycleCallbacks {

    companion object {
        private const val TAG = "CatMapAppLifecycle"

        lateinit var instance: CatMapApp
            private set
    }

    override fun onCreate() {
        registerActivityLifecycleCallbacks(this)

        super.onCreate()
        Log.d(TAG, "🚀 [Application] onCreate: CatMap süreci (Process) başlatıldı.")
        instance = this

        OnlinePresenceManager.initialize()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        Log.w(TAG, "⚠️ [Application] onLowMemory: Sistem belleği kritik seviyede düşük!")
    }

    // 🧹 İşletim sistemi arka plandaki önbelleği temizlememizi istediğinde çalışır
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        Log.w(TAG, "🧹 [Application] onTrimMemory: Bellek temizleme uyarısı alındı (Level: $level)")
    }

    /* ========================================================================
       GLOBAL ACTIVITY LIFECYCLE CALLBACKS (Tüm Ekranların Otomatik İzlenmesi)
       ======================================================================== */

    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        Log.d(TAG, "📱 [Activity] Created: ${activity.localClassName}")
    }

    override fun onActivityStarted(activity: Activity) {
        Log.d(TAG, "👁️ [Activity] Started: ${activity.localClassName}")
    }

    override fun onActivityResumed(activity: Activity) {
        Log.d(TAG, "⚡ [Activity] Resumed (Ekranda Aktif): ${activity.localClassName}")
    }

    override fun onActivityPaused(activity: Activity) {
        Log.d(TAG, "⏸️ [Activity] Paused: ${activity.localClassName}")
    }

    override fun onActivityStopped(activity: Activity) {
        Log.d(TAG, "🙈 [Activity] Stopped (Görünmez): ${activity.localClassName}")
    }

    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        Log.d(TAG, "💾 [Activity] SaveInstanceState: ${activity.localClassName}")
    }

    override fun onActivityDestroyed(activity: Activity) {
        Log.d(TAG, "💀 [Activity] Destroyed: ${activity.localClassName}")
    }
}