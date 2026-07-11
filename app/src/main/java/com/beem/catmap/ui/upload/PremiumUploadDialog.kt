package com.beem.catmap.ui.upload

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.beem.catmap.databinding.DialogPremiumUploadBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PremiumUploadDialog(
    context: Context,
    private val onAnimationEnd: () -> Unit
) {

    private var dialog: AlertDialog? = null
    private val binding: DialogPremiumUploadBinding =
        DialogPremiumUploadBinding.inflate(LayoutInflater.from(context))

    init {
        dialog = MaterialAlertDialogBuilder(context)
            .setView(binding.root)
            .setCancelable(false) // Kullanıcı dışarı basıp işlemi kesemesin
            .create()

        // 🎯 AUDITOR TALİMATI: Arka planı %60 karartma (Scrim Dim) ve pencere şeffaflık ayarı
        dialog?.window?.let { window ->
            window.setBackgroundDrawableResource(android.R.color.transparent)
            window.setDimAmount(0.60f) // %60 alpha dim oranı çakıldı!
        }
    }

    fun show() {
        if (dialog?.isShowing == false) {
            dialog?.show()
        }
    }
    fun dismiss(){
        if (dialog?.isShowing == true) {
            dialog?.dismiss()
        }
    }


    fun renderState(stage: UploadStage, progress: Int, errorMsg: String? = null) {
        when (stage) {
            UploadStage.FETCHING_LOCATION -> {
                binding.dialogIconProgress.visibility = View.VISIBLE
                binding.ivDialogSuccessCheck.visibility = View.GONE
                binding.dialogProgressBar.visibility = View.GONE
                binding.tvDialogStatus.text = "GPS uydularından konumunuz senkronize ediliyor... 📡"
            }
            UploadStage.UPLOADING_ASSETS -> {
                binding.dialogProgressBar.visibility = View.VISIBLE
                binding.dialogProgressBar.setProgress(progress, true)
                binding.tvDialogStatus.text = "Fotoğraflar buluta uçuruluyor... %$progress 🐾"
            }
            UploadStage.SUCCESS -> {
                performSuccessAndDismissAnimation()
            }
            UploadStage.ERROR -> {
                performErrorAnimation(errorMsg ?: "Bir hata oluştu!")
            }
        }
    }


    private fun performSuccessAndDismissAnimation() {
        binding.dialogIconProgress.visibility = View.GONE
        binding.ivDialogSuccessCheck.visibility = View.VISIBLE
        binding.tvDialogStatus.text = "Haritaya başarıyla işlendi! 🐾"
        binding.dialogProgressBar.visibility = View.INVISIBLE

        binding.root.postDelayed({
            dialog?.window?.decorView?.animate()
                ?.alpha(0f)
                ?.setDuration(300)
                ?.withEndAction {
                    this.dismiss()
                    onAnimationEnd.invoke()
                }
                ?.start()
        }, 800)
    }

    private fun performErrorAnimation(message: String) {
        binding.dialogIconProgress.visibility = View.GONE
        binding.ivDialogSuccessCheck.visibility = View.VISIBLE
        binding.ivDialogSuccessCheck.setImageResource(android.R.drawable.ic_delete)
        binding.tvDialogStatus.text = message
        binding.dialogProgressBar.visibility = View.GONE

        binding.root.postDelayed({
            dismissWithAnimation()
        }, 1800)
    }

    private fun dismissWithAnimation() {
        dialog?.window?.decorView?.animate()
            ?.alpha(0f)
            ?.setDuration(300)
            ?.withEndAction {
                dialog?.dismiss()
                onAnimationEnd.invoke()
            }
            ?.start()
    }
}