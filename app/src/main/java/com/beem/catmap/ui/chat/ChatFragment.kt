package com.beem.catmap.ui.chat

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.beem.catmap.Maps.MapsActivity
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.databinding.MesajlasmaBinding
import com.beem.catmap.mesaj.Mesaj
import com.beem.catmap.mesaj.MesajFotoGonderYonetici
import com.beem.catmap.models.ChatMessage
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: MesajlasmaBinding? = null
    private val binding get() = _binding!!

    // Alıcı Kullanıcı ID'sini Argument veya Navigation Engine üzerinden alıyoruz
    private val receiverId: String by lazy {
        arguments?.getString(ARG_RECEIVER_ID) ?: throw IllegalArgumentException("Receiver ID gerekli!")
    }

    private val viewModel: ChatViewModel by viewModels {
        ChatViewModelFactory(
            currentUserManager = CurrentUserManager.getInstance(requireContext()),
            receiverId = receiverId
        )
    }

    private lateinit var mesajAdapter: MessageAdapter

    // Galeri Görsel Seçim Launcher'ı
    private val galeriLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            data.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    MesajFotoGonderYonetici.getInstance().UriEkle(clipData.getItemAt(i).uri)
                }
            } ?: data.data?.let { uri ->
                MesajFotoGonderYonetici.getInstance().UriEkle(uri)
            }
            // MesajFotoGonderYonetici.getInstance().GondericiStart(mesajAdapter, requireContext())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MesajlasmaBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Bottom Navigation Bar'ı Gizle
        if (requireActivity() is MapsActivity) {
            requireActivity().findViewById<View>(R.id.bottom_navigation)?.isVisible = false
        }

        setupRecyclerView()
        setupListeners()
        observeUiState()
        setupKeyboardAdjustments()
    }

    private fun setupRecyclerView() {
        mesajAdapter = MessageAdapter(
            currentUserId = UserSession.userId,
            onMessageLongClick = { message, anchor ->
                showOptionMenu(message, anchor)
            },
            onReplyClick = { replyMessage ->
                scrollToMessage(replyMessage.id)
            },
            onPhotoClick = { photoUrls ->
                if (photoUrls.isNotEmpty()) {
                    openPhotoPreview(photoUrls)
                }
            },
        )
        binding.mesajRecyclerView.apply {
            adapter = mesajAdapter
            layoutManager = LinearLayoutManager(context).apply {
                stackFromEnd = true
            }
        }
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            SmartNavigationEngine.navigateBack()
        }

        // Gönder Butonu
        binding.gonderButton.setOnClickListener {
            val text = binding.mesajEditText.text.toString()
            viewModel.sendMessage(text)
            binding.mesajEditText.text?.clear()
        }

        // Yazıyor... Dinleyicisi
        binding.mesajEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onTextChanged(text.toString())
        }

        // Galeri Butonu
        binding.fotoEkleButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            galeriLauncher.launch(intent)
        }

        // Engeli Kaldır Butonu
        binding.engelKaldir.setOnClickListener {
            viewModel.removeBlock()
        }

        // Yanıtlama Kutusunu Kapat
        binding.cevapKapatButton.setOnClickListener {
            viewModel.setReplyMessage(null)
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    renderUi(state)
                }
            }
        }
    }

    private fun renderUi(state: ChatUiState) {

        if (state.receiverName.isNotEmpty()) {
            binding.kisiAdiText.text = state.receiverName
        }

        // Picasso / Glide ile Profil Fotoğrafını Yükle
        if (state.receiverPhotoUrl.isNotEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(state.receiverPhotoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(binding.kisiProfilFoto)
        }

        if (!state.isOtherUserTyping) {
            binding.kisiDurumText.text = state.receiverStatus
        } else {
            binding.kisiDurumText.text = "Yazıyor..."
        }


        // 1. Mesaj Listesini Güncelle
        mesajAdapter.submitList(state.messages) {
            if (state.messages.isNotEmpty()) {
                binding.mesajRecyclerView.scrollToPosition(state.messages.size - 1)
            }
        }

        // 2. Yüklenme Durumu (ProgressBar)
        binding.yukleniyorProgress.isVisible = state.isLoading
        binding.mesajRecyclerView.isVisible = !state.isLoading


        // 4. Engelleme UI Yönetimi
        val isBlocked = state.isBlockedByMe || state.isBlockedByOther
        mesajAdapter.isBlocked = isBlocked

        binding.gonderButton.isVisible = !isBlocked
        binding.mesajEditText.isVisible = !isBlocked
        binding.fotoEkleButton.isVisible = !isBlocked
        binding.engelKaldir.isVisible = isBlocked

        if (state.isBlockedByMe) {
            binding.engelKaldir.text = "ENGELİ KALDIR"
            binding.engelKaldir.isClickable = true
        } else if (state.isBlockedByOther) {
            binding.engelKaldir.text = "ENGELLENDİN"
            binding.engelKaldir.isClickable = false
        }

        // 5. Yanıtlama Kutusu (Reply Layout)
        binding.cevapAlani.isVisible = state.replyMessage != null
        state.replyMessage?.let { msg ->
            binding.cevapMetni.text = when (msg) {
                is ChatMessage.Text -> msg.message
                is ChatMessage.Photo -> "📷 Fotoğraf"
                is ChatMessage.Reply -> msg.message
            }
        }
    }

    private fun openPhotoPreview(photoUrls: List<String>) {
    }

    private fun setupKeyboardAdjustments() {
        // Modern WindowInsets ile klavye yükseldiğinde mesaj alanını yukarı kaydırma
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val imeInsets = insets.getInsets(WindowInsetsCompat.Type.ime())
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())

            val bottomPadding = if (imeInsets.bottom > 0) {
                imeInsets.bottom - systemInsets.bottom
            } else 0

            binding.mesajGonderLayout.setPadding(0, 0, 0, bottomPadding)
            insets
        }
    }

    private fun showOptionMenu(message: ChatMessage, view: View) {
    }

    private fun scrollToMessage(targetMessageId: String) {
        val currentList = mesajAdapter.currentList
        val index = currentList.indexOfFirst { it.id == targetMessageId }
        if (index != -1) {
            binding.mesajRecyclerView.scrollToPosition(index)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        const val ARG_RECEIVER_ID = "arg_receiver_id"

        fun newInstance(receiverId: String): ChatFragment {
            return ChatFragment().apply {
                arguments = newArgs(receiverId)
            }
        }

        fun newArgs(receiverId: String): Bundle {
            return Bundle().apply {
                putString(ARG_RECEIVER_ID, receiverId)
            }
        }
    }
}