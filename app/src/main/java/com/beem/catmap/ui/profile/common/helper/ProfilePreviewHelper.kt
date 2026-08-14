package com.beem.catmap.ui.profile.common

import android.app.Dialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.view.HapticFeedbackConstants
import android.view.View
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import com.beem.catmap.R
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition

object ProfilePreviewHelper {

    fun attachLongPressPreview(
        context: Context,
        targetView: View,
        photoUrl: String?
    ) {
        targetView.setOnLongClickListener { view ->
            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)

            val previewDialog = Dialog(context, R.style.Theme_ProfilePreviewDialog).apply {
                setContentView(R.layout.dialog_profile_preview)
                setCancelable(true)
            }

            val imgExpanded = previewDialog.findViewById<ImageView>(R.id.imgExpandedProfile)
            val container = previewDialog.findViewById<View>(R.id.previewContainer)

            Glide.with(context)
                .load(photoUrl)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(object : CustomTarget<Drawable>() {
                    override fun onResourceReady(
                        resource: Drawable,
                        transition: Transition<in Drawable>?
                    ) {
                        imgExpanded?.setImageDrawable(resource)
                    }

                    override fun onLoadCleared(placeholder: Drawable?) {
                        imgExpanded?.setImageDrawable(placeholder)
                    }
                })

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

            true
        }
    }
}