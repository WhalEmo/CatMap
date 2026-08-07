package com.beem.catmap

import android.app.Application
import com.beem.catmap.managers.OnlinePresenceManager

class CatMapApp : Application() {

    companion object {
        lateinit var instance: CatMapApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this

        OnlinePresenceManager.initialize()
    }
}