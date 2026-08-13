package com.beem.catmap.ui.profile.post

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.beem.catmap.maps.MapViewModel
import com.beem.catmap.R
import com.beem.catmap.WarningMessage
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.data.model.Post
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.components.CatMapPopupMenu
import com.beem.catmap.ui.extensions.getFormattedTimestamp
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class PostDetailFragment : Fragment() {

    private lateinit var WarningMessage: WarningMessage

    private val mapViewModel: MapViewModel by activityViewModels()

    // DÜZELTİLDİ: Profil ve Detay ekranının aynı ViewModel'i paylaşması için activityViewModels kullanıldı
    private val postViewModel: PostViewModel by activityViewModels()

    private var catId: String? = null
    private var loaderId: String? = null

    private var photoPager: ViewPager2? = null
    private var photoDotsContainer: LinearLayout? = null
    private var photoIndicatorCapsule: MaterialCardView? = null
    private var progressBar: ProgressBar? = null

    private val photoIndicatorDots: MutableList<View?> = ArrayList()
    private var photoPageChangeCallback: OnPageChangeCallback? = null

    private lateinit var catNameText: TextView
    private lateinit var bioText: TextView
    private lateinit var likeInfoTextView: TextView
    private lateinit var postMenu: ImageView
    private lateinit var postDateText: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            catId = it.getString(ARG_KEDIID)
            loaderId = it.getString(ARG_YUKLEYEN_ID)
        }
        WarningMessage = WarningMessage(requireContext(), true)
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
    ): View? {
        val view = inflater.inflate(R.layout.herbi_gonderi_icin, container, false)

        catNameText = view.findViewById(R.id.kediAdiText)
        bioText = view.findViewById(R.id.kediAciklama)
        likeInfoTextView = view.findViewById(R.id.begeniBilgiTextView)
        postMenu = view.findViewById(R.id.GonderiMenu)

        photoPager = view.findViewById(R.id.fotoPager)
        photoDotsContainer = view.findViewById(R.id.fotoDotsContainer)
        photoIndicatorCapsule = view.findViewById(R.id.fotoIndicatorCapsule)
        postDateText = view.findViewById(R.id.gonderiTarihiText)

        // Varsa layout dosyanızdaki ProgressBar ID'si
        progressBar = view.findViewById(R.id.progressBar)

        val currentUserManager = CurrentUserManager.getInstance(requireContext())

        // DÜZELTİLDİ: Yükleyen ID ataması erken aşamada yapıldı
        loaderId?.let { id ->
            postViewModel.setYukleyenID(id)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postViewModel.loaderId.collect { currentYukleyenId ->
                    val isMyPost = (currentYukleyenId == currentUserManager.getCurrentUser().id)
                    postMenu.isVisible = isMyPost
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
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleBackPressWithEngine()

        val haritadaGorButton: MaterialButton = view.findViewById(R.id.haritadaGorButon)
        val toolbar: MaterialToolbar = view.findViewById(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            SmartNavigationEngine.navigateBack()
        }

        haritadaGorButton.setOnClickListener {
            if (!catId.isNullOrBlank()) {
                SmartNavigationEngine.navigateTo(Screen.MAP)
                mapViewModel.requestZoomToCat(catId!!)
            }
        }

        // Tıklanan kediID üzerinden detay verileri Repository'den (önce cache, yoksa Firestore) çekiliyor
        catId?.let { id ->
            progressBar?.visibility = View.VISIBLE
            postViewModel.getPostDetail(id) { gonderi ->
                progressBar?.visibility = View.GONE
                if (gonderi != null) {
                    populateUi(gonderi)
                }
            }
        }

        postMenu.setOnClickListener { v ->
            showPostOptionMenu(v)
        }
    }

    private fun populateUi(post: Post) {
        catNameText.text = post.catName ?: ""
        if (post.bio.isNullOrBlank()) {
            bioText.text = "Bu sevimli dostumuz için henüz bir açıklama eklenmemiş. 🐾"
        } else {
            bioText.text = post.bio
        }
        postDateText.text = getFormattedTimestamp(post.date) ?: ""
        val begeni = post.likeCount ?: 0L
        if (begeni != 0L) {
            likeInfoTextView.text = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeni)
        } else {
            likeInfoTextView.text = "Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!"
        }

        val fotoListesi = ArrayList(post.photoUrlList ?: emptyList())
        photoPager?.adapter = PhotoAdapter(fotoListesi)

        setupPhotoIndicator(fotoListesi.size)

        // DÜZELTİLDİ: Eski callback varsa temizlenir
        if (photoPageChangeCallback != null) {
            photoPager?.unregisterOnPageChangeCallback(photoPageChangeCallback!!)
        }

        photoPageChangeCallback = object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePhotoIndicator(position)
            }
        }
        photoPager?.registerOnPageChangeCallback(photoPageChangeCallback!!)
    }

    private fun showPostOptionMenu(anchorView: View) {
        val currentYukleyenId = postViewModel.loaderId.value
        val redColor = ContextCompat.getColor(requireContext(), R.color.catmap_error)

        CatMapPopupMenu.Builder(requireContext())
            .addItem(
                id = R.id.gonderi_sil,
                title = "Gönderiyi Sil",
                iconRes = R.drawable.ic_small_close,
                textColor = redColor,
                iconTint = redColor
            ) {
                showDeletePostConfirmationDialog(currentYukleyenId)
            }

            .addItem(
                id = R.id.gonderiharita_sil,
                title = "Kediyi Haritadan Sil",
                iconRes = R.drawable.ic_small_close,
                textColor = redColor,
                iconTint = redColor
            ) {
                showDeleteCatFromMapConfirmationDialog(currentYukleyenId)
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
        if (photoPager != null && photoPageChangeCallback != null) {
            photoPager!!.unregisterOnPageChangeCallback(photoPageChangeCallback!!)
        }

        photoPager = null
        photoDotsContainer = null
        photoIndicatorCapsule = null
        photoPageChangeCallback = null
        progressBar = null

        photoIndicatorDots.clear()

        super.onDestroyView()
    }

    private fun setupPhotoIndicator(photoCount: Int) {
        photoDotsContainer?.removeAllViews()
        photoIndicatorDots.clear()

        if (photoCount <= 1) {
            photoIndicatorCapsule?.visibility = View.GONE
            return
        }

        photoIndicatorCapsule?.visibility = View.VISIBLE

        for (i in 0 until photoCount) {
            val dot = View(requireContext())
            val isSelected = i == 0
            val dotSize = dpToPx(if (isSelected) 8 else 6)
            val dotMargin = dpToPx(3)

            val layoutParams = LinearLayout.LayoutParams(dotSize, dotSize)
            layoutParams.setMargins(dotMargin, 0, dotMargin, 0)
            dot.layoutParams = layoutParams

            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (isSelected) R.drawable.dot_active else R.drawable.dot_inactive
            )

            photoDotsContainer?.addView(dot)
            photoIndicatorDots.add(dot)
        }
    }

    private fun updatePhotoIndicator(selectedPosition: Int) {
        for (i in photoIndicatorDots.indices) {
            val dot: View = photoIndicatorDots[i] ?: continue
            val isSelected = i == selectedPosition
            val dotSize = dpToPx(if (isSelected) 8 else 6)

            val layoutParams = dot.layoutParams as LinearLayout.LayoutParams
            layoutParams.width = dotSize
            layoutParams.height = dotSize
            dot.layoutParams = layoutParams

            dot.background = ContextCompat.getDrawable(
                requireContext(),
                if (isSelected) R.drawable.dot_active else R.drawable.dot_inactive
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return Math.round(dp * resources.displayMetrics.density)
    }
}