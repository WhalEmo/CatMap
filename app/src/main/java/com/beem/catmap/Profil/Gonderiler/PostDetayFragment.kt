package com.beem.catmap.Profil.Gonderiler
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.core.view.WindowInsetsControllerCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.viewpager2.widget.ViewPager2
import com.beem.catmap.Maps.FotoYuklemeListener
import com.beem.catmap.Maps.MapViewModel
import com.beem.catmap.Maps.MapsActivity
import com.beem.catmap.R
import com.beem.catmap.UyariMesaji
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.gonderi.PostViewModel
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
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

        val viewPager: ViewPager2 = view.findViewById(R.id.fotoPager)
        val kediAdiText: TextView = view.findViewById(R.id.kediAdiText)
        val aciklamaText: TextView = view.findViewById(R.id.kediAciklama)
        val begeniBilgiTextView: TextView = view.findViewById(R.id.begeniBilgiTextView)
        val gonderiMenu: ImageView = view.findViewById(R.id.GonderiMenu)

        val currentUserManager = CurrentUserManager.getInstance(requireContext())

        viewPager.adapter = FotoAdapter(fotoListesi ?: arrayListOf(), object : FotoYuklemeListener {
            override fun onTumFotograflarYuklendi() {
            }
        })

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
                        (activity as? MapsActivity)?.sonTiklananMarkeriSil()
                        SmartNavigationEngine.navigateBack()
                    }
                }
            }
        }
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

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
}