package com.beem.catmap.ui.markersclick

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.beem.catmap.Maps.FotoGeciciAdapter
import com.beem.catmap.Maps.mapkedi.Kediler
import com.beem.catmap.commentreply.CommentViewModel
import com.beem.catmap.commentreply.CommentsBottomSheetFragment
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.components.CatMapPopupMenu
import com.beem.catmap.ui.extensions.getFormattedDate
import com.beem.catmap.ui.extensions.kalpAnimasyonuYap
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.utils.toFirebaseTimestamp
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BottomSheetFragment : BottomSheetDialogFragment() {

    private lateinit var isim: TextView
    private lateinit var hakkinda: TextView
    private lateinit var fotoPager: ViewPager2
    private lateinit var kalpImageView: ImageView
    private lateinit var begeniSayisiTextView: TextView
    private lateinit var gonderiEkleButton: View
    private lateinit var yorumSayisiTextView: TextView
    private lateinit var patiYorumButton: MaterialCardView
    private lateinit var profilAlani: View
    private lateinit var yukleyenAdiText: TextView
    private lateinit var yukleyenPP: ImageView
    private lateinit var tarihText: TextView

    private lateinit var photoIndicatorCapsule: MaterialCardView
    private lateinit var photoDotsContainer: LinearLayout
    private lateinit var loadingContainer : FrameLayout
    private lateinit var nestedScrollViewContent: NestedScrollView

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


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.markerdaki_kediyi_gosterme, container, false)
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NORMAL, R.style.Dialog_FullWidth)
    }

    override fun onDestroyView() {
        fotoPager.unregisterOnPageChangeCallback(photoPageChangeCallback)
        photoIndicatorDots.clear()
        super.onDestroyView()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
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

            profilAlani.setOnClickListener {
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

        yorumSayisiTextView.setOnClickListener(openCommentsAction)
        patiYorumButton.setOnClickListener(openCommentsAction)

        kalpImageView.setOnClickListener {
            kalpImageView.kalpAnimasyonuYap()
            viewModel.toggleLike()
        }

        gonderiEkleButton.setOnClickListener { anchorView ->
            showOptionMenu(anchorView)
        }
    }

    private fun initViews(view: View) {
        isim = view.findViewById(R.id.isimgosterme)
        hakkinda = view.findViewById(R.id.hakkindagosterme)
        kalpImageView = view.findViewById(R.id.kalpImageView)
        begeniSayisiTextView = view.findViewById(R.id.begeniSayisiTextView)
        gonderiEkleButton = view.findViewById(R.id.GonderiEkleButton)
        yorumSayisiTextView = view.findViewById(R.id.yorumSayisiTextView)
        patiYorumButton = view.findViewById(R.id.btnYorumlar)
        profilAlani = view.findViewById(R.id.profilAlani)
        yukleyenAdiText = view.findViewById(R.id.yukleyenAdiText)
        yukleyenPP = view.findViewById(R.id.YukprofilFotoImageView)
        tarihText = view.findViewById(R.id.tarihText)

        fotoPager = view.findViewById(R.id.fotoPager)
        photoIndicatorCapsule = view.findViewById(R.id.fotoIndicatorCapsule)
        photoDotsContainer = view.findViewById(R.id.fotoDotsContainer)
        loadingContainer = view.findViewById(R.id.loadingContainer)
        nestedScrollViewContent = view.findViewById(R.id.nested)

        fotoAdapter = FotoGeciciAdapter(requireContext(), null)
        fotoPager.adapter = fotoAdapter
        fotoPager.offscreenPageLimit = 1

        fotoPager.registerOnPageChangeCallback(photoPageChangeCallback)
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCat.collectLatest { cat ->
                cat?.let {
                    isim.text = it.isim
                    if (it.hakkindasi.isNullOrBlank()) {
                        hakkinda.text = "Bu sevimli dostumuz için henüz bir açıklama eklenmemiş. 🐾"
                    } else {
                        hakkinda.text = it.hakkindasi
                    }
                    tarihText.text = getFormattedDate(it.createdAt) ?: ""

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
                    yukleyenAdiText.text = "@$username"
                    Glide.with(this@BottomSheetFragment)
                        .load(photoUrl)
                        .placeholder(R.drawable.kullanici)
                        .dontAnimate()
                        .into(yukleyenPP)

                    contentShow()
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isMyCat.collectLatest { isMyCat ->
                gonderiEkleButton.isVisible = isMyCat
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.likeCount.collectLatest { count ->
                begeniSayisiTextView.text = count.toString()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.isLiked.collectLatest { isLiked ->
                val iconRes = if (isLiked) {
                    R.drawable.baseline_favorite_24
                } else {
                    R.drawable.baseline_favorite_border_24
                }
                kalpImageView.setImageResource(iconRes)
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
                isEnabled = !isCatAdded
            ) {
                showAddCatConfirmationDialog(currentCat, likeCount)
            }
            .addItem(
                id = R.id.HaritadanSilme,
                title = "Haritadan Sil",
                iconRes = R.drawable.ic_small_close,
                textColor = redColor,
                iconTint = redColor,
                isVisible = true
            ) {
                showDeleteCatConfirmationDialog()
            }
            .build()
            .show(anchorView = view)
    }

    /**
     * 🟢 Kedi ekleme diyalog akışı (CatMapDialog)
     */
    private fun showAddCatConfirmationDialog(currentCat: Kediler, likeCount: Int) {
        CatMapDialog.build()
            .setTitle("Gönderilerine Eklensin mi?")
            .setMessage("${currentCat.isim} isimli dostumuzu gönderilerine eklemek istiyor musun?")
            .setPositiveButton("Evet, Ekle") {
                val newPost = Gonderi(
                    kediID = currentCat.id,
                    kediAdi = currentCat.isim,
                    aciklama = currentCat.hakkindasi,
                    fotoUrlListesi = currentCat.urLler,
                    tarih = currentCat.createdAt.toFirebaseTimestamp(),
                    begeniSayisi = likeCount.toLong()
                )
                viewModel.addCatToUserPosts(newPost)
                dismiss()
            }
            .setNegativeButton("Vazgeç")
            .show(childFragmentManager, "AddCatDialog")
    }

    /**
     * 🔴 Kedi silme diyalog akışı (CatMapDialog)
     */
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
        val bottomSheet = dialog.findViewById<FrameLayout>(
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
                    yorumSayisiTextView.clearAnimation()
                    yorumSayisiTextView.setTextColor(Color.WHITE)
                    yorumSayisiTextView.text = "$sayi Yorum"
                }
            }
        }
    }
    private fun yorumSayisiToplam() {
        yorumSayisiTextView.text = "Yükleniyor..."
        yorumSayisiTextView.setTextColor(Color.parseColor("#333333"))

        val fadeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.animasyonlu_yukleniyor)
        yorumSayisiTextView.startAnimation(fadeAnim)

        commentsViewModel.fetchCommentCount()
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


    private fun setupPhotoIndicator(photoCount: Int) {
        photoDotsContainer.removeAllViews()
        photoIndicatorDots.clear()

        if (photoCount <= 1) {
            photoIndicatorCapsule.visibility = View.GONE
            return
        }

        photoIndicatorCapsule.visibility = View.VISIBLE

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

            photoDotsContainer.addView(dot)
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
        loadingContainer.isVisible = true
        nestedScrollViewContent.isVisible = false
    }

    private fun contentShow() {
        loadingContainer.isVisible = false
        nestedScrollViewContent.isVisible = true
    }

}