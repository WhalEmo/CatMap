package com.beem.catmap

import android.app.Application

class CatMapApp : Application() {

    companion object {
        lateinit var instance: CatMapApp
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}