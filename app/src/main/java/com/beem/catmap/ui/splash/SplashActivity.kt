package com.beem.catmap.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen // Resmi kütüphanemiz
import com.beem.catmap.MainActivity
import com.beem.catmap.Maps.MapsActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        val kayit = getSharedPreferences("KullaniciKayit", Context.MODE_PRIVATE)
        val girisYapildi = kayit.getBoolean("GirisYapildi", false)

        // Veriler hazırlandığı an (milisaniyeler içinde) hedef aktiviteye yönlendiriyoruz usta
        val intent = if (girisYapildi) {
            Intent(this, MapsActivity::class.java)
        } else {
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish() // Splash'i arkada yok et!
    }
}