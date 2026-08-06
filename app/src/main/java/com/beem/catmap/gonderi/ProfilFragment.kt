package com.beem.catmap.Profil

import android.os.Bundle
import android.transition.AutoTransition
import android.transition.TransitionManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beem.catmap.KullaniciAuth.Kullanici
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
import com.beem.catmap.ui.extensions.bounceAndHaptic
import com.beem.catmap.ui.extensions.fadeInSmooth
import com.beem.catmap.ui.extensions.fadeOutSmooth
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
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
    private var firstLoad = false
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
    private var hasUnlockedPosts = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetUserId = it.getString(ARG_USER_ID)

            Log.d("TARGET",targetUserId.toString())
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

        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        targetUserId?.let { userId ->
            followViewModel.profilDurumunuHazirla(userId, forceRefresh = false)

            val isFollowing = followViewModel.followUiState.value.isFollowing
            Log.d("FOLLOW",isFollowing.toString())
            profileViewModel.tumProfilVerileriniYukle(
                targetUserId = userId,
                isFollowing,
                forceRefresh = false
            )

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
                        if (!state.isMoreLoading && !state.isLastPage) {
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
                    it.bounceAndHaptic()
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
                    it.bounceAndHaptic()
                    followViewModel.takiptenCikar(
                        takiptenCiktiginId = targetId,
                        currentUserId = myUserId
                    )
                }
            }
        }

        sohbetButon.setOnClickListener { }

        profiliDuzenleTiklandi.setOnClickListener {
            it.bounceAndHaptic()
            SmartNavigationEngine.navigateTo(Screen.EDIT_PROFILE)
        }
    }

    private fun yukleVerileri(forceRefresh: Boolean = false) {
        targetUserId?.let { userId ->
            followViewModel.profilDurumunuHazirla(userId, forceRefresh = forceRefresh)
            val isFollowing = followViewModel.followUiState.value.isFollowing
            profileViewModel.tumProfilVerileriniYukle(userId, isFollowing,forceRefresh = forceRefresh)
        } ?: run {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    profileViewModel.fullProfileState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                Log.d("SHIMMER","loadıngshow")
                                showShimmerLoading()
                            }
                            is UiState.Success -> {
                                hideShimmerLoading()
                                swipeRefreshLayout.isRefreshing = false

                                val fullData = state.data
                                val targetId = targetUserId ?: UserSession.userId.orEmpty()


                                viewModel.setupFromFullProfile(
                                    userId = targetId,
                                    initialPosts = fullData.posts,
                                    lastDoc = fullData.lastDocument,
                                    isLast = fullData.isLastPage,
                                    isAccessDenied = fullData.isAccessDenied
                                )

                                // FollowViewModel'i hazır verilerle doldur
                                followViewModel.setupFromFullProfile(
                                    followerCount = fullData.followerCount,
                                    followingCount = fullData.followingCount,
                                    isSelf = fullData.isSelfProfile
                                )

                                bindUserProfileData(fullData.profile)
                            }
                            is UiState.Blocked -> {
                                hideShimmerLoading()
                                swipeRefreshLayout.isRefreshing = false
                                handleBlockedUiState()
                            }
                            is UiState.Error -> {
                                hideShimmerLoading()
                                swipeRefreshLayout.isRefreshing = false
                            }
                            else -> {
                                swipeRefreshLayout.isRefreshing = false
                            }
                        }
                    }
                }

                // 2. Profil Güncelleme EventBus Takibi
                launch {
                    ProfileEventBus.profileEvent.collect { event ->
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


                launch {
                    followViewModel.followUiState.collectLatest { state ->
                        renderProfileButtons(state)
                    }
                }

                launch {
                    followViewModel.followUiState
                        .map { Pair(it.isFollowing, it.isSelfProfile) }
                        .distinctUntilChanged()
                        .collect { (isFollowing, isSelfProfile) ->
                            if (isSelfProfile) return@collect

                            if (isFollowing) {
                                targetUserId?.let { userId ->
                                    viewModel.gonderileriGetir(
                                        userId = userId,
                                        isFollowing = true
                                    )
                                }
                            } else {
                                viewModel.setAccessDenied(true)
                            }
                        }
                }

                launch {
                    if (targetUserId == myUserId) {
                        profileViewModel.profileState.collect { profileState ->
                            takipciSayisiTextView.text = profileState.takipciSayisi.toString()
                            takipEdilenSayisiTextView.text = profileState.takipEdilenSayisi.toString()
                            gonderiSayisiTextView.text = profileState.gonderiSayisi.toString()
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
                    }
                }

                // 5. Post RecyclerView ve UI State Dinleyici
                launch {
                    viewModel.uiState.collect { state ->

                        if (state.isLoading && !swipeRefreshLayout.isRefreshing && gonderiAdapter.itemCount == 0 && shimmerLayout.visibility != View.VISIBLE) {
                            progressBar.visibility = View.VISIBLE
                        } else {
                            progressBar.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false
                        }
                        if (state.isAccessDenied) {
                            recyclerView.visibility = View.GONE
                            postSectionHeader.visibility = View.GONE
                            swipeRefreshLayout.isRefreshing = false
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

    // DÜZELTME: Model türü UserProfileData yerine Kullanici yapıldı
    private fun bindUserProfileData(kullanici: Kullanici) {
        KullaniciAdi.text = kullanici.kullaniciAdi.orEmpty()

        val tamAd = kullanici.ad.trim()
        tvAd.text = tamAd

        bioTextView.text = kullanici.biyografi.orEmpty()

        gonderiSayisiTextView.text = (kullanici.gonderiSayisi ?: 0L).toString()

        Glide.with(requireContext())
            .load(kullanici.fotoUrl)
            .placeholder(R.drawable.kullanici)
            .error(R.drawable.kullanici)
            .into(profilFotoImageView)
    }

    private fun handleBlockedUiState() {
        blockedUserLayout.visibility = View.VISIBLE
    }

    private fun renderProfileButtons(state: FollowUiState) {
        if (state.isSelfProfile) {
            profiliDuzenleTiklandi.fadeInSmooth()
            takipEtButonu.fadeOutSmooth()
            takipEdiliyorVeMesajLayout.fadeOutSmooth()
            progressFollow.fadeOutSmooth()
        } else {
            profiliDuzenleTiklandi.fadeOutSmooth()

            if (state.isLoadingFollowState) {
                takipEtButonu.fadeOutSmooth()
                takipEdiliyorVeMesajLayout.fadeOutSmooth()
                progressFollow.fadeInSmooth()
            } else {
                progressFollow.fadeOutSmooth()

                if (state.isFollowing) {
                    takipEdiliyorVeMesajLayout.fadeInSmooth()
                    takipEtButonu.fadeOutSmooth()
                } else if (state.isFollowed) {
                    takipEtButonu.text = "Sen de takip et"
                    takipEtButonu.fadeInSmooth()
                    takipEdiliyorVeMesajLayout.fadeOutSmooth()
                } else {
                    takipEtButonu.text = "Takip et"
                    takipEtButonu.fadeInSmooth()
                    takipEdiliyorVeMesajLayout.fadeOutSmooth()
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