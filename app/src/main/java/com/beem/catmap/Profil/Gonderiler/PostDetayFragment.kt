package com.beem.catmap.Profil.Gonderiler

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback
import com.beem.catmap.Maps.FotoYuklemeListener
import com.beem.catmap.Maps.MapViewModel
import com.beem.catmap.R
import com.beem.catmap.UyariMesaji
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.gonderi.PostViewModel
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.manager.CatEventBus
import com.beem.catmap.ui.manager.CatMapEvent
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import kotlinx.coroutines.launch

class GonderiDetayFragment : Fragment() {

    private lateinit var uyari: UyariMesaji

    private val mapViewModel: MapViewModel by activityViewModels()

    // DÜZELTİLDİ: Profil ve Detay ekranının aynı ViewModel'i paylaşması için activityViewModels kullanıldı
    private val postViewModel: PostViewModel by activityViewModels()

    private var kediid: String? = null
    private var yukleyenId: String? = null

    private var photoPager: ViewPager2? = null
    private var photoDotsContainer: LinearLayout? = null
    private var photoIndicatorCapsule: MaterialCardView? = null
    private var progressBar: ProgressBar? = null

    private val photoIndicatorDots: MutableList<View?> = ArrayList()
    private var photoPageChangeCallback: OnPageChangeCallback? = null

    private lateinit var kediAdiText: TextView
    private lateinit var aciklamaText: TextView
    private lateinit var begeniBilgiTextView: TextView
    private lateinit var gonderiMenu: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            kediid = it.getString(ARG_KEDIID)
            yukleyenId = it.getString(ARG_YUKLEYEN_ID)
        }
        uyari = UyariMesaji(requireContext(), true)
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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.herbi_gonderi_icin, container, false)

        kediAdiText = view.findViewById(R.id.kediAdiText)
        aciklamaText = view.findViewById(R.id.kediAciklama)
        begeniBilgiTextView = view.findViewById(R.id.begeniBilgiTextView)
        gonderiMenu = view.findViewById(R.id.GonderiMenu)

        photoPager = view.findViewById(R.id.fotoPager)
        photoDotsContainer = view.findViewById(R.id.fotoDotsContainer)
        photoIndicatorCapsule = view.findViewById(R.id.fotoIndicatorCapsule)

        // Varsa layout dosyanızdaki ProgressBar ID'si
        progressBar = view.findViewById(R.id.progressBar)

        val currentUserManager = CurrentUserManager.getInstance(requireContext())

        // DÜZELTİLDİ: Yükleyen ID ataması erken aşamada yapıldı
        yukleyenId?.let { id ->
            postViewModel.setYukleyenID(id)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postViewModel.yukleyenID.collect { currentYukleyenId ->
                    val isMyPost = (currentYukleyenId == currentUserManager.getCurrentUser()?.id)
                    if (isMyPost) {
                        gonderiMenu.visibility = View.VISIBLE
                        gonderiMenu.setOnClickListener { v ->
                            val popupMenu = PopupMenu(requireContext(), v)
                            popupMenu.menuInflater.inflate(R.menu.gonderi_uc_nokta, popupMenu.menu)
                            popupMenu.setOnMenuItemClickListener { item ->
                                when (item.itemId) {
                                    R.id.gonderi_sil -> {
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Silme")
                                            .setMessage("Bu gönderiyi silmek istiyor musunuz?")
                                            .setPositiveButton("Evet") { _, _ ->
                                                kediid?.let { id ->
                                                    postViewModel.gonderiSil(currentYukleyenId, id)
                                                }
                                                SmartNavigationEngine.navigateBack()
                                                popupMenu.dismiss()
                                            }
                                            .setNegativeButton("Hayır") { dialog, _ -> dialog.dismiss() }
                                            .show()
                                        true
                                    }

                                    R.id.gonderiharita_sil -> {
                                        AlertDialog.Builder(requireContext())
                                            .setTitle("Silme")
                                            .setMessage("Kediyi haritadan silmek istiyor musunuz? Bu işlemi yaptığınızda, kediye ait gönderiler de silinecektir.")
                                            .setPositiveButton("Evet") { _, _ ->
                                                kediid?.let { id ->
                                                    postViewModel.haritadanVeGonderilerdenSil(currentYukleyenId, id)
                                                }
                                                popupMenu.dismiss()
                                            }
                                            .setNegativeButton("Hayır") { dialog, _ -> dialog.dismiss() }
                                            .show()
                                        true
                                    }

                                    else -> false
                                }
                            }
                            popupMenu.show()
                        }
                    } else {
                        gonderiMenu.visibility = View.GONE
                    }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postViewModel.haritaSilindiEvent.collect { silindi ->
                    if (silindi) {
                        kediid?.let { id ->
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
            if (!kediid.isNullOrBlank()) {
                SmartNavigationEngine.navigateTo(Screen.MAP)
                mapViewModel.requestZoomToCat(kediid!!)
            }
        }

        // Tıklanan kediID üzerinden detay verileri Repository'den (önce cache, yoksa Firestore) çekiliyor
        kediid?.let { id ->
            progressBar?.visibility = View.VISIBLE
            postViewModel.gonderiDetayiGetir(id) { gonderi ->
                progressBar?.visibility = View.GONE
                if (gonderi != null) {
                    populateUi(gonderi)
                }
            }
        }
    }

    private fun populateUi(gonderi: Gonderi) {
        kediAdiText.text = gonderi.kediAdi ?: ""
        if (gonderi.aciklama.isNullOrBlank()) {
            aciklamaText.text = "Bu sevimli dostumuz için henüz bir açıklama eklenmemiş. 🐾"
        } else {
            aciklamaText.text = gonderi.aciklama
        }

        val begeni = gonderi.begeniSayisi ?: 0L
        if (begeni != 0L) {
            begeniBilgiTextView.text = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeni)
        } else {
            begeniBilgiTextView.text = "Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!"
        }

        val fotoListesi = ArrayList(gonderi.fotoUrlListesi ?: emptyList())
        photoPager?.adapter = FotoAdapter(fotoListesi, object : FotoYuklemeListener {
            override fun onTumFotograflarYuklendi() {}
        })

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