package com.beem.catmap.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.R
import com.beem.catmap.UyariMesaji
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.databinding.BottomSheetAuthBinding
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch

class AuthBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: BottomSheetAuthBinding? = null
    private val binding get() = _binding!!

    private val viewModel: AuthViewModel by viewModels()
    private lateinit var uyariMesaji: UyariMesaji

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = BottomSheetAuthBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        uyariMesaji = UyariMesaji(requireContext(), false)

        val initialModeName = arguments?.getString(ARG_INITIAL_MODE)
        if (initialModeName != null) {
            val initialMode = AuthMode.valueOf(initialModeName)
            viewModel.setMode(initialMode)
        }

        setupListeners()
        observeStateFlow()
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setBackgroundDrawable((Color.TRANSPARENT).toDrawable())
        }
    }

    private fun setupListeners() {
        binding.btnGoToRegister.setOnClickListener { viewModel.setMode(AuthMode.REGISTER) }
        binding.btnGoToForgot.setOnClickListener { viewModel.setMode(AuthMode.FORGOT_PASSWORD) }
        binding.btnGoToLoginFromReg.setOnClickListener { viewModel.setMode(AuthMode.LOGIN) }
        binding.btnGoToLoginFromForgot.setOnClickListener { viewModel.setMode(AuthMode.LOGIN) }

        binding.btnLogin.setOnClickListener {
            viewModel.login(
                binding.edtLoginUser.text.toString(),
                binding.edtLoginPass.text.toString()
            )
        }

        binding.btnRegister.setOnClickListener {
            val user = Kullanici(
                ad = binding.edtRegName.text.toString(),
                soyad = binding.edtRegSurname.text.toString(),
                email = binding.edtRegEmail.text.toString(),
                kullaniciAdi = binding.edtRegUser.text.toString(),
                sifre = binding.edtRegPass.text.toString()
            )
            viewModel.register(user)
        }

        binding.btnResetPassword.setOnClickListener {
            viewModel.resetPassword(binding.edtForgotEmail.text.toString())
        }
    }

    // 🚀 StateFlow & SharedFlow Dinleyicisi
    private fun observeStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. UI State Dinleyicisi
                launch {
                    viewModel.currentMode.collect { mode ->
                        val targetChild = when(mode) {
                            AuthMode.LOGIN -> 0
                            AuthMode.REGISTER -> 1
                            AuthMode.FORGOT_PASSWORD -> 2
                        }
                        if (binding.viewFlipperAuth.displayedChild != targetChild) {
                            binding.viewFlipperAuth.displayedChild = targetChild
                        }
                    }
                }

                launch {
                    viewModel.uiState.collect { state ->
                        when (state) {
                            is AuthUiState.Idle -> {
                                setButtonsEnabled(true)
                            }
                            is AuthUiState.Loading -> {
                                setButtonsEnabled(false)
                                uyariMesaji.YuklemeDurum(state.message)
                            }
                            is AuthUiState.Success -> {
                                setButtonsEnabled(true)
                                uyariMesaji.BasariliDurum(state.message, 1000)
                                CurrentUserManager.getInstance(requireContext()).setCurrentUser(state.user)
                                dismiss()
                            }
                            is AuthUiState.Error -> {
                                setButtonsEnabled(true)
                                uyariMesaji.BasarisizDurum(state.errorMessage, 1500)
                            }
                        }
                    }
                }

                // 2. Event Dinleyicisi
                launch {
                    viewModel.event.collect { event ->
                        when (event) {
                            is AuthEvent.ShowToast -> {
                                Toast.makeText(requireContext(), event.message, Toast.LENGTH_SHORT).show()
                            }
                            is AuthEvent.NavigateToMap -> {
                                SmartNavigationEngine.navigateTo(Screen.MAP)
                            }
                        }
                    }
                }

            }
        }
    }

    private fun setButtonsEnabled(enabled: Boolean) {
        binding.btnLogin.isEnabled = enabled
        binding.btnRegister.isEnabled = enabled
        binding.btnResetPassword.isEnabled = enabled
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val TAG = "AuthBottomSheetFragment"
        private const val ARG_INITIAL_MODE = "arg_initial_mode"

        fun newInstance(initialMode: AuthMode = AuthMode.LOGIN): AuthBottomSheetFragment {
            val fragment = AuthBottomSheetFragment()
            val args = Bundle().apply {
                putString(ARG_INITIAL_MODE, initialMode.name)
            }
            fragment.arguments = args
            return fragment
        }
    }
}