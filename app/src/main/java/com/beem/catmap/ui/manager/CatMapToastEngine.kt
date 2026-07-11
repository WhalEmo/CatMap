package com.beem.catmap.ui.manager

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.view.Gravity // 🎯 MUTLAK MERKEZLEME İÇİN GRAVITY ŞART
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout // 🎯 FRAME_LAYOUT PARAMS İÇİN
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.beem.catmap.R
import java.lang.ref.WeakReference

object CatMapToastEngine {

    private var currentToastRef: WeakReference<View>? = null
    private var toastHideRunnable: Runnable? = null
    private val toastHandler = Handler(Looper.getMainLooper())

    @JvmStatic
    @SuppressLint("InflateParams")
    fun show(context: Context, message: String, iconRes: Int, strokeColor: Int, durationMs: Int) {
        if (context !is AppCompatActivity) return

        // 1. ESKİ TOAST VARSA TEMİZLE
        clearCurrentToast()

        // 2. TASARIMI ŞİŞİR
        val rootView = context.findViewById<ViewGroup>(android.R.id.content) ?: return
        val toastView = LayoutInflater.from(context).inflate(R.layout.layout_premium_toast, rootView, false)

        toastView.findViewById<TextView>(R.id.tvToastMessage).text = message
        toastView.findViewById<ImageView>(R.id.ivToastIcon).apply {
            setImageResource(iconRes)
            setColorFilter(strokeColor)
        }

        toastView.findViewById<View>(R.id.toastCardContainer).background?.let {
            if (it is GradientDrawable) {
                val strokeWidth = (1.5f * context.resources.displayMetrics.density).toInt()
                it.setStroke(strokeWidth, strokeColor)
            }
        }

        val params = FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL

            topMargin = (92 * context.resources.displayMetrics.density).toInt()
        }
        toastView.layoutParams = params

        toastView.alpha = 0f
        toastView.translationY = -400f
        rootView.addView(toastView)
        currentToastRef = WeakReference(toastView)

        // 🎬 GİRİŞ: Fast Out, Slow In (350ms - Decelerate)
        toastView.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(350)
            .setInterpolator(DecelerateInterpolator())
            .start()

        // 5. DOKUNUNCA KAPATMA INTERACTION'I
        toastView.setOnClickListener { dismissWithAnimation(rootView, toastView) }

        // 6. OTOMATİK ZAMANLAYICI
        val hideRunnable = Runnable { dismissWithAnimation(rootView, toastView) }
        toastHideRunnable = hideRunnable
        toastHandler.postDelayed(hideRunnable, durationMs.toLong())
    }

    private fun dismissWithAnimation(rootView: ViewGroup?, toastView: View?) {
        if (toastView == null || rootView == null) return
        toastHideRunnable?.let { toastHandler.removeCallbacks(it) }

        // 🎬 ÇIKIŞ: İvmelenerek dik yukarı kaçış (250ms - Accelerate)
        toastView.animate()
            .alpha(0f)
            .translationY(-400f)
            .setDuration(250)
            .setInterpolator(AccelerateInterpolator())
            .withEndAction {
                rootView.removeView(toastView)
                if (currentToastRef?.get() == toastView) {
                    currentToastRef = null
                }
            }
            .start()
    }

    @JvmStatic
    fun clearCurrentToast() {
        currentToastRef?.get()?.let { toastView ->
            toastHideRunnable?.let { toastHandler.removeCallbacks(it) }
            (toastView.parent as? ViewGroup)?.removeView(toastView)
        }
        currentToastRef = null
    }
}