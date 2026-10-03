package com.beem.catmap.ui.profile.common

import android.app.Dialog
import android.content.Context
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import coil.load
import coil.request.CachePolicy
import com.beem.catmap.R

object ProfilePreviewHelper {

    fun attachLongPressPreview(
        context: Context,
        targetView: View,
        photoUrl: String?
    ) {
        targetView.setOnLongClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            showPreview(context, photoUrl)
            true
        }
    }

    fun showPreview(context: Context, photoUrl: String?) {
        val previewDialog = Dialog(context, R.style.Theme_ProfilePreviewDialog).apply {
            setContentView(R.layout.dialog_profile_preview)
            setCancelable(true)
        }

        val imgExpanded = previewDialog.findViewById<ImageView>(R.id.imgExpandedProfile)
        val container = previewDialog.findViewById<View>(R.id.previewContainer)

        imgExpanded?.load(photoUrl) {
            crossfade(true)
            crossfade(200)
            placeholder(R.drawable.kullanici)
            error(R.drawable.kullanici)
            memoryCachePolicy(CachePolicy.ENABLED)
            diskCachePolicy(CachePolicy.ENABLED)
        }

        container?.setOnClickListener {
            previewDialog.dismiss()
        }

        imgExpanded?.scaleX = 0.6f
        imgExpanded?.scaleY = 0.6f

        previewDialog.show()

        imgExpanded?.animate()
            ?.scaleX(1.0f)
            ?.scaleY(1.0f)
            ?.setDuration(200)
            ?.setInterpolator(OvershootInterpolator(1.1f))
            ?.withLayer()
            ?.start()
    }
}