package com.beem.catmap.ui.auth

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.graphics.drawable.toDrawable
import androidx.core.widget.doOnTextChanged
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

        setupErrorClearingOnTextChange()
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
            clearAllErrors() // Önceki hataları temizle
            val username = binding.edtLoginUser.text.toString().trim()
            val password = binding.edtLoginPass.text.toString().trim()

            var hasError = false

            if (username.isEmpty()) {
                binding.tilLoginUser.error = "Kullanıcı adı boş bırakılamaz"
                hasError = true
            }

            if (password.isEmpty()) {
                binding.tilLoginPass.error = "Şifre boş bırakılamaz"
                hasError = true
            }

            if (!hasError) {
                viewModel.login(username, password)
            }
        }

        // 📝 Kaydol Butonu
        binding.btnRegister.setOnClickListener {
            clearAllErrors()
            val name = binding.edtRegName.text.toString().trim()
            val surname = binding.edtRegSurname.text.toString().trim()
            val email = binding.edtRegEmail.text.toString().trim()
            val username = binding.edtRegUser.text.toString().trim()
            val password = binding.edtRegPass.text.toString().trim()

            var hasError = false

            if (name.isEmpty()) {
                binding.tilRegName.error = "Ad zorunludur"
                hasError = true
            }
            if (surname.isEmpty()) {
                binding.tilRegSurname.error = "Soyad zorunludur"
                hasError = true
            }
            if (email.isEmpty()) {
                binding.tilRegEmail.error = "E-posta zorunludur"
                hasError = true
            }
            if (username.isEmpty()) {
                binding.tilRegUser.error = "Kullanıcı adı zorunludur"
                hasError = true
            }
            if (password.isEmpty()) {
                binding.tilRegPass.error = "Şifre zorunludur"
                hasError = true
            } else if (password.length < 5) {
                binding.tilRegPass.error = "Şifre en az 5 karakter olmalıdır"
                hasError = true
            }

            if (!hasError) {
                val user = Kullanici(
                    ad = name,
                    soyad = surname,
                    email = email,
                    kullaniciAdi = username,
                    sifre = password
                )
                viewModel.register(user)
            }
        }

        binding.btnResetPassword.setOnClickListener {
            clearAllErrors()
            val email = binding.edtForgotEmail.text.toString().trim()

            if (email.isEmpty()) {
                binding.tilForgotEmail.error = "E-posta adresi boş bırakılamaz"
            } else {
                viewModel.resetPassword(email)
            }
        }
    }

    // 🚀 StateFlow & SharedFlow Dinleyicisi
    private fun observeStateFlow() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {

                // 1. UI State Dinleyicisi
                launch {
                    viewModel.currentMode.collect { mode ->
                        clearAllErrors()
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
                            else -> {}
                        }
                    }
                }

            }
        }
    }


    private fun clearAllErrors() {
        val layouts = listOf(
            binding.tilLoginUser, binding.tilLoginPass,
            binding.tilRegName, binding.tilRegSurname,
            binding.tilRegEmail, binding.tilRegUser, binding.tilRegPass,
            binding.tilForgotEmail
        )

        layouts.forEach { layout ->
            layout.error = null
            layout.isErrorEnabled = false
        }
    }

    private fun setupErrorClearingOnTextChange() {
        val inputMap = mapOf(
            binding.edtLoginUser to binding.tilLoginUser,
            binding.edtLoginPass to binding.tilLoginPass,
            binding.edtRegName to binding.tilRegName,
            binding.edtRegSurname to binding.tilRegSurname,
            binding.edtRegEmail to binding.tilRegEmail,
            binding.edtRegUser to binding.tilRegUser,
            binding.edtRegPass to binding.tilRegPass,
            binding.edtForgotEmail to binding.tilForgotEmail
        )

        inputMap.forEach { (editText, textInputLayout) ->
            editText.doOnTextChanged { _, _, _, _ ->
                textInputLayout.error = null
                textInputLayout.isErrorEnabled = false
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