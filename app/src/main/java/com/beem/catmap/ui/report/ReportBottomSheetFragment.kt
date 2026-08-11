package com.beem.catmap.ui.report

import android.content.res.ColorStateList
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RadioButton
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.R
import com.beem.catmap.databinding.BottomSheetReportBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class ReportBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetReportBinding? = null
    private val binding get() = _binding!!

    private var targetId: String = ""
    private lateinit var reportType: ReportType
    private var selectedReason: String? = null

    private val viewModel: ReportViewModel by lazy { ReportViewModel() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetId = it.getString(ARG_TARGET_ID).orEmpty()
            reportType = ReportType.valueOf(it.getString(ARG_REPORT_TYPE) ?: ReportType.POST.name)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetReportBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🟢 1. Başlık ve Şikayet Sebepleri
        binding.txtReportTitle.text = reportType.title

        reportType.reasons.forEachIndexed { index, reason ->
            val radioButton = RadioButton(requireContext()).apply {
                id = index
                text = reason
                textSize = 14f
                setTextColor(ContextCompat.getColor(context, R.color.catmap_text_primary))
                setPadding(16, 20, 16, 20)
                buttonTintList = ContextCompat.getColorStateList(context, R.color.catmap_primary)
            }
            binding.rgReportReasons.addView(radioButton)
        }

        // 🟢 2. Seçim Değişiklik Takibi
        binding.rgReportReasons.setOnCheckedChangeListener { _, checkedId ->
            if (checkedId != -1) {
                selectedReason = reportType.reasons[checkedId]

                if (!binding.btnSubmitReport.isEnabled) {
                    binding.btnSubmitReport.isEnabled = true
                    binding.btnSubmitReport.animate().alpha(1.0f).setDuration(150).start()
                }
            }
        }

        // 🟢 3. Gönder Butonu Aksiyonu
        binding.btnSubmitReport.setOnClickListener {
            selectedReason?.let { reason ->
                viewModel.submitReport(targetId, reportType, reason)
            }
        }

        observeViewModel()
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.reportState.collect { state ->
                    when (state) {
                        is ReportUiState.Idle -> {
                            binding.cardStatusBanner.isVisible = false
                        }
                        is ReportUiState.Loading -> {
                            showBannerLoading("Bildiriminiz Firebase'e iletiliyor...")
                        }
                        is ReportUiState.Success -> {
                            showBannerSuccess("Bildiriminiz başarıyla iletildi.")
                            viewLifecycleOwner.lifecycleScope.launch {
                                delay(1000.milliseconds)
                                dismiss()
                                viewModel.resetState()
                            }
                        }
                        is ReportUiState.Error -> {
                            showBannerError(state.message)
                        }
                    }
                }
            }
        }
    }

    // 🐾 STATUS BANNER YARDIMCI METOTLARI
    private fun showBannerLoading(message: String) {
        binding.cardStatusBanner.isVisible = true
        binding.bannerProgress.isVisible = true
        binding.imgBannerIcon.isVisible = false
        binding.tvBannerMessage.text = message
        binding.btnSubmitReport.isEnabled = false
        binding.btnSubmitReport.alpha = 0.3f
        binding.rgReportReasons.isEnabled = false
    }

    private fun showBannerSuccess(message: String) {
        val successColor = ContextCompat.getColor(requireContext(), R.color.catmap_success)

        binding.cardStatusBanner.isVisible = true
        binding.bannerProgress.isVisible = false

        binding.cardStatusBanner.strokeColor = successColor

        binding.imgBannerIcon.apply {
            isVisible = true
            setImageResource(R.drawable.ic_check_circle)
            imageTintList = ColorStateList.valueOf(successColor)
        }
        binding.tvBannerMessage.text = message
    }

    private fun showBannerError(message: String) {
        val errorColor = ContextCompat.getColor(requireContext(), R.color.catmap_error)

        binding.cardStatusBanner.isVisible = true
        binding.bannerProgress.isVisible = false

        binding.cardStatusBanner.strokeColor = errorColor

        binding.imgBannerIcon.apply {
            isVisible = true
            setImageResource(R.drawable.ic_error_outline)
            imageTintList = ColorStateList.valueOf(errorColor)
        }

        binding.tvBannerMessage.text = message
        binding.tvBannerMessage.setTextColor(errorColor)

        binding.btnSubmitReport.isEnabled = true
        binding.btnSubmitReport.alpha = 1.0f
        binding.rgReportReasons.isEnabled = true
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val ARG_TARGET_ID = "arg_target_id"
        private const val ARG_REPORT_TYPE = "arg_report_type"

        fun newInstance(targetId: String, reportType: ReportType): ReportBottomSheetFragment {
            return ReportBottomSheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_TARGET_ID, targetId)
                    putString(ARG_REPORT_TYPE, reportType.name)
                }
            }
        }
    }
}