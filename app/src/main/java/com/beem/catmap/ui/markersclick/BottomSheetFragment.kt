package com.beem.catmap.ui.markersclick

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.Post
import com.beem.catmap.databinding.MarkerdakiKediyiGostermeBinding
import com.beem.catmap.maps.FotoGeciciAdapter
import com.beem.catmap.maps.mapkedi.Kediler
import com.beem.catmap.ui.commentreply.CommentViewModel
import com.beem.catmap.ui.commentreply.CommentsBottomSheetFragment
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.components.CatMapPopupMenu
import com.beem.catmap.ui.extensions.getFormattedDate
import com.beem.catmap.ui.extensions.kalpAnimasyonuYap
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.report.ReportType
import com.beem.catmap.utils.toFirebaseTimestamp
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: MarkerdakiKediyiGostermeBinding? = null
    private val binding get() = _binding!!

    private val photoIndicatorDots = mutableListOf<View>()

    private val photoPageChangeCallback =
        object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePhotoIndicator(position)
            }
        }

    private lateinit var fotoAdapter: FotoGeciciAdapter
    private val viewModel: CatDetailViewModel by activityViewModels()
    private val commentsViewModel: CommentViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = MarkerdakiKediyiGostermeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onDestroyView() {
        binding.fotoPager.unregisterOnPageChangeCallback(photoPageChangeCallback)
        photoIndicatorDots.clear()
        _binding = null
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews()
        loadStart()
        observeViewModel()
        setupCommentCountObserver()

        @Suppress("DEPRECATION")
        val cat = arguments?.getSerializable(ARG_CAT) as? Kediler

        cat?.let { kedi ->
            viewModel.setCatData(kedi)

            kedi.id?.let { catId ->
                commentsViewModel.initCatId(catId)
                yorumSayisiToplam()
            }

            if (!kedi.yukleyenId.isNullOrEmpty()) {
                viewModel.loadOwnerInfo(kedi.yukleyenId)
            } else {
                contentShow()
            }

            binding.profilAlani.setOnClickListener {
                yukleyenProfilineGit(kedi.yukleyenId)
            }
        }

        val openCommentsAction = View.OnClickListener {
            val currentCatId = viewModel.selectedCat.value?.id
            if (!currentCatId.isNullOrEmpty()) {
                val commentsFragment = CommentsBottomSheetFragment.newInstance(currentCatId)
                commentsFragment.show(parentFragmentManager, CommentsBottomSheetFragment.TAG)
            }
        }

        binding.yorumSayisiTextView.setOnClickListener(openCommentsAction)
        binding.btnYorumlar.setOnClickListener(openCommentsAction)

        binding.kalpImageView.setOnClickListener {
            binding.kalpImageView.kalpAnimasyonuYap()
            viewModel.toggleLike()
        }

        binding.GonderiEkleButton.setOnClickListener { anchorView ->
            showOptionMenu(anchorView)
        }
    }

    private fun initViews() {
        fotoAdapter = FotoGeciciAdapter(requireContext(), null)
        binding.fotoPager.adapter = fotoAdapter
        binding.fotoPager.offscreenPageLimit = 1

        binding.fotoPager.registerOnPageChangeCallback(photoPageChangeCallback)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCat.collectLatest { cat ->
                cat?.let {
                    binding.isimgosterme.text = it.isim
                    if (it.hakkindasi.isNullOrBlank()) {
                        binding.hakkindagosterme.text = "Bu sevimli dostumuz için henüz bir açıklama eklenmemiş. 🐾"
                    } else {
                        binding.hakkindagosterme.text = it.hakkindasi
                    }
                    val locationParts = listOfNotNull(
                        it.city?.takeIf { it.isNotBlank() },
                        it.district?.takeIf { it.isNotBlank() },
                        it.neighborhood?.takeIf { it.isNotBlank() }
                    )
                    if (locationParts.isNotEmpty()) {
                        binding.konumChip.isVisible = true
                        binding.konumText.text = locationParts.joinToString(" • ")
                    } else {
                        binding.konumChip.visibility = View.GONE
                    }
                    binding.tarihText.text = getFormattedDate(it.createdAt) ?: ""

                    if (!it.urLler.isNullOrEmpty()) {
                        val uriList = withContext(Dispatchers.IO) {
                            it.urLler.mapNotNull { url -> url.toUri() }
                        }
                        fotoAdapter.submitList(uriList)
                        setupPhotoIndicator(uriList.size)
                    } else {
                        fotoAdapter.submitList(emptyList())
                        setupPhotoIndicator(0)
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.ownerInfo.collectLatest { ownerData ->
                ownerData?.let { (username, photoUrl) ->
                    binding.yukleyenAdiText.text = "@$username"
                    Glide.with(this@BottomSheetFragment)
                        .load(photoUrl)
                        .placeholder(R.drawable.kullanici)
                        .dontAnimate()
                        .into(binding.YukprofilFotoImageView)

                    contentShow()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.likeCount.collectLatest { count ->
                binding.begeniSayisiTextView.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLiked.collectLatest { isLiked ->
                val iconRes = if (isLiked) {
                    R.drawable.baseline_favorite_24
                } else {
                    R.drawable.baseline_favorite_border_24
                }
                binding.kalpImageView.setImageResource(iconRes)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.catDeletedSuccess.collectLatest { success ->
                if (success) {
                    UiMessageManager.emitMessage(UiMessageState.Info("Kedi haritadan silindi."))
                    dismiss()
                }
            }
        }
    }

    private fun showOptionMenu(view: View) {
        val isCatAdded = viewModel.isAlreadyAdded.value
        val currentCat = viewModel.selectedCat.value ?: return
        val likeCount = viewModel.likeCount.value
        val isMyCat = currentCat.yukleyenId == UserSession.userId

        val redColor = ContextCompat.getColor(requireContext(), R.color.catmap_error)
        val successColor = ContextCompat.getColor(requireContext(), R.color.catmap_success)
        val addCatColor = ContextCompat.getColor(requireContext(), R.color.catmap_text_dark)

        CatMapPopupMenu.Builder(requireContext())
            .addItem(
                id = R.id.gonderi_ekle,
                title = if (isCatAdded) "Gönderilerinizde Ekli" else "Gönderilerime Ekle",
                iconRes = if (isCatAdded) R.drawable.ic_check_circle else R.drawable.ic_add,
                textColor = if (isCatAdded) successColor else addCatColor,
                iconTint = if (isCatAdded) successColor else addCatColor,
                isEnabled = !isCatAdded,
                isVisible = isMyCat
            ) {
                showAddCatConfirmationDialog(currentCat, likeCount)
            }
            .addItem(
                id = R.id.HaritadanSilme,
                title = "Haritadan Sil",
                iconRes = R.drawable.ic_small_close,
                textColor = redColor,
                iconTint = redColor,
                isVisible = isMyCat
            ) {
                showDeleteCatConfirmationDialog()
            }
            .addItem(
                id = 3,
                title = "Kediyi Bildir",
                iconRes = R.drawable.ic_error_outline,
                textColor = redColor, iconTint = redColor,
                isVisible = !isMyCat
            ) {
                NavigationHelper.showReportBottomSheet(
                    childFragmentManager,
                    currentCat.id,
                    reportType = ReportType.CAT
                )
            }
            .build()
            .show(anchorView = view)
    }

    private fun showAddCatConfirmationDialog(currentCat: Kediler, likeCount: Int) {
        CatMapDialog.build()
            .setTitle("Gönderilerine Eklensin mi?")
            .setMessage("${currentCat.isim} isimli dostumuzu gönderilerine eklemek istiyor musun?")
            .setPositiveButton("Evet, Ekle") {
                val newPost = Post(
                    catId = currentCat.id,
                    catName = currentCat.isim,
                    bio = currentCat.hakkindasi,
                    photoUrlList = currentCat.urLler,
                    date = currentCat.createdAt.toFirebaseTimestamp(),
                    likeCount = likeCount.toLong(),
                    city = currentCat.city,
                    district = currentCat.district,
                    neighborhood = currentCat.neighborhood
                )
                viewModel.addCatToUserPosts(newPost)
                dismiss()
            }
            .setNegativeButton("Vazgeç")
            .show(childFragmentManager, "AddCatDialog")
    }

    private fun showDeleteCatConfirmationDialog() {
        CatMapDialog.build()
            .setTitle("Kediyi Haritadan Sil?")
            .setMessage("Bu dostumuzu haritadan silmek istediğine emin misin? Bu işlem kediye ait tüm gönderileri de silecektir.")
            .setPositiveButton("Evet, Sil") {
                viewModel.deleteCatFromUserAndMap()
            }
            .setNegativeButton("İptal")
            .show(childFragmentManager, "DeleteCatDialog")
    }

    private fun yukleyenProfilineGit(kediYukleyenID: String?) {
        if (!kediYukleyenID.isNullOrEmpty()) {
            dismiss()
            NavigationHelper.navigateToProfile(kediYukleyenID)
        }
    }

    override fun onStart() {
        super.onStart()

        val dialog = dialog as? BottomSheetDialog ?: return
        val bottomSheet = dialog.findViewById<android.widget.FrameLayout>(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return

        bottomSheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

        val behavior = BottomSheetBehavior.from(bottomSheet)
        behavior.apply {
            state = BottomSheetBehavior.STATE_EXPANDED
            skipCollapsed = true
            isFitToContents = false
            expandedOffset = 0
        }
    }

    private fun setupCommentCountObserver() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                commentsViewModel.commentCount.collectLatest { sayi ->
                    binding.yorumSayisiTextView.clearAnimation()
                    binding.yorumSayisiTextView.setTextColor(Color.WHITE)
                    binding.yorumSayisiTextView.text = "$sayi Yorum"
                }
            }
        }
    }

    private fun yorumSayisiToplam() {
        binding.yorumSayisiTextView.text = "Yükleniyor..."
        binding.yorumSayisiTextView.setTextColor(Color.parseColor("#333333"))

        val fadeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.animasyonlu_yukleniyor)
        binding.yorumSayisiTextView.startAnimation(fadeAnim)

        commentsViewModel.fetchCommentCount()
    }

    private fun setupPhotoIndicator(photoCount: Int) {
        binding.fotoDotsContainer.removeAllViews()
        photoIndicatorDots.clear()

        if (photoCount <= 1) {
            binding.fotoIndicatorCapsule.visibility = View.GONE
            return
        }

        binding.fotoIndicatorCapsule.visibility = View.VISIBLE

        repeat(photoCount) { index ->
            val dot = View(requireContext())
            val selected = index == 0

            val size = dpToPx(if (selected) 8 else 6)
            val margin = dpToPx(3)

            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply {
                setMargins(margin, 0, margin, 0)
            }

            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (selected) {
                    R.drawable.dot_active
                } else {
                    R.drawable.dot_inactive
                }
            )

            binding.fotoDotsContainer.addView(dot)
            photoIndicatorDots.add(dot)
        }
    }

    private fun updatePhotoIndicator(selectedPosition: Int) {
        photoIndicatorDots.forEachIndexed { index, dot ->
            val selected = index == selectedPosition
            val size = dpToPx(if (selected) 8 else 6)

            val params = dot.layoutParams as LinearLayout.LayoutParams
            params.width = size
            params.height = size
            dot.layoutParams = params

            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (selected) {
                    R.drawable.dot_active
                } else {
                    R.drawable.dot_inactive
                }
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    private fun loadStart() {
        binding.loadingContainer.isVisible = true
        binding.nested.isVisible = false
    }

    private fun contentShow() {
        binding.loadingContainer.isVisible = false
        binding.nested.isVisible = true
    }

    companion object {
        const val TAG = "BottomSheetFragment"
        private const val ARG_CAT = "arg_cat"

        fun newInstance(cat: Kediler): BottomSheetFragment {
            val fragment = BottomSheetFragment()
            val args = Bundle().apply {
                putSerializable(ARG_CAT, cat)
            }
            fragment.arguments = args
            return fragment
        }
    }
}