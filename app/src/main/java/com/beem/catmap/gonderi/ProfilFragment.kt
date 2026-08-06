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
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
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
import com.beem.catmap.gonderi.ProfileViewModel
import com.beem.catmap.gonderi.UiState
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private var postsLoaded = false
    private lateinit var blockedUserLayout: View
    private lateinit var btnBackEngel: ImageButton
    private lateinit var KullaniciAdiEngel: TextView
    private lateinit var shimmerLayout: ShimmerFrameLayout
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
    private lateinit var takipciSayisiLayout: LinearLayout
    private lateinit var takipEdilenSayisiLayout: LinearLayout
    private lateinit var postSectionHeader: LinearLayout
    private lateinit var takipEdilenSayisiTextView: TextView
    private lateinit var gonderiSayisiTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var KullaniciAdi: TextView
    private lateinit var tvAd: TextView
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

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            shimmerLayout.stopShimmer()
        } else {
            handleBackPressWithEngine()
            // Eğer veriler yükleniyorsa shimmer'ı başlat, yoksa kapat
            if (profileViewModel.fullProfileState.value is UiState.Loading) {
                showShimmerLoading()
            } else {
                hideShimmerLoading()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (!isHidden) {
            handleBackPressWithEngine()
        }

        initViews(view)
        showShimmerLoading()

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        targetUserId?.let { userId ->
            followViewModel.profilDurumunuHazirla(userId)///kenı prfılım, takıp takıpcı mı durumalrına bakıyoe
            profileViewModel.tumProfilVerileriniYukle(userId, forceRefresh = false)
        }
    }

    private fun initViews(view: View) {
        shimmerLayout = view.findViewById(R.id.shimmerLayout)
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
        takipciSayisiLayout = view.findViewById(R.id.takipciSayisiLayout)
        takipEdilenSayisiLayout = view.findViewById(R.id.takipEdilenSayisiLayout)
        takipEdilenSayisiTextView = view.findViewById(R.id.takipEdilenSayisiTextView)
        gonderiSayisiTextView = view.findViewById(R.id.gonderiSayisiTextView)
        bioTextView = view.findViewById(R.id.bioTextView)
        KullaniciAdi = view.findViewById(R.id.KullaniciAdi)
        tvAd = view.findViewById(R.id.tvAdSoyad)
        profilFotoImageView = view.findViewById(R.id.profilFotoImageView)
        postSectionHeader = view.findViewById(R.id.postSectionHeader)
        blockedUserLayout = view.findViewById(R.id.blockedUserLayout)
        btnBackEngel = view.findViewById(R.id.btnBackEngel)
        KullaniciAdiEngel = view.findViewById(R.id.KullaniciAdiEngel)
    }

    // --- UI GÖRÜNÜRLÜK VE SHIMMER METODLARI ---

    private fun showShimmerLoading() {
        if (!swipeRefreshLayout.isRefreshing) {
            shimmerLayout.startShimmer()
            shimmerLayout.visibility = View.VISIBLE
            swipeRefreshLayout.visibility = View.GONE
        }
    }

    private fun hideShimmerLoading() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        swipeRefreshLayout.visibility = View.VISIBLE
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

                        val state = viewModel.uiState.value
                        if (!state.isMoreLoading && !viewModel.isLastPage) {
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

        takipciSayisiLayout.setOnClickListener {
            targetUserId?.let { userId ->
                val args = bundleOf(
                    "yukleyenID" to userId,
                    "startPage" to 0,
                    "kullaniciAdi" to KullaniciAdi.text.toString(),
                )
                SmartNavigationEngine.navigateTo(Screen.FOLLOWERS, args, userId)
            }
        }

        takipEdilenSayisiLayout.setOnClickListener {
            targetUserId?.let { userId ->
                val args = bundleOf(
                    "yukleyenID" to userId,
                    "startPage" to 1,
                    "kullaniciAdi" to KullaniciAdi.text.toString(),
                )
                SmartNavigationEngine.navigateTo(Screen.FOLLOWERS, args, userId)
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
            followViewModel.profilDurumunuHazirla(userId)///
            profileViewModel.tumProfilVerileriniYukle(userId, forceRefresh = forceRefresh)
        } ?: run {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1. Ana Profil Verisi Akışı (Single Source of Truth)
                launch {
                    profileViewModel.fullProfileState.collect { state ->
                        if (isHidden) return@collect
                        when (state) {
                            is UiState.Loading -> {
                                showShimmerLoading()
                            }
                            is UiState.Success -> {
                                hideShimmerLoading()
                                val fullData = state.data
                                val targetId = targetUserId ?: UserSession.userId ?: ""

                                // PostViewModel'in Paging yapısını verilerle doldur
                                viewModel.setupFromFullProfile(
                                    userId = targetId,
                                    cacheData = fullData.postsCache
                                )

                                // FollowViewModel'i hazır verilerle doldur
                                followViewModel.setupFromFullProfile(
                                    followerCount = fullData.followerCount,
                                    followingCount = fullData.followingCount,
                                    isSelf = fullData.isSelfProfile
                                )

                                // Header profil bilgilerini bağla
                                bindUserProfileData(fullData.profile)
                            }
                            is UiState.Blocked -> {
                                hideShimmerLoading()
                                handleBlockedUiState()
                            }
                            is UiState.Error -> {
                                hideShimmerLoading()
                                swipeRefreshLayout.isRefreshing = false
                            }
                            else -> {}
                        }
                    }
                }

                // 2. Profil Güncelleme EventBus Takibi
                launch {
                    ProfileEventBus.profileEvent.collect { event ->
                        if (isHidden) return@collect
                        when (event) {
                            is ProfileEvent.ProfileUpdated -> {
                                val guncelKullanici = event.updatedUser
                                if (targetUserId == myUserId && guncelKullanici != null) {
                                    profileViewModel.lokalProfilVerisiniGuncelle(guncelKullanici)
                                }
                            }
                            else -> {}
                        }
                    }
                }

                // 3. Takip Et / Takipten Çık Buton Durumu Dinleyici
                launch {
                    followViewModel.followUiState.collectLatest { state ->
                        if (isHidden) return@collectLatest
                        renderProfileButtons(state)
                    }
                }

                // 4. Takipçi / Takip Edilen Sayıları Dinleyici
                launch {
                    if (targetUserId == myUserId) {
                        followViewModel.profileState.collect { profileState ->
                            if (isHidden) return@collect
                            takipciSayisiTextView.text = profileState.takipciSayisi.toString()
                            takipEdilenSayisiTextView.text = profileState.takipEdilenSayisi.toString()
                        }
                    } else {
                        launch {
                            followViewModel.targetUserTakipciSayisi.collect { sayi ->
                                if (isHidden) return@collect
                                takipciSayisiTextView.text = sayi.toString()
                            }
                        }

                        launch {
                            followViewModel.targetUserTakipEdilenSayisi.collect { sayi ->
                                if (isHidden) return@collect
                                takipEdilenSayisiTextView.text = sayi.toString()
                            }
                        }
                    }
                }

                // 5. Post RecyclerView ve UI State Dinleyici
                launch {
                    viewModel.uiState.collect { state ->
                        if (isHidden) return@collect

                        if (state.isLoading && !swipeRefreshLayout.isRefreshing && gonderiAdapter.itemCount == 0 && shimmerLayout.visibility != View.VISIBLE) {
                            progressBar.visibility = View.VISIBLE
                        } else {
                            progressBar.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false
                        }

                        if (state.isAccessDenied) {
                            recyclerView.visibility = View.GONE
                            postSectionHeader.visibility = View.GONE
                            tvEmpty.text = "🔒 Bu hesap gizli.\nGönderilerini görmek için takip et."
                            tvEmpty.visibility = View.VISIBLE
                        } else {
                            postSectionHeader.visibility = View.VISIBLE
                            if (state.isEmpty) {
                                tvEmpty.text = "Henüz gönderi yok"
                                tvEmpty.visibility = View.VISIBLE
                                recyclerView.visibility = View.GONE
                            } else {
                                tvEmpty.visibility = View.GONE
                                recyclerView.visibility = View.VISIBLE
                                gonderiAdapter.submitList(state.posts)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindUserProfileData(profileData: com.beem.catmap.gonderi.UserProfileData) {
        KullaniciAdi.text = profileData.kullaniciAdi
        tvAd.text = profileData.ad
        bioTextView.text = profileData.hakkinda

        gonderiSayisiTextView.text = profileData.gonderiSayisi.toString()

        Glide.with(requireContext())
            .load(profileData.fotoUrl)
            .placeholder(R.drawable.kullanici)
            .error(R.drawable.kullanici)
            .into(profilFotoImageView)
    }

    private fun handleBlockedUiState() {
        blockedUserLayout.visibility= View.VISIBLE
    }

    override fun onPause() {
        shimmerLayout.stopShimmer()
        super.onPause()
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
            gonderi.kediID ?: "",
            targetUserId ?: ""
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