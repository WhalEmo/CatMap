package com.beem.catmap.ui.auth

import android.os.Bundle
import android.text.InputFilter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.databinding.FragmentProfileSetupBinding
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import kotlinx.coroutines.launch

class ProfileSetupFragment : Fragment() {

    private var _binding: FragmentProfileSetupBinding? = null
    private val binding get() = _binding!!

    private val viewModel: ProfileSetupViewModel by viewModels()
    private var currentUserModel: UserModel? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileSetupBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        currentUserModel = arguments?.getSerializable(KEY_USER) as? UserModel

        setupUsernameFilter()
        setupClickListeners()
        setupInitialData()
        observeViewModel()
    }

    private fun setupUsernameFilter() {
        val usernameFilter = InputFilter { source, _, _, _, _, _ ->
            val regex = Regex("^[a-zA-Z0-9_.]+$")
            if (source.isEmpty() || regex.matches(source)) null else ""
        }
        binding.etUsername.filters = arrayOf(usernameFilter)
    }

    private fun setupInitialData() {
        currentUserModel?.let { user ->
            // Ad Soyad Google'dan gelen isimle doluyor
            if (user.name.isNotBlank()) {
                binding.etFullName.setText(user.name)
            }

            // Email'den otomatik kullanıcı adı türetme (@ öncesini alma)
            val defaultUsername = user.email.substringBefore("@")
                .lowercase()
                .replace(Regex("[^a-zA-Z0-9_.]"), "") // Sadece izin verilen karakterleri tut

            binding.etUsername.setText(defaultUsername)
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.loadingState.collect { message ->
                        if (message != null) {
                            showLoadingPill(message)
                        } else {
                            hideLoadingPill()
                        }
                    }
                }

                launch {
                    viewModel.uiEvent.collect { event ->
                        when (event) {
                            is ProfileSetupViewModel.UiEvent.Success -> {
                                SmartNavigationEngine.navigateTo(Screen.MAP)
                            }
                            is ProfileSetupViewModel.UiEvent.Error -> {
                                binding.tilUsername.error = event.message
                            }
                        }
                    }
                }
            }
        }
    }

    private fun setupClickListeners() {
        // 🚀 "Macera Başlasın" Butonu (Kullanıcının kendi seçtiği bilgilerle)
        binding.btnCompleteProfile.setOnClickListener {
            val username = binding.etUsername.text.toString().trim().lowercase()
            val fullName = binding.etFullName.text.toString().trim()
            val bio = binding.etBio.text.toString().trim()

            if (username.isEmpty()) {
                binding.tilUsername.error = "Kullanıcı adı boş bırakılamaz!"
                return@setOnClickListener
            }

            viewModel.completeProfile(
                userModel = currentUserModel,
                preferredUsername = username,
                fullName = fullName,
                bio = bio
            )
        }

        // 💨 "Şimdilik Atla" Butonu (Varsayılan e-posta kullanıcı adı ile arka planda kayıt)
        binding.btnSkipSetup.setOnClickListener {
            val defaultUsername = binding.etUsername.text.toString().trim().lowercase()
            val defaultName = currentUserModel?.name ?: ""

            viewModel.completeProfile(
                userModel = currentUserModel,
                preferredUsername = defaultUsername,
                fullName = defaultName,
                bio = "",
                isAutoGenerateIfTaken = true // 🟢 Eğer çakışırsa arkasına sayı ekle
            )
        }
    }

    /**
     * 🟢 Premium Loading Kapsülünü Gösterir
     */
    private fun showLoadingPill(message: String) {
        binding.tvLoadingMessage.text = message
        binding.loadingPill.apply {
            if (!isVisible) {
                alpha = 0f
                isVisible = true
                animate().alpha(1f).setDuration(200).start()
            }
        }
        // Giriş alanlarını ve butonları işlem sırasında kilitliyoruz
        setFormEnabled(false)
    }

    /**
     * 🔴 Kapsülü Pürüzsüzce Gizler
     */
    private fun hideLoadingPill() {
        binding.loadingPill.animate().alpha(0f).setDuration(200).withEndAction {
            binding.loadingPill.isVisible = false
        }.start()
        setFormEnabled(true)
    }

    private fun setFormEnabled(enabled: Boolean) {
        binding.btnCompleteProfile.isEnabled = enabled
        binding.btnSkipSetup.isEnabled = enabled
        binding.tilUsername.isEnabled = enabled
        binding.tilFullName.isEnabled = enabled
        binding.tilBio.isEnabled = enabled
    }

    companion object {
        private const val KEY_USER = "key_google_user"

        fun newInstance(userModel: UserModel): ProfileSetupFragment {
            return ProfileSetupFragment().apply {
                arguments = newBundle(userModel)
            }
        }

        fun newBundle(userModel: UserModel): Bundle {
            return bundleOf(KEY_USER to userModel)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}