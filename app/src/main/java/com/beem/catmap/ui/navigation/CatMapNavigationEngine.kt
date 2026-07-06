package com.beem.catmap.ui.navigation

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import com.beem.catmap.databinding.ActivityMapsBinding
import java.lang.ref.WeakReference

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
            SmartNavigationEngine.navigateTo(targetScreen)
            true
        }
    }

    private fun setupCaptureHub() {
        binding.btnCaptureLayout.setOnClickListener {
            triggerHaptic(HapticFeedbackConstants.CONFIRM)
            SmartNavigationEngine.navigateTo(Screen.CAMERA)
        }
    }

    fun updateUISilently(@IdRes selectedMenuId: Int) {
        val screen = Screen.fromMenuId(selectedMenuId)

        if (screen.tabIndex >= 0) {
            binding.bottomNavigation.visibility = View.VISIBLE
        } else {
            binding.bottomNavigation.visibility = View.GONE
        }

        val currentSelectedId = binding.bottomNavigation.selectedItemId

        if (currentSelectedId != selectedMenuId) {
            isUpdatingSilently = true
            binding.bottomNavigation.selectedItemId = selectedMenuId
            isUpdatingSilently = false
        }
    }

    fun selectMapTabSilently() {
        updateUISilently(com.beem.catmap.R.id.haritagit)
    }

    private fun triggerHaptic(feedbackConstant: Int) {
        activityRef.get()?.window?.decorView?.performHapticFeedback(feedbackConstant)
    }
}