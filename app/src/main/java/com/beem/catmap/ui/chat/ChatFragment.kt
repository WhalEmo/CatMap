package com.beem.catmap.ui.chat

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupWindow
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.graphics.drawable.toDrawable
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
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.Maps.MapsActivity
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.databinding.MesajlasmaBinding
import com.beem.catmap.databinding.DialogMessageDeleteBinding
import com.beem.catmap.mesaj.Mesaj
import com.beem.catmap.mesaj.MesajFotoGonderYonetici
import com.beem.catmap.models.ChatMessage
import com.beem.catmap.ui.chat.dialogs.EditMessageDialogFragment
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
        val linearLayoutManager = LinearLayoutManager(context).apply {
            stackFromEnd = true // İlk açılışta listenin en altından başlar
        }
        mesajAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)

                val totalItemCount = mesajAdapter.itemCount

                // 🚨 SIFIRLA BÖLÜNME / NEGARİF İNDEKS KORUMASI
                if (totalItemCount <= 0) return

                val targetPosition = totalItemCount - 1
                if (targetPosition < 0) return // Geçersiz pozisyon koruması

                val lastCompletelyVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()

                // Sadece en alta yeni eleman eklendiyse
                val isAddedToBottom = positionStart >= (totalItemCount - itemCount)

                // Kullanıcı en altlarda mı? (Eğer liste çok kısaysa (örn 2 mesaj varsa) doğrudan true kabul et)
                val isUserAtBottom = lastCompletelyVisiblePosition == -1 ||
                        lastCompletelyVisiblePosition >= (totalItemCount - itemCount - 4)

                if (isAddedToBottom && isUserAtBottom) {
                    // RecyclerView'ın layout hesaplamasını tamamlamasını bekleyip güvenle kaydırıyoruz
                    binding.mesajRecyclerView.post {
                        if (targetPosition < mesajAdapter.itemCount) {
                            binding.mesajRecyclerView.smoothScrollToPosition(targetPosition)
                        }
                    }
                }
            }
        })

        binding.mesajRecyclerView.apply {
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (dy < 0) {
                        val firstVisibleItemPosition = linearLayoutManager.findFirstVisibleItemPosition()

                        // Kullanıcı listenin en üstündeki ilk 3 mesajın sınırına geldi mi?
                        if (firstVisibleItemPosition <= 3 && firstVisibleItemPosition != -1) {
                            viewModel.loadOlderMessages()
                        }
                    }
                }
            })

            adapter = mesajAdapter
            layoutManager = linearLayoutManager
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


        if (mesajAdapter.currentList != state.messages) {
            mesajAdapter.submitList(state.messages)
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

    private fun showOptionMenu(message: ChatMessage, anchorView: View) {
        val menuView = LayoutInflater.from(requireContext()).inflate(R.layout.mesaj_secenek_menu, null)

        val popupWindow = PopupWindow(
            menuView,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            true
        ).apply {
            isOutsideTouchable = true
            elevation = 10f
        }

        val btnMesajGuncelle = menuView.findViewById<View>(R.id.btnMesajGuncelle)
        val btnMesajSil = menuView.findViewById<View>(R.id.btnMesajSil)

        if (message is ChatMessage.Photo) {
            btnMesajGuncelle?.isVisible = false
        }

        btnMesajGuncelle?.setOnClickListener {
            popupWindow.dismiss()
            val currentText = when (message) {
                is ChatMessage.Text -> message.message
                is ChatMessage.Reply -> message.message
                else -> return@setOnClickListener
            }

            val editDialog = EditMessageDialogFragment.newInstance(currentText)
            editDialog.setOnSaveClickListener { newText ->
                viewModel.updateMessage(message.id, newText)
            }
            editDialog.show(childFragmentManager, "EditMessageDialog")
        }

        btnMesajSil?.setOnClickListener {
            popupWindow.dismiss()
            showDeleteConfirmDialog(message.id)
        }
        popupWindow.showAsDropDown(anchorView, 0, -anchorView.height / 2)
    }


    private fun showDeleteConfirmDialog(messageId: String) {
        val dialogBinding = DialogMessageDeleteBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CatMapDialogTheme)
            .setView(dialogBinding.root)
            .create()

        dialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())

        // 4. Buton Tıklamaları
        dialogBinding.btnVazgec.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSil.setOnClickListener {
            viewModel.deleteMessage(messageId)
            dialog.dismiss()
        }

        dialog.show()

        // 5. Ekran genişliğine göre dialog'u %85 oranında hizala
        dialog.window?.setLayout(
            (resources.displayMetrics.widthPixels * 0.85).toInt(),
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT
        )
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