package com.beem.catmap.ui.components

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.beem.catmap.R
import com.beem.catmap.databinding.DialogCatmapBinding
import androidx.core.graphics.drawable.toDrawable

class CatMapDialog : DialogFragment() {

    private var _binding: DialogCatmapBinding? = null
    private val binding get() = _binding!!

    private var titleText: String? = null
    private var messageText: String? = null
    private var positiveBtnText: String? = "Anladım"
    private var positiveAction: (() -> Unit)? = null

    // 🔴 1. Başlığı Kapatmak İçin Kurşungeçirmez Yöntem
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = DialogCatmapBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🟢 Değerleri view'lara basıyoruz (Hata payı bırakmamak için null check'e gerek yok, boş string basar)
        binding.txtDialogTitle.text = titleText
        binding.txtDialogMessage.text = messageText
        binding.btnDialogPositive.text = positiveBtnText

        binding.btnDialogPositive.setOnClickListener {
            positiveAction?.invoke()
            dismiss()
        }
    }

    // 🔴 2. KÜRDA GÖRÜNTÜSÜNÜ YOK EDEN VE ANİMASYONLARI BAĞLAYAN KRİTİK YER!
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            // Arka plan pencere çizimini tamamen saydam yapıyoruz (Köşelerdeki lekeyi siler)
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

            // Animasyon stili
            attributes?.windowAnimations = R.style.CatMapDialogAnimation

            // FrameLayout içi padding sayesinde kenarlarda tam 24dp boşluk kalır
            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
        }
    }

    // 🚀 Builder Metodları (Fluvent Pattern)
    fun setTitle(title: String): CatMapDialog {
        this.titleText = title
        return this
    }

    fun setMessage(message: String): CatMapDialog {
        this.messageText = message
        return this
    }

    fun setPositiveButton(text: String, action: (() -> Unit)? = null): CatMapDialog {
        this.positiveBtnText = text
        this.positiveAction = action
        return this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun build(): CatMapDialog = CatMapDialog()
    }
}