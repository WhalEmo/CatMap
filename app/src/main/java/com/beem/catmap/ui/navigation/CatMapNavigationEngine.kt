package com.beem.catmap.ui.navigation

import android.view.HapticFeedbackConstants
import android.view.View
import android.widget.Toast
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.beem.catmap.MainActivity
import com.beem.catmap.Maps.MapsActivity
import com.beem.catmap.Profil.ProfilSayfasiFragment
import com.beem.catmap.R
import com.beem.catmap.databinding.ActivityMapsBinding
import com.beem.catmap.sohbet.SohbetFragment
import com.beem.catmap.ui.upload.YuklemeArayuzuFragment
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import java.lang.ref.WeakReference

class CatMapNavigationEngine(
    activity: AppCompatActivity,
    private val binding: ActivityMapsBinding,
    private val onFragmentChanged: (Fragment) -> Unit
) {
    private val activityRef = WeakReference(activity)

    init {
        setupNavigation()
        setupCaptureHub()
    }

    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            triggerHaptic(HapticFeedbackConstants.CLOCK_TICK)

            val targetTag = when (item.itemId) {
                R.id.haritagit -> "MAP_FRAGMENT_TAG"
                R.id.upload -> "YUKLE"
                R.id.sohbet -> "CHAT"
                R.id.profilim -> "PROFILE"
                else -> return@setOnItemSelectedListener false
            }

            navigateTo(targetTag)
            true
        }
    }

    // 🚀 2. ÖZEL KAZANDIRILAN INSTAGRAM TARZI AKILLI VİZÖR TETİKLEYİCİSİ
    private fun setupCaptureHub() {
        val activity = activityRef.get() ?: return

        binding.btnCaptureLayout.setOnClickListener {
            // Tok onay titreşimi (CONFIRMATION muadili)
            triggerHaptic(HapticFeedbackConstants.CONFIRM)
            Toast.makeText(activity, "CatMap Akıllı Vizör Hub Devreye Girdi...", Toast.LENGTH_SHORT).show()
        }
    }

    // 🧭 KOTLIN TABANLI DÜZLEŞTİRİLMİŞ NAVİGASYON KÖPRÜSÜ
    private fun navigateTo(tag: String) {
        val activity = activityRef.get() ?: return

        val fm = activity.supportFragmentManager
        fm.popBackStackImmediate(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE)

        // Bilgi butonunu haritaya bağlama lojiği
        val btnShowFact = activity.findViewById<View>(R.id.btnShowFact)
        btnShowFact?.visibility = if (tag == "MAP_FRAGMENT_TAG") View.VISIBLE else View.GONE

        SmartNavigationEngine.navigateTo(tag, object : FragmentProvider {
            override fun createFragment(targetTag: String): Fragment? {
                return when (targetTag) {
                    "MAP_FRAGMENT_TAG" -> {
                        SupportMapFragment().apply {
                            if (activity is OnMapReadyCallback) {
                                getMapAsync(activity)
                            }
                        }
                    }
                    "PROFILE" -> ProfilSayfasiFragment.newInstance(MainActivity.kullanici.id)
                    "CHAT" -> SohbetFragment {
                        activity.supportFragmentManager
                            .beginTransaction()
                            .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left)
                            .replace(R.id.fragment_container, com.beem.catmap.mesaj.MesajFragment(activity as MapsActivity))
                            .addToBackStack(null)
                            .commit()
                    }
                    "YUKLE" -> YuklemeArayuzuFragment()
                    else -> null
                }
            }
        }) { targetFragment ->
            onFragmentChanged.invoke(targetFragment)
        }
    }

    // 🎨 ANDROID STANDARTLARINA UYGUN AKILLI SEÇİCİ (Ucube döngüler silindi dayıcım!)
    fun updateUI(@IdRes selectedMenuId: Int) {
        // Android'in yerleşik seçicisini uyandırarak renkleri ve durumları otomatik yönetmesini sağlıyoruz
        if (binding.bottomNavigation.selectedItemId != selectedMenuId) {
            binding.bottomNavigation.selectedItemId = selectedMenuId
        }
    }

    // 🎯 SESSİZCE HARİTAYA DÖNDÜRME SİSTEMİ (OnBackPressed İçin)
    fun selectMapTabSilently() {
        navigateTo("MAP_FRAGMENT_TAG")
        updateUI(R.id.haritagit)
    }

    private fun triggerHaptic(feedbackConstant: Int) {
        activityRef.get()?.window?.decorView?.performHapticFeedback(feedbackConstant)
    }
}