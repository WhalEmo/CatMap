package com.beem.catmap.Maps.markersclick

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
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.net.toUri
import androidx.core.view.isVisible
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.beem.catmap.Maps.FotoGeciciAdapter
import com.beem.catmap.Maps.MapKedi.Kediler
import com.beem.catmap.Maps.markersclick.comments.CommentViewModel
import com.beem.catmap.Maps.markersclick.comments.CommentsBottomSheetFragment
import com.beem.catmap.R
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.navigation.NavigationHelper
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
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
    private lateinit var patiYorumButton: ImageButton
    private lateinit var profilAlani: View
    private lateinit var yukleyenAdiText: TextView
    private lateinit var yukleyenPP: ImageView

    private lateinit var fotoAdapter: FotoGeciciAdapter
    private val viewModel: CatDetailViewModel by viewModels()
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

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        observeViewModel()

        @Suppress("DEPRECATION")
        val cat = arguments?.getSerializable(ARG_CAT) as? Kediler

        cat?.let { kedi ->
            viewModel.setCatData(kedi)

            kedi.id?.let { catId ->
                commentsViewModel.initCatId(catId)
            }

            kedi.yukleyenId?.let { ownerId ->
                viewModel.loadOwnerInfo(ownerId)
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
            viewModel.toggleLike()
        }

        gonderiEkleButton.setOnClickListener { anchorView ->
            showOptionMenu(anchorView)
        }
    }

    private fun initViews(view: View) {
        isim = view.findViewById(R.id.isimgosterme)
        hakkinda = view.findViewById(R.id.hakkindagosterme)
        fotoPager = view.findViewById(R.id.fotoPager)
        kalpImageView = view.findViewById(R.id.kalpImageView)
        begeniSayisiTextView = view.findViewById(R.id.begeniSayisiTextView)
        gonderiEkleButton = view.findViewById(R.id.GonderiEkleButton)
        yorumSayisiTextView = view.findViewById(R.id.yorumSayisiTextView)
        patiYorumButton = view.findViewById(R.id.imageButton)
        profilAlani = view.findViewById(R.id.profilAlani)
        yukleyenAdiText = view.findViewById(R.id.yukleyenAdiText)
        yukleyenPP = view.findViewById(R.id.YukprofilFotoImageView)

        fotoAdapter = FotoGeciciAdapter(requireContext(), null)
        fotoPager.adapter = fotoAdapter
        fotoPager.offscreenPageLimit = 1
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.selectedCat.collectLatest { cat ->
                cat?.let {
                    isim.text = it.isim
                    hakkinda.text = it.hakkindasi

                    if (!it.urLler.isNullOrEmpty()) {
                        val uriList = withContext(Dispatchers.IO) {
                            it.urLler.mapNotNull { url -> url.toUri() }
                        }
                        fotoAdapter.submitList(uriList)
                    } else {
                        fotoAdapter.submitList(emptyList())
                    }

                    yorumSayisiToplam()
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
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.kediyi_gosterme_uc_nokta, popupMenu.menu)

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.gonderi_ekle -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Ekleme")
                        .setMessage("Bu kediyi gönderilerinize eklemek istiyor musunuz?")
                        .setPositiveButton("Evet") { _, _ ->
                            UiMessageManager.emitMessage(UiMessageState.Info("İşlem gerçekleştiriliyor..."))
                        }
                        .setNegativeButton("Hayır", null)
                        .show()
                    true
                }
                R.id.HaritadanSilme -> {
                    AlertDialog.Builder(requireContext())
                        .setTitle("Silme")
                        .setMessage("Kediyi haritadan silmek istiyor musunuz? Bu işlem kediye ait gönderileri de silecektir.")
                        .setPositiveButton("Evet") { _, _ ->
                            viewModel.deleteCatFromUserAndMap()
                        }
                        .setNegativeButton("Hayır", null)
                        .show()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }

    private fun yukleyenProfilineGit(kediYukleyenID: String?) {
        if (!kediYukleyenID.isNullOrEmpty()) {
            dismiss()
            NavigationHelper.navigateToProfile(kediYukleyenID)
        }
    }
    /*
    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState) as BottomSheetDialog

        dialog.setOnShowListener { dialogInterface ->
            val bottomSheetDialog = dialogInterface as BottomSheetDialog
            val bottomSheet = bottomSheetDialog.findViewById<FrameLayout>(
                com.google.android.material.R.id.design_bottom_sheet
            )

            bottomSheet?.let { sheet ->
                val behavior = BottomSheetBehavior.from(sheet)

                sheet.layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT

                behavior.skipCollapsed = true
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return dialog
    }

     */
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

    private fun yorumSayisiToplam() {
        val currentCatId = viewModel.selectedCat.value?.id ?: return


        yorumSayisiTextView.text = "Yükleniyor..."
        yorumSayisiTextView.setTextColor(Color.parseColor("#333333"))

        val fadeAnim = AnimationUtils.loadAnimation(requireContext(), R.anim.animasyonlu_yukleniyor)
        yorumSayisiTextView.startAnimation(fadeAnim)


        commentsViewModel.fetchCommentCount()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                commentsViewModel.commentCount.collectLatest { sayi ->
                    yorumSayisiTextView.setTextColor(Color.BLACK)
                    yorumSayisiTextView.text = "$sayi Yorum"
                }
            }
        }
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