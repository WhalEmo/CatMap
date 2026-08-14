package com.beem.catmap.ui.components

import android.app.Dialog
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.WindowManager
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import com.beem.catmap.R
import com.beem.catmap.databinding.DialogCatmapInputBinding
import com.beem.catmap.ui.extensions.applyInputLimits

class CatMapInputDialog : DialogFragment() {

    private var _binding: DialogCatmapInputBinding? = null
    private val binding get() = _binding!!

    private var titleText: String? = null
    private var initialText: String? = null
    private var hintText: String? = "Bir şeyler yazın..."

    private var positiveBtnText: String? = "Güncelle"
    private var positiveAction: ((String) -> Unit)? = null

    private var negativeBtnText: String? = null
    private var negativeAction: (() -> Unit)? = null

    private var maxLinesCount: Int = 10

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
        _binding = DialogCatmapInputBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.txtDialogTitle.text = titleText
        binding.etDialogInput.setText(initialText.orEmpty())
        binding.etDialogInput.hint = hintText
        binding.btnDialogPositive.text = positiveBtnText

        binding.btnDialogPositive.isEnabled = false
        binding.btnDialogPositive.alpha = 0.3f

        binding.etDialogInput.applyInputLimits(maxLength = 280, maxLines = maxLinesCount)

        binding.etDialogInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val currentText = s?.toString()?.trim().orEmpty()
                val originalText = initialText.orEmpty().trim()

                val isChangedAndValid = currentText.isNotEmpty() &&
                        currentText != originalText &&
                        currentText.length <= 280

                binding.btnDialogPositive.isEnabled = isChangedAndValid

                binding.btnDialogPositive.animate()
                    .alpha(if (isChangedAndValid) 1.0f else 0.3f)
                    .setDuration(150)
                    .start()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.etDialogInput.requestFocus()
        binding.etDialogInput.setSelection(binding.etDialogInput.text?.length ?: 0)

        binding.btnDialogPositive.setOnClickListener {
            val inputText = binding.etDialogInput.text.toString().trim()
            if (inputText.isNotEmpty() && inputText != initialText.orEmpty().trim()) {
                positiveAction?.invoke(inputText)
                dismiss()
            }
        }

        // 🔴 Negatif (Vazgeç) Butonu Yönetimi (Null/Boş ise GONE!)
        if (!negativeBtnText.isNullOrEmpty()) {
            binding.btnDialogNegative.text = negativeBtnText
            binding.btnDialogNegative.isVisible = true
            binding.btnDialogNegative.setOnClickListener {
                negativeAction?.invoke()
                dismiss()
            }
        } else {
            binding.btnDialogNegative.isVisible = false
        }
    }

    // 🔴 2. PENCERE VE ANİMASYON STANDARTLARI (CatMapDialog Birebir İkizi)
    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
            attributes?.windowAnimations = R.style.CatMapDialogAnimation

            setLayout(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.WRAP_CONTENT
            )
            setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)
        }
    }

    // 🚀 Fluent Builder Metodları
    fun setTitle(title: String): CatMapInputDialog {
        this.titleText = title
        return this
    }

    fun setInitialText(text: String?): CatMapInputDialog {
        this.initialText = text
        return this
    }

    fun setHint(hint: String): CatMapInputDialog {
        this.hintText = hint
        return this
    }

    fun setPositiveButton(text: String, action: ((String) -> Unit)? = null): CatMapInputDialog {
        this.positiveBtnText = text
        this.positiveAction = action
        return this
    }

    fun setNegativeButton(text: String, action: (() -> Unit)? = null): CatMapInputDialog {
        this.negativeBtnText = text
        this.negativeAction = action
        return this
    }

    fun setMaxLinesCount(newCount: Int): CatMapInputDialog {
        this.maxLinesCount = newCount
        return this
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun build(): CatMapInputDialog = CatMapInputDialog()
    }
}