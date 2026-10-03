package com.beem.catmap.ui.profile.post

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import coil.load
import coil.request.CachePolicy
import com.beem.catmap.R
import com.beem.catmap.WarningMessage
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.Post
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.databinding.HerbiGonderiIcinBinding
import com.beem.catmap.ui.map.MapViewModel
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.components.CatMapPopupMenu
import com.beem.catmap.ui.extensions.getFormattedTimestamp
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.beem.catmap.ui.profile.post.components.PostPhotoSlider
import com.beem.catmap.ui.report.ReportType
import com.stfalcon.imageviewer.StfalconImageViewer
import kotlinx.coroutines.launch

class PostDetailFragment : Fragment() {

    private var _binding: HerbiGonderiIcinBinding? = null
    private val binding get() = _binding!!

    private lateinit var warningMessage: WarningMessage

    private val mapViewModel: MapViewModel by activityViewModels()

    // DÜZELTİLDİ: Profil ve Detay ekranının aynı ViewModel'i paylaşması için activityViewModels kullanıldı
    private val postViewModel: PostViewModel by activityViewModels()

    private var catId: String? = null
    private var loaderId: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            catId = it.getString(ARG_KEDIID)
            loaderId = it.getString(ARG_YUKLEYEN_ID)
        }
        warningMessage = WarningMessage(requireContext(), true)
    }

    override fun onResume() {
        super.onResume()
        activity?.window?.let { window ->
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.catmap_surface_white)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = true
        }
    }

    override fun onPause() {
        super.onPause()
        activity?.window?.let { window ->
            window.statusBarColor = ContextCompat.getColor(requireContext(), R.color.catmap_background)
            val controller = WindowInsetsControllerCompat(window, window.decorView)
            controller.isAppearanceLightStatusBars = true
        }
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = HerbiGonderiIcinBinding.inflate(inflater, container, false)

        val currentUserManager = CurrentUserManager.getInstance(requireContext())

        // DÜZELTİLDİ: Yükleyen ID ataması erken aşamada yapıldı
        loaderId?.let { id ->
            postViewModel.setYukleyenID(id)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postViewModel.loaderId.collect { currentYukleyenId ->
                    val isMyPost = (currentYukleyenId == currentUserManager.getCurrentUser().id)
                    binding.GonderiMenu.isVisible = isMyPost
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postViewModel.mapsDeleteEvent.collect { silindi ->
                    if (silindi) {
                        catId?.let { id ->
                            CatEventBus.emitEvent(CatMapEvent.Deleted(catId = id))
                        }
                        SmartNavigationEngine.navigateBack()
                    }
                }
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupBottomBarPadding()
        handleBackPressWithEngine()


        binding.toolbar.setNavigationOnClickListener {
            SmartNavigationEngine.navigateBack()
        }

        binding.haritadaGorButon.setOnClickListener {
            if (!catId.isNullOrBlank()) {
                SmartNavigationEngine.navigateTo(Screen.MAP)
                mapViewModel.requestZoomToCat(catId!!)
            }
        }

        // Tıklanan kediID üzerinden detay verileri Repository'den (önce cache, yoksa Firestore) çekiliyor
        catId?.let { id ->
            // Layout dosyanızda varsa progressBar erişimi (kullanılmıyorsa güvenli kalır)
            postViewModel.getPostDetail(id) { gonderi ->
                if (gonderi != null) {
                    populateUi(gonderi)
                }
            }
        }

        binding.GonderiMenu.setOnClickListener { v ->
            showPostOptionMenu(v)
        }

    }


    private fun setupBottomBarPadding() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { view, windowInsets ->
            val systemBars = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(0, 0, 0, systemBars.bottom)
            windowInsets
        }
    }

    private fun populateUi(post: Post) {
        binding.kediAdiText.text = post.catName ?: ""

        binding.kediAciklama.text = post.bio?.trim()?.takeIf { it.isNotBlank() }
            ?: "Bu sevimli dostumuz için henüz bir açıklama eklenmemiş. 🐾"

        binding.gonderiTarihiText.text = getFormattedTimestamp(post.date) ?: ""
        val begeni = post.likeCount ?: 0L
        if (begeni != 0L) {
            binding.begeniBilgiTextView.text = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeni)
        } else {
            binding.begeniBilgiTextView.text = "Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!"
        }

        val locationParts = listOfNotNull(
            post.city?.takeIf { it.isNotBlank() },
            post.district?.takeIf { it.isNotBlank() },
            post.neighborhood?.takeIf { it.isNotBlank() }
        )

        if (locationParts.isNotEmpty()) {
            binding.konumChip.isVisible = true
            binding.konumText.text = locationParts.joinToString(" • ")
        } else {
            binding.konumChip.isVisible = false
        }

        val fotoListesi = ArrayList(post.photoUrlList ?: emptyList())

        binding.postPhotoPagerCompose.setContent {
            PostPhotoSlider(
                photos = fotoListesi,
                onPhotoClick = { position ->
                    openFullScreenViewer(fotoListesi, position)
                }
            )
        }
    }

    private fun openFullScreenViewer(urls: List<String>, startPosition: Int) {
        if (urls.isEmpty()) return
        StfalconImageViewer.Builder<String>(requireContext(), urls) { imageView, url ->
            imageView.load(url) {
                crossfade(true)
                memoryCachePolicy(CachePolicy.ENABLED)
                diskCachePolicy(CachePolicy.ENABLED)
            }
        }
            .withStartPosition(startPosition)
            .withHiddenStatusBar(false)
            .allowSwipeToDismiss(true)
            .show()
    }

    private fun showPostOptionMenu(anchorView: View) {
        val currentYukleyenId = postViewModel.loaderId.value
        val isMyPost = UserSession.userId == currentYukleyenId
        val redColor = ContextCompat.getColor(requireContext(), R.color.catmap_error)

        CatMapPopupMenu.Builder(requireContext())
            .addItem(
                id = R.id.gonderi_sil,
                title = "Gönderiyi Sil",
                iconRes = R.drawable.ic_small_close,
                textColor = redColor,
                iconTint = redColor,
                isVisible = isMyPost
            ) {
                showDeletePostConfirmationDialog(currentYukleyenId)
            }

            .addItem(
                id = R.id.gonderiharita_sil,
                title = "Kediyi Haritadan Sil",
                iconRes = R.drawable.ic_small_close,
                textColor = redColor,
                iconTint = redColor,
                isVisible = isMyPost
            ) {
                showDeleteCatFromMapConfirmationDialog(currentYukleyenId)
            }
            .addItem(
                id = 3,
                title = "Gönderiyi Bildir",
                iconRes = R.drawable.ic_error_outline,
                textColor = redColor, iconTint = redColor,
                isVisible = !isMyPost
            ) {
                catId?.let { catId ->
                    NavigationHelper.showReportBottomSheet(
                        childFragmentManager,
                        catId,
                        reportType = ReportType.POST
                    )
                }
            }
            .build()
            .show(anchorView = anchorView)
    }

    private fun showDeletePostConfirmationDialog(currentYukleyenId: String) {
        CatMapDialog.build()
            .setTitle("Gönderi Silinsin mi?")
            .setMessage("Bu gönderiyi silmek istediğine emin misin? Bu işlem geri alınamaz.")
            .setPositiveButton("Evet, Sil") {
                catId?.let { id ->
                    postViewModel.postDelete(currentYukleyenId, id)
                }
                SmartNavigationEngine.navigateBack()
            }
            .setNegativeButton("Vazgeç")
            .show(childFragmentManager, "DeletePostDialog")
    }

    private fun showDeleteCatFromMapConfirmationDialog(currentYukleyenId: String) {
        CatMapDialog.build()
            .setTitle("Kediyi Haritadan Sil?")
            .setMessage("Kediyi haritadan silmek istediğine emin misin? Bu işlem kediye ait tüm gönderileri de silebilir.")
            .setPositiveButton("Evet, Sil") {
                catId?.let { id ->
                    postViewModel.mapAndPostDeleteCat(currentYukleyenId, id)
                }
            }
            .setNegativeButton("İptal")
            .show(childFragmentManager, "DeleteCatFromMapDialog")
    }

    companion object {
        private const val ARG_KEDIID = "kediid"
        private const val ARG_YUKLEYEN_ID = "yukleyenId"

        @JvmStatic
        fun newBundle(
            kediid: String,
            yukleyenId: String?
        ): Bundle {
            return bundleOf(
                ARG_KEDIID to kediid,
                ARG_YUKLEYEN_ID to yukleyenId
            )
        }
    }

    override fun onDestroyView() {
        _binding = null

        super.onDestroyView()
    }
}