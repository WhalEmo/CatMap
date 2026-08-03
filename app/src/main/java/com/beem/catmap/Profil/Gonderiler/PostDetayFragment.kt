package com.beem.catmap.Profil.Gonderiler


import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
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
    private val postViewModel: PostViewModel by activityViewModels()

    private var fotoListesi: ArrayList<String>? = null
    private var kediAdi: String? = null
    private var aciklama: String? = null
    private var begeni: Long = 0L
    private var kediid: String? = null

    private var photoPager: ViewPager2? = null
    private var photoDotsContainer: LinearLayout? = null
    private var photoIndicatorCapsule: MaterialCardView? = null

    private val photoIndicatorDots: MutableList<View?> = ArrayList<View?>()

    private var photoPageChangeCallback: OnPageChangeCallback? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            fotoListesi = it.getStringArrayList(ARG_FOTO_LIST)
            kediAdi = it.getString(ARG_KEDI_ADI)
            aciklama = it.getString(ARG_ACIKLAMA)
            begeni = it.getLong(ARG_BEGENİ, 0L)
            kediid = it.getString(ARG_KEDIID)
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
            controller.isAppearanceLightStatusBars = false
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.herbi_gonderi_icin, container, false)

        val kediAdiText: TextView = view.findViewById(R.id.kediAdiText)
        val aciklamaText: TextView = view.findViewById(R.id.kediAciklama)
        val begeniBilgiTextView: TextView = view.findViewById(R.id.begeniBilgiTextView)
        val gonderiMenu: ImageView = view.findViewById(R.id.GonderiMenu)


        photoPager = view.findViewById(R.id.fotoPager);
        photoDotsContainer = view.findViewById(R.id.fotoDotsContainer);
        photoIndicatorCapsule = view.findViewById(R.id.fotoIndicatorCapsule);

        val currentUserManager = CurrentUserManager.getInstance(requireContext())


        kediAdiText.text = kediAdi
        if (aciklama.isNullOrBlank()) {
            aciklamaText.text = "Bu sevimli dostumuz için henüz bir açıklama eklenmemiş. 🐾"
        } else {
            aciklamaText.text = aciklama
        }

        if (begeni != 0L) {
            val bilgi = String.format("Bu kediyi %d kişi beğendi. Sen de beğenmek istersen haritada göre bas!", begeni)
            begeniBilgiTextView.text = bilgi
        } else {
            begeniBilgiTextView.text = "Bu kediyi henüz kimse beğenmedi. Beğenmek istersen haritada göre bas!"
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                postViewModel.yukleyenID.collect { yukleyenId ->
                    val isMyPost = (yukleyenId == currentUserManager.getCurrentUser()?.id)
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
                                                    postViewModel.gonderiSil(yukleyenId, id)
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
                                                    postViewModel.haritadanVeGonderilerdenSil(yukleyenId, id)
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


        val safePhotoList = fotoListesi ?: arrayListOf()

        photoPager!!.setAdapter(FotoAdapter(fotoListesi, object : FotoYuklemeListener {
            override fun onTumFotograflarYuklendi() {
            }
        }))

        setupPhotoIndicator(safePhotoList.size)

        photoPageChangeCallback = object : OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                updatePhotoIndicator(position)
            }
        }

        photoPager!!.registerOnPageChangeCallback(photoPageChangeCallback!!)

    }

    companion object {
        private const val ARG_FOTO_LIST = "fotoListesi"
        private const val ARG_KEDI_ADI = "kediAdi"
        private const val ARG_ACIKLAMA = "aciklama"
        private const val ARG_BEGENİ = "begeni"
        private const val ARG_KEDIID = "kediid"

        @JvmStatic
        fun newBundle(
            fotoListesi: ArrayList<String>,
            kediAdi: String,
            aciklama: String,
            begeni: Long?,
            kediid: String
        ): Bundle {
            return bundleOf(
                ARG_FOTO_LIST to fotoListesi,
                ARG_KEDI_ADI to kediAdi,
                ARG_ACIKLAMA to aciklama,
                ARG_BEGENİ to (begeni ?: 0L),
                ARG_KEDIID to kediid
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

        photoIndicatorDots.clear()

        super.onDestroyView()
    }

    private fun setupPhotoIndicator(photoCount: Int) {
        photoDotsContainer!!.removeAllViews()
        photoIndicatorDots.clear()

        if (photoCount <= 1) {
            photoIndicatorCapsule!!.setVisibility(View.GONE)
            return
        }

        photoIndicatorCapsule!!.setVisibility(View.VISIBLE)

        for (i in 0..<photoCount) {
            val dot = View(requireContext())

            val isSelected = i == 0

            val dotSize = dpToPx(if (isSelected) 8 else 6)
            val dotMargin = dpToPx(3)

            val layoutParams =
                LinearLayout.LayoutParams(dotSize, dotSize)

            layoutParams.setMargins(
                dotMargin,
                0,
                dotMargin,
                0
            )

            dot.setLayoutParams(layoutParams)

            dot.setBackground(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (isSelected) R.drawable.dot_active else R.drawable.dot_inactive
                )
            )

            photoDotsContainer!!.addView(dot)
            photoIndicatorDots.add(dot)
        }
    }

    private fun updatePhotoIndicator(selectedPosition: Int) {
        for (i in photoIndicatorDots.indices) {
            val dot: View = photoIndicatorDots.get(i)!!

            val isSelected = i == selectedPosition

            val dotSize = dpToPx(if (isSelected) 8 else 6)

            val layoutParams =
                dot.getLayoutParams() as LinearLayout.LayoutParams

            layoutParams.width = dotSize
            layoutParams.height = dotSize

            dot.setLayoutParams(layoutParams)

            dot.setBackground(
                ContextCompat.getDrawable(
                    requireContext(),
                    if (isSelected)
                        R.drawable.dot_active
                    else
                        R.drawable.dot_inactive
                )
            )
        }
    }

    private fun dpToPx(dp: Int): Int {
        return Math.round(
            dp * getResources().getDisplayMetrics().density
        )
    }
}