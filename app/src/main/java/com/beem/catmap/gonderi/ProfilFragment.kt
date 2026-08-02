package com.beem.catmap.Profil

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beem.catmap.Profil.Gonderiler.GonderiAdapter
import com.beem.catmap.Profil.Gonderiler.GonderiDetayFragment
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.gonderi.FollowUiState
import com.beem.catmap.gonderi.FollowViewModel
import com.beem.catmap.gonderi.PostViewModel
import com.beem.catmap.gonderi.ProfileUpdateResult
import com.beem.catmap.gonderi.ProfileViewModel
import com.beem.catmap.gonderi.UiState
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.bumptech.glide.Glide
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private val viewModel: PostViewModel by activityViewModels()
    private val followViewModel: FollowViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()

    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressFollow: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton
    private lateinit var profiliDuzenleTiklandi: Button
    private lateinit var takipEtButonu: Button
    private lateinit var takipEdiliyorVeMesajLayout: LinearLayout
    private lateinit var takipEdiliyorButonu: Button
    private lateinit var sohbetButon: Button
    private lateinit var takipciSayisiTextView: TextView
    private lateinit var takipEdilenSayisiTextView: TextView
    private lateinit var gonderiSayisiTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var KullaniciAdi: TextView
    private lateinit var profilFotoImageView: CircleImageView
    private lateinit var gonderiAdapter: GonderiAdapter
    val myUserId = UserSession.userId
    private var targetUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetUserId = it.getString(ARG_USER_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_profil_sayfasi, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        handleBackPressWithEngine()

        recyclerView = view.findViewById(R.id.gonderiRecyclerView)
        progressBar = view.findViewById(R.id.progressBarr)
        progressFollow = view.findViewById(R.id.progressFollow)
        tvEmpty = view.findViewById(R.id.emptyTextView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        btnBack = view.findViewById(R.id.btnBack)
        profiliDuzenleTiklandi = view.findViewById(R.id.profiliDuzenleTiklandi)
        takipEtButonu = view.findViewById(R.id.takipEtButonu)
        takipEdiliyorVeMesajLayout = view.findViewById(R.id.takipEdiliyorVeMesajLayout)
        takipEdiliyorButonu = view.findViewById(R.id.takipEdiliyorButonu)
        sohbetButon = view.findViewById(R.id.sohbetButon)
        takipciSayisiTextView = view.findViewById(R.id.takipciSayisiTextView)
        takipEdilenSayisiTextView = view.findViewById(R.id.takipEdilenSayisiTextView)
        gonderiSayisiTextView = view.findViewById(R.id.gonderiSayisiTextView)
        bioTextView = view.findViewById(R.id.bioTextView)
        KullaniciAdi = view.findViewById(R.id.KullaniciAdi)
        profilFotoImageView = view.findViewById(R.id.profilFotoImageView)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        targetUserId?.let { userId ->
            // 1. Önce ViewModel'deki Cache durumunu hazırla
            viewModel.profilDurumunuHazirla(userId)
            followViewModel.profilDurumunuHazirla(userId)
            followViewModel.targetUserClearOrPrepare(userId)
            followViewModel.takipTakipciSayisiGetir(userId, false)
            profileViewModel.profilBilgileriniYukle(userId)

            // 2. YALNIZCA cache boşsa sunucudan veri çek!
            // gonderiKaydet ile cache zaten güncellendiği için forceRefresh = false ile çağırıyoruz.
            viewModel.gonderileriGetir(userId, forceRefresh = false)
            followViewModel.takipTakipciSayisiGetir(userId, forceRefresh = false)
        }
    }

    private fun setupRecyclerView() {
        gonderiAdapter = GonderiAdapter { gonderi ->
            onGonderiTiklandi(gonderi)
        }

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)

        recyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = gonderiAdapter
            setHasFixedSize(true)

            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    if (dy > 0) {
                        val visibleItemCount = gridLayoutManager.childCount
                        val totalItemCount = gridLayoutManager.itemCount
                        val firstVisibleItemPosition = gridLayoutManager.findFirstVisibleItemPosition()

                        if (!viewModel.isLoadingMore && !viewModel.isLastPage) {
                            if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount - 3) {
                                viewModel.dahaFazlaGonderiGetir()
                            }
                        }
                    }
                }
            })
        }
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            SmartNavigationEngine.navigateBack()
        }

        swipeRefreshLayout.setOnRefreshListener {
            yukleVerileri(forceRefresh = true)
        }

        takipEtButonu.setOnClickListener {
            targetUserId?.let { targetId ->
                if (targetId != myUserId) {
                    followViewModel.takipEt(
                        takipEttiginId = targetId,
                        currentUserId = myUserId
                    )
                }
            }
        }

        takipEdiliyorButonu.setOnClickListener {
            targetUserId?.let { targetId ->
                if (targetId != myUserId) {
                    followViewModel.takiptenCikar(
                        takiptenCiktiginId = targetId,
                        currentUserId = myUserId
                    )
                }
            }
        }

        sohbetButon.setOnClickListener { }

        profiliDuzenleTiklandi.setOnClickListener {
            SmartNavigationEngine.navigateTo(Screen.EDIT_PROFILE)
        }
    }

    private fun yukleVerileri(forceRefresh: Boolean = false) {
        targetUserId?.let { userId ->
            followViewModel.takipTakipciSayisiGetir(userId, forceRefresh = forceRefresh)
            viewModel.gonderileriGetir(userId, forceRefresh = forceRefresh)
        } ?: run {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Profil Güncelleme Durumu
                launch {
                    profileViewModel.profileUpdateState.collect { state ->
                        when (state) {
                            is ProfileUpdateResult.Loading -> {}
                            is ProfileUpdateResult.Success -> {
                                Toast.makeText(requireContext(), "Profil başarıyla güncellendi.", Toast.LENGTH_SHORT).show()
                            }
                            is ProfileUpdateResult.UsernameAlreadyTaken -> {
                                Toast.makeText(requireContext(), "Bu kullanıcı adı daha önce alınmış.", Toast.LENGTH_SHORT).show()
                            }
                            is ProfileUpdateResult.Error -> {
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            ProfileUpdateResult.Idle -> {}
                        }
                    }
                }


                launch {
                    followViewModel.followUiState.collectLatest { state ->
                        renderProfileButtons(state)
                    }
                }

                launch {

                    if (targetUserId == myUserId) {

                        followViewModel.profileState.collect { profileState ->
                            takipciSayisiTextView.text =
                                profileState.takipciSayisi.toString()

                            takipEdilenSayisiTextView.text =
                                profileState.takipEdilenSayisi.toString()

                            gonderiSayisiTextView.text =
                                profileState.gonderiSayisi.toString()
                        }

                    } else {

                        launch {
                            followViewModel.targetUserTakipciSayisi.collect { sayi ->
                                takipciSayisiTextView.text = sayi.toString()
                            }
                        }

                        launch {
                            followViewModel.targetUserTakipEdilenSayisi.collect { sayi ->
                                takipEdilenSayisiTextView.text = sayi.toString()
                            }
                        }

                        launch {
                            viewModel.gonderiSayisi.collect { sayi ->
                                gonderiSayisiTextView.text = sayi.toString()
                            }
                        }
                    }
                }

                launch {
                    profileViewModel.userProfile.collect { state ->
                        when (state) {
                            is UiState.Success -> {
                                KullaniciAdi.text = state.data.kullaniciAdi
                                bioTextView.text = state.data.hakkinda

                                Glide.with(requireContext())
                                    .load(state.data.fotoUrl)
                                    .placeholder(R.drawable.kullanici)
                                    .error(R.drawable.kullanici)
                                    .into(profilFotoImageView)
                            }
                            else -> {}
                        }
                    }
                }
                // 6. Gönderi Sayısı
                launch {
                    viewModel.gonderiSayisi.collect { sayi ->
                            gonderiSayisiTextView.text = sayi.toString()
                    }
                }

                // 7. Gönderi Listesinin Durumu
                launch {
                    viewModel.gonderilerState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                if (!swipeRefreshLayout.isRefreshing && gonderiAdapter.itemCount == 0) {
                                    progressBar.visibility = View.VISIBLE
                                }
                                tvEmpty.visibility = View.GONE
                            }
                            is UiState.Success -> {
                                progressBar.visibility = View.GONE
                                swipeRefreshLayout.isRefreshing = false

                                if (state.data.isEmpty()) {
                                    tvEmpty.visibility = View.VISIBLE
                                    recyclerView.visibility = View.GONE
                                } else {
                                    val ilkGonderi = state.data.first()
                                    Log.d("AdapterDebug", "🔍 [İLK ELEMAN / YENİ GÖNDERİ DETAYI]:")
                                    Log.d("AdapterDebug", "   ➜ Kedi ID: ${ilkGonderi.kediID}")
                                    Log.d("AdapterDebug", "   ➜ Kedi Adı: ${ilkGonderi.kediAdi}")
                                    Log.d("AdapterDebug", "   ➜ Açıklama: ${ilkGonderi.aciklama}")
                                    Log.d("AdapterDebug", "   ➜ Fotoğraflar (Size: ${ilkGonderi.fotoUrlListesi?.size}): ${ilkGonderi.fotoUrlListesi}")
                                    Log.d("AdapterDebug", "   ➜ Tarih: ${ilkGonderi.tarih}")
                                    tvEmpty.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE

                                    Log.d("AdapterDebug", "🚀 Adapter'a submitList() çağrısı yapılıyor...")
                                    gonderiAdapter.submitList(state.data.toList()) {
                                        Log.d("AdapterDebug", "✅ submitList Tamamlandı! RecyclerView 0. pozisyona kaydırılıyor.")
                                        recyclerView.scrollToPosition(0)
                                    }
                                }
                            }
                            is UiState.Error -> {
                                progressBar.visibility = View.GONE
                                swipeRefreshLayout.isRefreshing = false
                                Toast.makeText(requireContext(), state.message, Toast.LENGTH_SHORT).show()
                            }
                            UiState.Idle -> {
                                progressBar.visibility = View.GONE
                                swipeRefreshLayout.isRefreshing = false
                            }
                        }
                    }
                }

                // 8. Genel İşlem Sonucu Bildirimleri
                launch {
                    viewModel.islemSonucu.collect { result ->
                        when (result) {
                            is UiState.Loading -> {}
                            is UiState.Success -> {
                                Toast.makeText(requireContext(), result.data, Toast.LENGTH_SHORT).show()
                            }
                            is UiState.Error -> {
                                Toast.makeText(requireContext(), result.message, Toast.LENGTH_LONG).show()
                            }
                            UiState.Idle -> {}
                        }
                    }
                }
            }
        }
    }

    private fun renderProfileButtons(state: FollowUiState) {
        if (state.isSelfProfile) {
            profiliDuzenleTiklandi.visibility = View.VISIBLE
            takipEtButonu.visibility = View.GONE
            takipEdiliyorVeMesajLayout.visibility = View.GONE
        } else {
            profiliDuzenleTiklandi.visibility = View.GONE
            if (state.isLoadingFollowState) {
                takipEtButonu.visibility = View.GONE
                takipEdiliyorVeMesajLayout.visibility = View.GONE
                progressFollow.visibility = View.VISIBLE
            } else {
                progressFollow.visibility = View.GONE
                if (state.isFollowing) {
                    takipEdiliyorVeMesajLayout.visibility = View.VISIBLE
                    takipEtButonu.visibility = View.GONE
                } else if (state.isFollowed) {
                    takipEtButonu.text = "Sende takip et"
                    takipEtButonu.visibility = View.VISIBLE
                    takipEdiliyorVeMesajLayout.visibility = View.GONE
                } else {
                    takipEtButonu.text = "Takip et"
                    takipEtButonu.visibility = View.VISIBLE
                    takipEdiliyorVeMesajLayout.visibility = View.GONE
                }
            }
        }
    }

    private fun onGonderiTiklandi(gonderi: Gonderi) {
        val fotoList = if (gonderi.fotoUrlListesi != null) {
            ArrayList(gonderi.fotoUrlListesi)
        } else {
            ArrayList()
        }

        val args = GonderiDetayFragment.newBundle(
            fotoList,
            gonderi.kediAdi ?: "",
            gonderi.aciklama ?: "",
            gonderi.begeniSayisi ?: 0L,
            gonderi.kediID ?: ""
        )
        SmartNavigationEngine.navigateTo(Screen.POST, args, gonderi.kediID)
    }

    companion object {
        private const val ARG_USER_ID = "USER_ID"

        @JvmStatic
        fun newArgs(userId: String): Bundle {
            return bundleOf(ARG_USER_ID to userId)
        }
    }
}