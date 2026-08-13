package com.beem.catmap.ui.profile.common

import androidx.fragment.app.FragmentManager
import com.beem.catmap.ui.components.CatMapDialog

object ProfileDialogHelper {

    fun showUnfollowerDialog(
        fragmentManager: FragmentManager,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        if (fragmentManager.findFragmentByTag("TakipcidenCikarDialog") != null) return

        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcıyı takipçilerinizden çıkarmak istediğinize emin misiniz?"
        } else {
            "Bu kullanıcıyı takipçilerinizden çıkarmak istediğinize emin misiniz?"
        }

        CatMapDialog.Companion.build()
            .setTitle("Takipçiden Çıkar")
            .setMessage(message)
            .setPositiveButton("Evet, Çıkar") {
                onConfirm()
            }
            .setNegativeButton("İptal")
            .show(fragmentManager, "TakipcidenCikarDialog")
    }

    fun showBlockDialog(
        fragmentManager: FragmentManager,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        if (fragmentManager.findFragmentByTag("EngelleDialog") != null) return

        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcıyı engellemek istediğinize emin misiniz? Bu kullanıcı artık profilinizi göremeyecek."
        } else {
            "Bu kullanıcıyı engellemek istediğinize emin misiniz? Bu kullanıcı artık profilinizi göremeyecek."
        }

        CatMapDialog.Companion.build()
            .setTitle("Kullanıcıyı Engelle")
            .setMessage(message)
            .setPositiveButton("Engelle") {
                onConfirm()
            }
            .setNegativeButton("Vazgeç")
            .show(fragmentManager, "EngelleDialog")
    }

    fun showUnblockDialog(
        fragmentManager: FragmentManager,
        kullaniciAdi: String?,
        onConfirm: () -> Unit
    ) {
        if (fragmentManager.findFragmentByTag("EngelKaldirDialog") != null) return

        val message = if (!kullaniciAdi.isNullOrBlank()) {
            "@$kullaniciAdi isimli kullanıcının engelini kaldırmak istediğinize emin misiniz?"
        } else {
            "Bu kullanıcının engelini kaldırmak istediğinize emin misiniz?"
        }

        CatMapDialog.Companion.build()
            .setTitle("Engeli Kaldır")
            .setMessage(message)
            .setPositiveButton("Engeli Kaldır") {
                onConfirm()
            }
            .setNegativeButton("İptal")
            .show(fragmentManager, "EngelKaldirDialog")
    }
}