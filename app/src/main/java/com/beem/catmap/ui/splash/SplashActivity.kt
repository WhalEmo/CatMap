package com.beem.catmap.ui.splash

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.MainActivity
import com.beem.catmap.Maps.MapsActivity

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val kayit = getSharedPreferences("KullaniciKayit", Context.MODE_PRIVATE)
        val girisYapildi = kayit.getBoolean("GirisYapildi", false)

        MainActivity.kullanici = Kullanici()

        // Veriler hazırlandığı an (milisaniyeler içinde) hedef aktiviteye yönlendiriyoruz usta
        val intent = if (girisYapildi) {
            Log.d("SPLASH", "maps")
            MainActivity.kullanici.GetYerelKullanici(this)
            Intent(this, MapsActivity::class.java)
        } else {
            Log.d("SPLASH", "main")
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish() // Splash'i arkada yok et!
    }
}