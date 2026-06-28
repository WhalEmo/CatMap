package com.beem.catmap.ui.upload

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.beem.catmap.databinding.DialogPremiumUploadBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class PremiumUploadDialog(context: Context) {

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

    fun updateProgress(progress: Int) {
        binding.dialogProgressBar.setProgress(progress, true)
        binding.tvDialogStatus.text = "Bihter buluta uçuruluyor... %$progress"

        // 🎯 KRİTİK UX DOKUNUŞU: Yükleme tamamlandığında yapılacak o asil kapanış resitali
        if (progress >= 100) {
            performSuccessAndDismissAnimation()
        }
    }

    private fun performSuccessAndDismissAnimation() {
        // 1. Dönen progress ikonunu gizle, başarı ikonunu patlat
        binding.dialogIconProgress.visibility = View.GONE
        binding.ivDialogSuccessCheck.visibility = View.VISIBLE
        binding.tvDialogStatus.text = "Haritaya başarıyla işlendi! 🐾"
        binding.dialogProgressBar.visibility = View.INVISIBLE

        // 2. 800ms boyunca kullanıcının beynine 'Başarılı' sinyalini pürüzsüzce kazı (Perceived Success)
        binding.root.postDelayed({
            // 3. Şak diye kapatma! Yumuşak bir Fade-Out animasyonuyla eriterek yok et
            dialog?.window?.decorView?.animate()
                ?.alpha(0f)
                ?.setDuration(300)
                ?.withEndAction {
                    dialog?.dismiss()
                }
                ?.start()
        }, 800) // Tam 800ms auditor reçetesi
    }
}