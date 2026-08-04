package com.beem.catmap.ui.message.dialogs

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.fragment.app.DialogFragment
import com.beem.catmap.databinding.MesajDuzenleBinding
import androidx.core.graphics.drawable.toDrawable

class EditMessageDialogFragment : DialogFragment() {

    private var _binding: MesajDuzenleBinding? = null
    private val binding get() = _binding!!

    private var initialText: String = ""
    private var onSaveClickListener: ((String) -> Unit)? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            initialText = it.getString(ARG_INITIAL_TEXT, "")
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MesajDuzenleBinding.inflate(inflater, container, false)

        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            requestFeature(Window.FEATURE_NO_TITLE)
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 1. Mevcut Mesaj Metnini Doldur ve İmleci Sona Al
        binding.editYeniMesaj.apply {
            setText(initialText)
            setSelection(initialText.length)
            requestFocus()
        }

        // 2. Buton Dinleyicileri
        binding.btnIptal.setOnClickListener {
            dismiss()
        }

        binding.btnMesajiKaydet.setOnClickListener {
            val updatedText = binding.editYeniMesaj.text.toString().trim()
            if (updatedText.isNotEmpty() && updatedText != initialText) {
                onSaveClickListener?.invoke(updatedText)
            }
            dismiss()
        }
    }

    override fun onStart() {
        super.onStart()
        // Dialog Genişlik Ayarı (Ekranın %90'ını kaplasın)
        dialog?.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.90).toInt(),
            ViewGroup.LayoutParams.WRAP_CONTENT
        )
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    fun setOnSaveClickListener(listener: (String) -> Unit) {
        this.onSaveClickListener = listener
    }

    companion object {
        private const val ARG_INITIAL_TEXT = "arg_initial_text"

        fun newInstance(initialText: String): EditMessageDialogFragment {
            return EditMessageDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_INITIAL_TEXT, initialText)
                }
            }
        }
    }
}