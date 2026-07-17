package com.beem.catmap.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.beem.catmap.MainActivity
import com.beem.catmap.Maps.MapsActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kayit = getSharedPreferences("KullaniciKayit", Context.MODE_PRIVATE)
        val girisYapildi = kayit.getBoolean("GirisYapildi", false)

        val intent = if (girisYapildi) {
            Log.d("SPLASH", "maps")
            Intent(this, MapsActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        overridePendingTransition(0, 0)
        finish()
    }
}