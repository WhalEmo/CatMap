package com.beem.catmap.ui.navigation

import android.os.Bundle
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.beem.catmap.databinding.ActivityMapsBinding
import com.beem.catmap.ui.extensions.fadeIn
import com.beem.catmap.ui.extensions.fadeOut
import java.lang.ref.WeakReference
import androidx.core.view.isVisible
import com.beem.catmap.MainActivity
import com.beem.catmap.Profil.ProfilSayfasiFragment
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
            if (isUpdatingSilently) return@setOnItemSelectedListener true

            val targetScreen = Screen.fromMenuId(item.itemId)
            SmartNavigationEngine.navigateTo(
                targetScreen = targetScreen,
                args = if (targetScreen == Screen.PROFILE)
                    ProfilSayfasiFragment.newArgs(UserSession.userId) else null,
                key = null
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

        if(screen.isNode && screen != Screen.AUTH) menuShow() else menuHide()

        if (screen == Screen.MAP) mapItemViewFadeIn() else mapItemViewFadeOut()

        screen.menuId?.let { selectedMenuId ->
            val currentSelectedId = binding.bottomNavigation.selectedItemId
            if (currentSelectedId != selectedMenuId) {
                isUpdatingSilently = true
                binding.bottomNavigation.selectedItemId = selectedMenuId
                isUpdatingSilently = false
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

    private fun mapItemViewFadeIn(){
    }

    private fun mapItemViewFadeOut(){
    }

    fun selectMapTabSilently() {
        updateUISilently(Screen.MAP)
    }

    private fun triggerHaptic(feedbackConstant: Int) {
        activityRef.get()?.window?.decorView?.performHapticFeedback(feedbackConstant)
    }
}