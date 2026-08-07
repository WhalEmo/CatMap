package com.beem.catmap.ui.message

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.util.Log
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
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.Maps.MapsActivity
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.databinding.DialogMessageDeleteBinding
import com.beem.catmap.databinding.MesajlasmaBinding
import com.beem.catmap.models.ChatMessage
import com.beem.catmap.ui.extensions.fadeIn
import com.beem.catmap.ui.extensions.fadeOut
import com.beem.catmap.ui.message.dialogs.EditMessageDialogFragment
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MessageFragment : Fragment() {

    private var _binding: MesajlasmaBinding? = null
    private val binding get() = _binding!!

    private val receiverId: String by lazy {
        arguments?.getString(ARG_RECEIVER_ID) ?: throw IllegalArgumentException("Receiver ID gerekli!")
    }

    private val viewModel: MessageViewModel by viewModels {
        MessageViewModelFactory(
            currentUserManager = CurrentUserManager.getInstance(requireContext()),
            receiverId = receiverId
        )
    }

    private lateinit var mesajAdapter: MessageAdapter

    private val galeriLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val data = result.data ?: return@registerForActivityResult
            val selectedUris = mutableListOf<android.net.Uri>()

            // Çoklu seçim kontrolü
            data.clipData?.let { clipData ->
                for (i in 0 until clipData.itemCount) {
                    selectedUris.add(clipData.getItemAt(i).uri)
                }
            } ?: data.data?.let { uri ->
                // Tekli seçim kontrolü
                selectedUris.add(uri)
            }

            if (selectedUris.isNotEmpty()) {
                // 🚀 Singleton Yonetici yerine doğrudan ViewModel'a paslıyoruz!
                viewModel.sendPhotos(selectedUris)
            }
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

        if (requireActivity() is MapsActivity) {
            requireActivity().findViewById<View>(R.id.bottom_navigation)?.isVisible = false
        }

        setupRecyclerView()
        setupListeners()
        setupScrollToBottomButton()
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
            stackFromEnd = true
        }

        mesajAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                val totalItemCount = mesajAdapter.itemCount
                if (totalItemCount <= 0) return

                val targetPosition = totalItemCount - 1
                if (targetPosition < 0) return

                val lastCompletelyVisiblePosition = linearLayoutManager.findLastCompletelyVisibleItemPosition()
                val isAddedToBottom = positionStart >= (totalItemCount - itemCount)
                val isUserAtBottom = lastCompletelyVisiblePosition == -1 ||
                        lastCompletelyVisiblePosition >= (totalItemCount - itemCount - 4)

                if (isAddedToBottom && isUserAtBottom) {
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
                        if (firstVisibleItemPosition <= 3 && firstVisibleItemPosition != -1) {
                            viewModel.loadOlderMessages()
                        }
                    }
                }
            })

            adapter = mesajAdapter
            layoutManager = linearLayoutManager
        }

        setupSwipeToReply()
    }

    private fun setupSwipeToReply() {
        val swipeHandler = object : ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT) {

            override fun getSwipeDirs(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder): Int {
                val position = viewHolder.bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return 0

                val message = mesajAdapter.currentList[position]
                val isMyMessage = message.senderId == UserSession.userId

                // Kendi mesajımızsa SADECE SOLA (LEFT), karşı tarafınsa SADECE SAĞA (RIGHT) izin ver
                return if (isMyMessage) ItemTouchHelper.LEFT else ItemTouchHelper.RIGHT
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                val position = viewHolder.bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val message = mesajAdapter.currentList[position]

                    if (!mesajAdapter.isBlocked) {
                        viewModel.setReplyMessage(message)
                        binding.mesajEditText.requestFocus()
                    }

                    // Sağa/sola çekilen kartın ekrandan gitmeyip yerine geri esnemesi için:
                    mesajAdapter.notifyItemChanged(position)
                }
            }

            override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float = 0.2f

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                // Maksimum kaydırma mesafesini limitleme (Sağ için +150px, Sol için -150px)
                val maxSwipePx = 150f
                val limitedDX = when {
                    dX > maxSwipePx -> maxSwipePx
                    dX < -maxSwipePx -> -maxSwipePx
                    else -> dX
                }

                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    limitedDX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }

        ItemTouchHelper(swipeHandler).attachToRecyclerView(binding.mesajRecyclerView)
    }

    private fun setupScrollToBottomButton() {
        // 1. Tıklama Olayı: En son mesaja yumuşakça kaydır
        binding.btnScrollToBottom.setOnClickListener {
            val lastPosition = mesajAdapter.itemCount - 1
            if (lastPosition >= 0) {
                binding.mesajRecyclerView.smoothScrollToPosition(lastPosition)
            }
        }

        // 2. Scroll Dinleyicisi: Kullanıcı yukarı kaydırdı mı kontrol et
        binding.mesajRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
                val lastCompletelyVisibleItem = layoutManager.findLastCompletelyVisibleItemPosition()
                val totalItemCount = mesajAdapter.itemCount

                // Kullanıcı en alt mesajı göremiyorsa BUTONU GÖSTER, en alt mesaja ulaştıysa GİZLE
                val isAtBottom = lastCompletelyVisibleItem >= totalItemCount - 2 // Son 1-2 mesaj mesafesi

                if (!isAtBottom && totalItemCount > 5) {
                    if (!binding.btnScrollToBottom.isShown) {
                        binding.btnScrollToBottom.fadeIn()
                    }
                } else {
                    if (binding.btnScrollToBottom.isShown) {
                        binding.btnScrollToBottom.fadeOut()
                    }
                }
            }
        })
    }

    private fun setupListeners() {
        binding.btnBack.setOnClickListener {
            SmartNavigationEngine.navigateBack()
        }

        binding.gonderButton.setOnClickListener {
            val text = binding.mesajEditText.text.toString()
            viewModel.sendMessage(text)
            binding.mesajEditText.text?.clear()
        }

        binding.mesajEditText.doOnTextChanged { text, _, _, _ ->
            viewModel.onTextChanged(text.toString())
        }

        binding.fotoEkleButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK).apply {
                type = "image/*"
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            }
            galeriLauncher.launch(intent)
        }

        binding.engelKaldir.setOnClickListener {
            viewModel.removeBlock()
        }

        // Yanıtlama Kutusunu Kapat
        binding.cevapKapatButton.setOnClickListener {
            viewModel.setReplyMessage(null)
        }
    }

    override fun onPause() {
        super.onPause()
        Log.d("LifecycleDebug", "⏸️ Fragment onPause() - Ekran arka plana gidiyor veya kapandı!")
    }

    override fun onStop() {
        super.onStop()
        Log.d("LifecycleDebug", "🛑 Fragment onStop() - Ekran artık görünmüyor!")
    }

    override fun onResume() {
        super.onResume()
        Log.d("LifecycleDebug", "▶️ Fragment onResume() - Ekran tam ön planda!")
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                Log.d("LifecycleDebug", "🟢 repeatOnLifecycle(RESUMED) Bloğuna GİRİLDİ")

                viewModel.uiState.collectLatest { state ->
                    Log.d("LifecycleDebug", "📩 uiState Emit Geldi! (Mesaj Sayısı: ${state.messages.size})")
                    renderUi(state)
                    viewModel.markUnreadMessagesAsRead()
                }
            }
        }
    }

    private fun renderUi(state: MessageUiState) {
        if (state.receiverName.isNotEmpty()) {
            binding.kisiAdiText.text = state.receiverName
        }

        if (state.receiverPhotoUrl.isNotEmpty()) {
            com.squareup.picasso.Picasso.get()
                .load(state.receiverPhotoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(binding.kisiProfilFoto)
        }

        binding.kisiDurumText.text = if (state.isOtherUserTyping) "Yazıyor..." else state.receiverStatus

        if (mesajAdapter.currentList != state.messages) {
            mesajAdapter.submitList(state.messages)
        }

        binding.yukleniyorProgress.isVisible = state.isLoading
        binding.mesajRecyclerView.isVisible = !state.isLoading

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

        // 🎯 YANITLAMA KUTUSU (REPLY LAYOUT) GÖRÜNÜRLÜK VE METİN DÜZENLEMESİ
        binding.cevapAlani.isVisible = state.replyMessage != null && !isBlocked
        state.replyMessage?.let { msg ->
            binding.cevapMetni.text = when (msg) {
                is ChatMessage.Text -> msg.message
                is ChatMessage.Photo -> "📷 Fotoğraf"
                is ChatMessage.Reply -> msg.message
                is ChatMessage.Deleted -> msg.message
            }
        }
    }

    private fun openPhotoPreview(photoUrls: List<String>) {
        if (photoUrls.isEmpty()) return

        val dialog = MessagePhotoPreviewDialog.newInstance(photoUrls)
        dialog.show(childFragmentManager, "MessagePhotoPreviewDialog")
    }

    private fun setupKeyboardAdjustments() {
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
        if (message is ChatMessage.Deleted) return
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

        dialogBinding.btnVazgec.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnSil.setOnClickListener {
            viewModel.deleteMessage(messageId)
            dialog.dismiss()
        }

        dialog.show()

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

        fun newInstance(receiverId: String): MessageFragment {
            return MessageFragment().apply {
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