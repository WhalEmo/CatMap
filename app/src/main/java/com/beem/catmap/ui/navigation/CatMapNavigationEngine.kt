package com.beem.catmap.ui.navigation

import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.appcompat.app.AppCompatActivity
import com.beem.catmap.ui.profile.common.ProfileFragment
import com.beem.catmap.databinding.ActivityMapsBinding
import com.beem.catmap.ui.extensions.fadeIn
import com.beem.catmap.ui.extensions.fadeOut
import java.lang.ref.WeakReference
import com.beem.catmap.data.local.UserSession

class CatMapNavigationEngine(
    activity: AppCompatActivity,
    private val binding: ActivityMapsBinding
) {
    private val activityRef = WeakReference(activity)
    private var isUpdatingSilently = false

    init {
        setupNavigation()
        setupCaptureHub()
    }


    private fun setupNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            Log.d("NAV_BRIDGE", "👇 BOTTOM_NAV TIKLANDI/TETİKLENDİ -> Item ID: ${item.itemId}, isUpdatingSilently: $isUpdatingSilently")

            if (isUpdatingSilently) {
                Log.d("NAV_BRIDGE", "🛑 SILENT UPDATE AKTİF -> Navigasyon İPTAL edildi.")
                return@setOnItemSelectedListener true
            }

            val targetScreen = Screen.fromMenuId(item.itemId)
            Log.d("NAV_BRIDGE", "🚀 NAVIGATE TO TETİKLENİYOR -> Hedef Ekran: $targetScreen")

            SmartNavigationEngine.navigateTo(
                targetScreen = targetScreen
            )
            true
        }
    }

    private fun setupCaptureHub() {
        binding.btnCaptureLayout.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.CONFIRM)
            SmartNavigationEngine.navigateTo(Screen.CAMERA)
        }
    }

    fun updateUISilently(screen: Screen) {
        Log.d("NAV_BRIDGE", "🤫 updateUISilently ÇAĞRILDI -> Ekran: $screen")

        if (screen.isNode && screen != Screen.AUTH && screen != Screen.PROFILE_SETUP) menuShow() else menuHide()


        screen.menuId?.let { selectedMenuId ->
            val currentSelectedId = binding.bottomNavigation.selectedItemId
            Log.d("NAV_BRIDGE", "📊 MENU CHECK -> Mevcut Seçili ID: $currentSelectedId, Hedef Menu ID: $selectedMenuId")

            if (currentSelectedId != selectedMenuId) {
                Log.d("NAV_BRIDGE", "⚡ SECILI ITEM DEĞİŞTİRİLİYOR (Listener Temizleniyor)...")

                // Listener'ı boşa çıkarıp logluyoruz
                binding.bottomNavigation.setOnItemSelectedListener(null)
                binding.bottomNavigation.selectedItemId = selectedMenuId
                setupNavigation()

                Log.d("NAV_BRIDGE", "✅ SECILI ITEM DEĞİŞTİRİLDİ VE LISTENER TEKRAR BAĞLANDI.")
            }
        }
    }

    private fun menuShow(){
        binding.bottomNavigation.fadeIn()
        binding.btnCaptureLayout.fadeIn()
    }

    private fun menuHide(){
        binding.bottomNavigation.fadeOut()
        binding.btnCaptureLayout.fadeOut()
    }



    private fun triggerHaptic(feedbackConstant: Int) {
        activityRef.get()?.window?.decorView?.performHapticFeedback(feedbackConstant)
    }
}