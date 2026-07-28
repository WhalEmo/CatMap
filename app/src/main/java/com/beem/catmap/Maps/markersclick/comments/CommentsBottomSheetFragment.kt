package com.beem.catmap.Maps.markersclick.comments

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.widget.doOnTextChanged
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.SimpleItemAnimator
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.Maps.markersclick.BottomSheetFragment
import com.beem.catmap.R
import com.beem.catmap.YorumYanit.Yanit_Model
import com.beem.catmap.YorumYanit.Yorum_Adapter
import com.beem.catmap.YorumYanit.Yorum_Model
import com.beem.catmap.data.repository.UserRepository
import com.beem.catmap.ui.navigation.NavigationHelper
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: CommentViewModel by viewModels()
    private lateinit var userRepository: UserRepository
    private lateinit var currentUser: Kullanici

    private lateinit var yorumlarRecyclerView: RecyclerView
    private lateinit var shimmerFrameLayout: com.facebook.shimmer.ShimmerFrameLayout
    private lateinit var yorumLayout: View
    private lateinit var yanitLayout: View
    private lateinit var carpiLayout: View
    private lateinit var iptalButton: View
    private lateinit var kimeYanitText: TextView
    private lateinit var yntEditText: EditText
    private lateinit var yorumEditText: EditText
    private lateinit var yorumGonderButton: View
    private lateinit var yanitGonderButton: View
    private lateinit var bosYorumText: TextView
    private lateinit var yorumGonderUserPp: CircleImageView
    private lateinit var yanitGonderUserPp: CircleImageView

    private lateinit var yorumAdapter: Yorum_Adapter
    private lateinit var paginationProgressBar: ProgressBar

    private var catId: String = ""
    private var hedefYorumId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)
        catId = arguments?.getString(ARG_CAT_ID).orEmpty()

        // Repository ve currentUser nesnesini burada 1 kez yüklüyoruz
        userRepository = UserRepository.getInstance(requireContext())
        currentUser = userRepository.getCurrentUser()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.yorum_gosterme, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)

        shimmerFrameLayout.startShimmer()
        shimmerFrameLayout.isVisible = true
        yorumlarRecyclerView.isVisible = false
        bosYorumText.isVisible = false

        setupRecyclerView()
        setupTextWatchers()
        observeViewModel()
        loadCurrentUserProfileImage()

        if (catId.isNotEmpty()) {
            viewModel.initCatId(catId)
        }
    }

    private fun initViews(view: View) {
        yorumlarRecyclerView = view.findViewById(R.id.yorumlarRecyclerView)
        yorumLayout = view.findViewById(R.id.yorumgndrLayout)
        yanitLayout = view.findViewById(R.id.yntgndrLayout)
        carpiLayout = view.findViewById(R.id.carpilayout)
        iptalButton = view.findViewById(R.id.iptalButton)
        kimeYanitText = view.findViewById(R.id.kimeyanit)
        yntEditText = view.findViewById(R.id.yntEditText)
        yorumEditText = view.findViewById(R.id.yorumEditText)
        shimmerFrameLayout = view.findViewById(R.id.shimmerFrameLayout)
        yorumGonderButton = view.findViewById(R.id.yorumgonder)
        yanitGonderButton = view.findViewById(R.id.yntgonder)
        bosYorumText = view.findViewById(R.id.bosYorumTextView)
        paginationProgressBar = view.findViewById(R.id.paginationProgressBar)
        yorumGonderUserPp = view.findViewById(R.id.YrmgndrFotoImageView)
        yanitGonderUserPp = view.findViewById(R.id.YntgndrFotoImageView)

        setButtonState(yorumGonderButton, false)
        setButtonState(yanitGonderButton, false)

        iptalButton.setOnClickListener { resetToCommentMode() }
        yorumGonderButton.setOnClickListener { sendComment() }
        yanitGonderButton.setOnClickListener { sendReply() }
    }

    private fun loadCurrentUserProfileImage() {
        // Doğrudan sınıf seviyesindeki currentUser nesnesini kullanıyoruz
        val photoUrl = currentUser.fotoUrl

        if (!photoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(yorumGonderUserPp)

            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(yanitGonderUserPp)
        } else {
            yorumGonderUserPp.setImageResource(R.drawable.kullanici)
            yanitGonderUserPp.setImageResource(R.drawable.kullanici)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        yorumlarRecyclerView.layoutManager = layoutManager

        (yorumlarRecyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        val currentUserId = currentUser.id
        yorumAdapter = Yorum_Adapter(requireContext(), currentUserId)
        yorumlarRecyclerView.adapter = yorumAdapter

        yorumAdapter.setOnYorumInteractionListener(object : Yorum_Adapter.OnYorumInteractionListener {
            override fun onKalpTiklandi(yorum: Yorum_Model) {
                viewModel.toggleBegeni(catId, yorum, currentUser.id)
            }

            override fun onYanitlariGorTiklandi(yorum: Yorum_Model) {
                yorum.yorumID?.let { id ->
                    hedefYorumId = id
                    viewModel.toggleYanitlarGorunurluk(id)
                }
            }

            override fun onYanitlaTiklandi(yorum: Yorum_Model) {
                hedefYorumId = yorum.yorumID
                carpiLayout.isVisible = true
                yorumLayout.isVisible = false
                yanitLayout.isVisible = true
                kimeYanitText.text = "@${yorum.kullaniciAdi}"
                yntEditText.requestFocus()
            }

            override fun onYanitYanitlaTiklandi(yanit: Yanit_Model, yorumId: String) {
                hedefYorumId = yorumId
                carpiLayout.isVisible = true
                yorumLayout.isVisible = false
                yanitLayout.isVisible = true
                kimeYanitText.text = "@${yanit.adi}"
                yntEditText.requestFocus()
            }

            override fun onKullaniciAdiTiklandi(userId: String) {
                dismiss()
                val parentBottomSheet = parentFragmentManager.findFragmentByTag(BottomSheetFragment.TAG)
                if (parentBottomSheet is BottomSheetDialogFragment) {
                    parentBottomSheet.dismiss()
                }
                NavigationHelper.navigateToProfile(userId)
            }

            override fun onSilTiklandi(yorum: Yorum_Model) {
                yorum.yorumID?.let { id -> viewModel.deleteComment(id) }
            }

            override fun onGuncelleTiklandi(yorum: Yorum_Model) {
                val context = requireContext()
                val builder = android.app.AlertDialog.Builder(context, R.style.ModernAlertDialog)
                builder.setTitle("Yorumu Güncelle")

                val input = EditText(context).apply {
                    setText(yorum.yorumicerik)
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                    maxLines = 6
                    gravity = android.view.Gravity.TOP or android.view.Gravity.START
                    background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.edittext_oval_bg)
                    setPadding(30, 30, 30, 30)
                }
                builder.setView(input)

                builder.setPositiveButton("Güncelle") { _, _ ->
                    val yeniYorum = input.text.toString().trim()
                    if (yeniYorum.isNotEmpty() && yorum.yorumID != null) {
                        viewModel.updateComment(yorum.yorumID, yeniYorum)
                        android.widget.Toast.makeText(context, "Yorum güncellendi", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
                val dialog = builder.create()
                dialog.show()
            }

            override fun onYanitKalpTiklandi(yanit: Yanit_Model, yorumId: String) {
                viewModel.toggleYanitBegeni(catId, yorumId, yanit, currentUser.id)
            }

            override fun onYanitSilTiklandi(yanit: Yanit_Model, yorumId: String) {
                yanit.yanitId?.let { yanitId ->
                    viewModel.deleteYanit(yorumId, yanitId)
                }
            }

            override fun onYanitGuncelleTiklandi(yanit: Yanit_Model, yorumId: String) {
                val context = requireContext()
                val builder = android.app.AlertDialog.Builder(context, R.style.ModernAlertDialog)
                builder.setTitle("Yanıtı Güncelle")

                val input = EditText(context).apply {
                    setText(yanit.yaniticerik)
                    inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                    maxLines = 6
                    gravity = android.view.Gravity.TOP or android.view.Gravity.START
                    background = androidx.core.content.ContextCompat.getDrawable(context, R.drawable.edittext_oval_bg)
                    setPadding(30, 30, 30, 30)
                }
                builder.setView(input)

                builder.setPositiveButton("Güncelle") { _, _ ->
                    val yeniIcerik = input.text.toString().trim()
                    if (yeniIcerik.isNotEmpty() && yanit.yanitId != null) {
                        viewModel.updateYanit(yorumId, yanit.yanitId, yeniIcerik)
                        android.widget.Toast.makeText(context, "Yanıt güncellendi", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
                val dialog = builder.create()
                dialog.show()
            }

            override fun onDahaFazlaYanitGetirTiklandi(yorum: Yorum_Model) {
                yorum.yorumID?.let { yorumId ->
                    viewModel.yanitlariYukle(yorumId, 3, false)
                }
            }
        })

        yorumlarRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount && firstVisibleItemPosition >= 0) {
                        viewModel.loadMoreComments()
                    }
                }
            }
        })
    }
    private fun setupTextWatchers() {
        yorumEditText.doOnTextChanged { text, _, _, _ -> setButtonState(yorumGonderButton, !text.isNullOrBlank()) }
        yntEditText.doOnTextChanged { text, _, _, _ -> setButtonState(yanitGonderButton, !text.isNullOrBlank()) }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.comments.collectLatest { list ->
                        yorumAdapter.submitYorumList(list)
                        shimmerFrameLayout.stopShimmer()
                        shimmerFrameLayout.isVisible = false
                        val isEmpty = viewModel.isEmpty.value
                        yorumlarRecyclerView.isVisible = !isEmpty
                        bosYorumText.isVisible = isEmpty
                    }
                }
                launch {
                    viewModel.isEmpty.collectLatest { isEmpty ->
                        bosYorumText.isVisible = isEmpty
                        yorumlarRecyclerView.isVisible = !isEmpty
                    }
                }
                launch {
                    viewModel.actionSuccess.collectLatest {
                        yorumEditText.setText("")
                        yntEditText.setText("")
                        resetToCommentMode()
                    }
                }
                launch {
                    viewModel.isPaginationLoading.collect { isLoading ->
                        paginationProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
            }
        }
    }
    private fun sendComment() {
        val text = yorumEditText.text.toString().trim()
        if (text.isNotEmpty()) viewModel.sendComment(text)
    }
    private fun sendReply() {
        val targetId = hedefYorumId ?: return

        val etiket = kimeYanitText.text.toString().trim()
        val mesaj = yntEditText.text.toString().trim()

        val formatliEtiket = if (etiket.startsWith("@")) etiket else "@$etiket"
        val birlesmisIcerik = "$formatliEtiket $mesaj"

        if (mesaj.isNotEmpty()) {
            viewModel.sendReply(targetId, birlesmisIcerik) { _ ->
                yntEditText.setText("")
                resetToCommentMode()
            }
        }
    }

    private fun resetToCommentMode() {
        carpiLayout.isVisible = false
        yanitLayout.isVisible = false
        yorumLayout.isVisible = true
        hedefYorumId = null
    }

    private fun setButtonState(view: View, enabled: Boolean) {
        view.isEnabled = enabled
        view.alpha = if (enabled) 1f else 0.5f
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<FrameLayout>(com.google.android.material.R.id.design_bottom_sheet) ?: return
        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isFitToContents = false
            expandedOffset = 0
        }
    }
    override fun show(manager: FragmentManager, tag: String?) {
        val existingFragment = manager.findFragmentByTag(tag)
        if (existingFragment != null && (existingFragment.isAdded || existingFragment.isStateSaved)) return
        try {
            super.show(manager, tag)
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }
    }
    companion object {
        @JvmField
        val TAG = "CommentsBottomSheetFragment"
        private const val ARG_CAT_ID = "arg_cat_id"

        @JvmStatic
        fun newInstance(catId: String): CommentsBottomSheetFragment {
            return CommentsBottomSheetFragment().apply {
                arguments = Bundle().apply { putString(ARG_CAT_ID, catId) }
            }
        }
    }
}