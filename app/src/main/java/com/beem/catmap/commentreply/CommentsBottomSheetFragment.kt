package com.beem.catmap.commentreply

import android.app.AlertDialog
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
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
import com.beem.catmap.ui.markersclick.BottomSheetFragment
import com.beem.catmap.R
import com.beem.catmap.models.ReplyModel
import com.beem.catmap.models.CommentModel
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.extensions.kalpAnimasyonuYap
import com.beem.catmap.ui.navigation.NavigationHelper
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CommentsBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: CommentViewModel by viewModels()
    private lateinit var currentUserManager: CurrentUserManager
    private lateinit var currentUser: Kullanici

    private lateinit var commentRecyclerView: RecyclerView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var commentLayout: View
    private lateinit var replyLayout: View
    private lateinit var cancelLayout: View
    private lateinit var cancelButton: View
    private lateinit var replyToTextView: TextView
    private lateinit var replyEditText: EditText
    private lateinit var commentEditText: EditText
    private lateinit var sendCommentButton: View
    private lateinit var sendReplyButton: View
    private lateinit var emptyCommentText: TextView
    private lateinit var sendCommentUserPp: CircleImageView
    private lateinit var sendReplyUserPp: CircleImageView

    private lateinit var commentAdapter: CommentAdapter
    private lateinit var paginationProgressBar: ProgressBar

    private var catId: String = ""
    private var targetCommentId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)
        catId = arguments?.getString(ARG_CAT_ID).orEmpty()

        currentUserManager = CurrentUserManager.getInstance(requireContext())
        currentUser = currentUserManager.getCurrentUser()
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
        commentRecyclerView.isVisible = false
        emptyCommentText.isVisible = false

        setupRecyclerView()
        setupTextWatchers()
        observeViewModel()
        loadCurrentUserProfileImage()

        if (catId.isNotEmpty()) {
            viewModel.initCatId(catId)
        }
    }

    private fun initViews(view: View) {
        commentRecyclerView = view.findViewById(R.id.yorumlarRecyclerView)
        commentLayout = view.findViewById(R.id.yorumgndrLayout)
        replyLayout = view.findViewById(R.id.yntgndrLayout)
        cancelLayout = view.findViewById(R.id.carpilayout)
        cancelButton = view.findViewById(R.id.iptalButton)
        replyToTextView = view.findViewById(R.id.kimeyanit)
        replyEditText = view.findViewById(R.id.yntEditText)
        commentEditText = view.findViewById(R.id.yorumEditText)
        shimmerFrameLayout = view.findViewById(R.id.shimmerFrameLayout)
        sendCommentButton = view.findViewById(R.id.yorumgonder)
        sendReplyButton = view.findViewById(R.id.yntgonder)
        emptyCommentText = view.findViewById(R.id.bosYorumTextView)
        paginationProgressBar = view.findViewById(R.id.paginationProgressBar)
        sendCommentUserPp = view.findViewById(R.id.YrmgndrFotoImageView)
        sendReplyUserPp = view.findViewById(R.id.YntgndrFotoImageView)

        setButtonState(sendCommentButton, false)
        setButtonState(sendReplyButton, false)

        cancelButton.setOnClickListener { resetToCommentMode() }
        sendCommentButton.setOnClickListener { sendComment() }
        sendReplyButton.setOnClickListener { sendReply() }
    }

    private fun loadCurrentUserProfileImage() {
        val photoUrl = currentUser.fotoUrl

        if (!photoUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(sendCommentUserPp)

            Glide.with(this)
                .load(photoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(sendReplyUserPp)
        } else {
            sendCommentUserPp.setImageResource(R.drawable.kullanici)
            sendReplyUserPp.setImageResource(R.drawable.kullanici)
        }
    }

    private fun setupRecyclerView() {
        val layoutManager = LinearLayoutManager(requireContext())
        commentRecyclerView.layoutManager = layoutManager

        (commentRecyclerView.itemAnimator as? SimpleItemAnimator)?.supportsChangeAnimations = false
        val currentUserId = currentUser.id
        commentAdapter = CommentAdapter(requireContext(), currentUserId)
        commentRecyclerView.adapter = commentAdapter

        commentAdapter.setOnYorumInteractionListener(object : CommentAdapter.OnYorumInteractionListener {
            override fun onCommentLikeClicked(yorum: CommentModel, kalpView: ImageView) {
                kalpView.kalpAnimasyonuYap()
                viewModel.toggleCommentLike(catId, yorum, currentUser.id)
            }

            override fun onShowRepliesClicked(yorum: CommentModel) {
                yorum.commentId?.let { id ->
                    targetCommentId = id
                    viewModel.toggleRepliesVisibility(id)
                }
            }

            override fun onReplyClicked(yorum: CommentModel) {
                targetCommentId = yorum.commentId
                cancelLayout.isVisible = true
                commentLayout.isVisible = false
                replyLayout.isVisible = true
                replyToTextView.text = "@${yorum.username}"
                replyEditText.requestFocus()
            }

            override fun onShowReplyRepliesClicked(yanit: ReplyModel, yorumId: String) {
                targetCommentId = yorumId
                cancelLayout.isVisible = true
                commentLayout.isVisible = false
                replyLayout.isVisible = true
                replyToTextView.text = "@${yanit.name}"
                replyEditText.requestFocus()
            }

            override fun onUsernameClicked(userId: String) {
                dismiss()
                val parentBottomSheet = parentFragmentManager.findFragmentByTag(BottomSheetFragment.TAG)
                if (parentBottomSheet is BottomSheetDialogFragment) {
                    parentBottomSheet.dismiss()
                }
                NavigationHelper.navigateToProfile(userId)
            }

            override fun onDeleteClicked(yorum: CommentModel) {
                yorum.commentId?.let { id -> viewModel.deleteComment(id) }
            }

            override fun onUpdateClicked(yorum: CommentModel) {
                val context = requireContext()
                val builder = AlertDialog.Builder(context, R.style.ModernAlertDialog)
                builder.setTitle("Yorumu Güncelle")

                val input = EditText(context).apply {
                    setText(yorum.commentContent)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                    maxLines = 6
                    gravity = Gravity.TOP or Gravity.START
                    background = ContextCompat.getDrawable(context, R.drawable.edittext_oval_bg)
                    setPadding(30, 30, 30, 30)
                }
                builder.setView(input)

                builder.setPositiveButton("Güncelle") { _, _ ->
                    val yeniYorum = input.text.toString().trim()
                    if (yeniYorum.isNotEmpty() && yorum.commentId != null) {
                        viewModel.updateComment(yorum.commentId, yeniYorum)
                        Toast.makeText(context, "Yorum güncellendi", Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
                val dialog = builder.create()
                dialog.show()
            }

            override fun onReplyLikeClicked(yanit: ReplyModel, yorumId: String, kalpView: ImageView) {
                kalpView.kalpAnimasyonuYap()
                viewModel.toggleReplyLike(catId, yorumId, yanit, currentUser.id)
            }

            override fun onDeleteReply(yanit: ReplyModel, yorumId: String) {
                yanit.replyId?.let { yanitId ->
                    viewModel.deleteReply(yorumId, yanitId)
                }
            }

            override fun onReplyUpdate(yanit: ReplyModel, yorumId: String) {
                val context = requireContext()
                val builder = AlertDialog.Builder(context, R.style.ModernAlertDialog)
                builder.setTitle("Yanıtı Güncelle")

                val input = EditText(context).apply {
                    setText(yanit.replyContent)
                    inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_MULTI_LINE
                    minLines = 3
                    maxLines = 6
                    gravity = Gravity.TOP or Gravity.START
                    background = ContextCompat.getDrawable(context, R.drawable.edittext_oval_bg)
                    setPadding(30, 30, 30, 30)
                }
                builder.setView(input)

                builder.setPositiveButton("Güncelle") { _, _ ->
                    val yeniIcerik = input.text.toString().trim()
                    if (yeniIcerik.isNotEmpty() && yanit.replyId != null) {
                        viewModel.updateReply(yorumId, yanit.replyId, yeniIcerik)
                        Toast.makeText(context, "Yanıt güncellendi", Toast.LENGTH_SHORT).show()
                    }
                }
                builder.setNegativeButton("İptal") { dialog, _ -> dialog.cancel() }
                val dialog = builder.create()
                dialog.show()
            }

            override fun onLoadMoreRepliesClicked(yorum: CommentModel) {
                yorum.commentId?.let { yorumId ->
                    viewModel.loadReplies(yorumId, 3, false)
                }
            }
        })

        commentRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
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
        commentEditText.doOnTextChanged { text, _, _, _ -> setButtonState(sendCommentButton, !text.isNullOrBlank()) }
        replyEditText.doOnTextChanged { text, _, _, _ -> setButtonState(sendReplyButton, !text.isNullOrBlank()) }
    }
    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.comments.collectLatest { list ->
                        commentAdapter.submitYorumList(list)
                    }
                }
                launch {
                    viewModel.isLoading.collect { isLoading ->
                        if (isLoading) {
                            shimmerFrameLayout.isVisible = true
                            shimmerFrameLayout.startShimmer()
                            commentRecyclerView.isVisible = false
                            emptyCommentText.isVisible = false
                        } else {
                            shimmerFrameLayout.stopShimmer()
                            shimmerFrameLayout.isVisible = false
                            val isEmpty = viewModel.isEmpty.value
                            commentRecyclerView.isVisible = !isEmpty
                            emptyCommentText.isVisible = isEmpty
                        }
                    }
                }
                launch {
                    viewModel.isEmpty.collectLatest { isEmpty ->
                        emptyCommentText.isVisible = isEmpty
                        commentRecyclerView.isVisible = !isEmpty
                    }
                }
                launch {
                    viewModel.isPaginationLoading.collect { isLoading ->
                        paginationProgressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
                    }
                }
                launch {
                    viewModel.actionSuccess.collectLatest { successMessage ->
                        Toast.makeText(requireContext(), successMessage, Toast.LENGTH_SHORT).show()
                        commentEditText.setText("")
                        replyEditText.setText("")
                        resetToCommentMode()
                    }
                }
                launch {
                    viewModel.errorMessage.collectLatest { errorMessage ->
                        Toast.makeText(requireContext(), errorMessage, Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
    private fun sendComment() {
        val text = commentEditText.text.toString().trim()
        if (text.isNotEmpty()) viewModel.sendComment(text)
    }
    private fun sendReply() {
        val targetId = targetCommentId ?: return

        val etiket = replyToTextView.text.toString().trim()
        val mesaj = replyEditText.text.toString().trim()

        val formatliEtiket = if (etiket.startsWith("@")) etiket else "@$etiket"
        val birlesmisIcerik = "$formatliEtiket $mesaj"

        if (mesaj.isNotEmpty()) {
            viewModel.sendReply(targetId, birlesmisIcerik) { _ ->
                replyEditText.setText("")
                resetToCommentMode()
            }
        }
    }
    private fun resetToCommentMode() {
        cancelLayout.isVisible = false
        replyLayout.isVisible = false
        commentLayout.isVisible = true
        targetCommentId = null
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