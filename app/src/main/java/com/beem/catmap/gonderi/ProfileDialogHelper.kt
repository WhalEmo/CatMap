package com.beem.catmap.gonderi

import androidx.fragment.app.FragmentManager
import com.beem.catmap.ui.components.CatMapDialog

object ProfileDialogHelper {

    fun showTakipcidenCikarDialog(
        fragmentManager: FragmentManager,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcıyı takipçilerinizden çıkarmak istediğinize emin misiniz?"
        } else {
            "Bu kullanıcıyı takipçilerinizden çıkarmak istediğinize emin misiniz?"
        }

        CatMapDialog.build()
            .setTitle("Takipçiden Çıkar")
            .setMessage(message)
            .setPositiveButton("Evet, Çıkar") {
                onConfirm()
            }
            .setNegativeButton("İptal")
            .show(fragmentManager, "TakipcidenCikarDialog")
    }

    fun showEngelleDialog(
        fragmentManager: FragmentManager,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcıyı engellemek istediğinize emin misiniz? Bu kullanıcı artık profilinizi göremeyecek."
        } else {
            "Bu kullanıcıyı engellemek istediğinize emin misiniz? Bu kullanıcı artık profilinizi göremeyecek."
        }

        CatMapDialog.build()
            .setTitle("Kullanıcıyı Engelle")
            .setMessage(message)
            .setPositiveButton("Engelle") {
                onConfirm()
            }
            .setNegativeButton("Vazgeç")
            .show(fragmentManager, "EngelleDialog")
    }

    fun showEngelKaldirDialog(
        fragmentManager: FragmentManager,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcının engelini kaldırmak istediğinize emin misiniz?"
        } else {
            "Bu kullanıcının engelini kaldırmak istediğinize emin misiniz?"
        }

        CatMapDialog.build()
            .setTitle("Engeli Kaldır")
            .setMessage(message)
            .setPositiveButton("Engeli Kaldır") {
                onConfirm()
            }
            .setNegativeButton("İptal")
            .show(fragmentManager, "EngelKaldirDialog")
    }
}