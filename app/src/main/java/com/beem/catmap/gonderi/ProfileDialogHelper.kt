package com.beem.catmap.gonderi
//package com.beem.catmap.ui.helper

import android.content.Context
import androidx.appcompat.app.AlertDialog

object ProfileDialogHelper {

    fun showTakipcidenCikarDialog(
        context: Context,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcıyı takipçilerinizden çıkarmak istediğinize emin misiniz?"
        } else {
            "Bu kullanıcıyı takipçilerinizden çıkarmak istediğinize emin misiniz?"
        }

        AlertDialog.Builder(context)
            .setTitle("Takipçiden Çıkar")
            .setMessage(message)
            .setPositiveButton("Evet") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    fun showEngelleDialog(
        context: Context,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcıyı engellemek istediğinize emin misiniz? Bu kullanıcı artık profilinizi göremeyecek."
        } else {
            "Bu kullanıcıyı engellemek istediğinize emin misiniz? Bu kullanıcı artık profilinizi göremeyecek."
        }

        AlertDialog.Builder(context)
            .setTitle("Kullanıcıyı Engelle")
            .setMessage(message)
            .setPositiveButton("Engelle") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    fun showEngelKaldirDialog(
        context: Context,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcının engelini kaldırmak istediğinize emin misiniz?"
        } else {
            "Bu kullanıcının engelini kaldırmak istediğinize emin misiniz?"
        }

        AlertDialog.Builder(context)
            .setTitle("Engeli Kaldır")
            .setMessage(message)
            .setPositiveButton("Engeli Kaldır") { dialog, _ ->
                onConfirm()
                dialog.dismiss()
            }
            .setNegativeButton("İptal", null)
            .show()
    }
}