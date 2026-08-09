package com.beem.catmap.Profil

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beem.catmap.KullaniciAuth.Kullanici
import com.beem.catmap.Profil.Gonderiler.GonderiAdapter
import com.beem.catmap.Profil.Gonderiler.LoadingFooterAdapter
import com.beem.catmap.R
import com.beem.catmap.UyariMesaji
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.gonderi.FollowUiState
import com.beem.catmap.gonderi.FollowViewModel
import com.beem.catmap.gonderi.PostViewModel
import com.beem.catmap.gonderi.ProfileDialogHelper
import com.beem.catmap.gonderi.ProfileViewModel
import com.beem.catmap.gonderi.UiState
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.extensions.bounceAndHaptic
import com.beem.catmap.ui.extensions.setLoadingState
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.beem.catmap.ui.viewmodel.BlockActionState
import com.beem.catmap.ui.viewmodel.UserBlockViewModel
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {
    private val viewModel: PostViewModel by viewModels()
    private val followViewModel: FollowViewModel by viewModels()
    private val userBlockViewModel: UserBlockViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var blockedUserLayout: View
    private lateinit var btnBackEngel: ImageButton
    private lateinit var KullaniciAdiEngel: TextView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var myConstraintLayout: ConstraintLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressFollow: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton
    private lateinit var profiliDuzenleTiklandi: Button
    private lateinit var takipEtButonu: Button
    private lateinit var ppMenuButton: ImageView
    private lateinit var takipEdiliyorVeMesajLayout: LinearLayout
    private lateinit var takipEdiliyorButonu: Button
    private lateinit var engelKaldirButton: Button
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
    private val loadingFooterAdapter = LoadingFooterAdapter()
    private lateinit var concatAdapter: ConcatAdapter
    private var profileLoaded = false
    val myUserId = UserSession.userId
    private var targetUserId: String? = null
    private var initialIsFollowed: Boolean = false
    private lateinit var uyariMesaji: UyariMesaji

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            targetUserId = it.getString(ARG_USER_ID)
            initialIsFollowed = it.getBoolean("IS_FOLLOWED", false)
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
        uyariMesaji = UyariMesaji(requireContext(), true)
        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (initialIsFollowed) {
            followViewModel.setInitialFollowedState(isFollowed = true)
        }

        yukleVerileri(forceRefresh = false)
    }

    override fun onStop() {
        super.onStop()
        profileLoaded = false
        profileViewModel.resetProfileState()
    }
    private fun initViews(view: View) {
        myConstraintLayout = view.findViewById(R.id.myConstraintLayout)
        shimmerLayout = view.findViewById(R.id.shimmerLayout)
        recyclerView = view.findViewById(R.id.gonderiRecyclerView)
        progressBar = view.findViewById(R.id.progressBarr)
        progressFollow = view.findViewById(R.id.progressFollow)
        tvEmpty = view.findViewById(R.id.emptyTextView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        btnBack = view.findViewById(R.id.btnBack)
        profiliDuzenleTiklandi = view.findViewById(R.id.profiliDuzenleTiklandi)
        takipEtButonu = view.findViewById(R.id.takipEtButonu)
        engelKaldirButton = view.findViewById(R.id.EngelKaldirButton)
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
        ppMenuButton = view.findViewById(R.id.ppMenuButton)
    }

    override fun onDestroyView() {
        uyariMesaji.kapat()
        super.onDestroyView()
    }

    private fun showShimmerLoading() {
        shimmerLayout.visibility = View.VISIBLE
        shimmerLayout.startShimmer()

        myConstraintLayout.visibility = View.GONE
        blockedUserLayout.visibility = View.GONE
    }

    private fun hideShimmerLoading() {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE

        myConstraintLayout.visibility = View.VISIBLE
    }

    private fun setupRecyclerView() {
        gonderiAdapter = GonderiAdapter { gonderi ->
            onGonderiTiklandi(gonderi)
        }

        concatAdapter = ConcatAdapter(gonderiAdapter, loadingFooterAdapter)

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position >= gonderiAdapter.itemCount) {
                    3
                } else {
                    1
                }
            }
        }

        recyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = concatAdapter

            setHasFixedSize(false)

            layoutAnimation = AnimationUtils.loadLayoutAnimation(
                requireContext(),
                R.anim.layout_animation_fall_down
            )

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
            it.bounceAndHaptic()
            SmartNavigationEngine.navigateBack()
        }

        swipeRefreshLayout.setOnRefreshListener {
            yukleVerileri(forceRefresh = true)
        }

        takipEtButonu.setOnClickListener {
            it.bounceAndHaptic()
            takipEdiliyorButonu.setLoadingState(true)
            targetUserId?.let { targetId ->
                if (targetId != myUserId) {
                    viewModel.setPostLoadingState()
                    Log.d("SHIMMER","TAKIPETE BASILDI")
                    followViewModel.takipEt(
                        takipEttiginId = targetId,
                        currentUserId = myUserId,
                        onSuccess = {
                            takipEdiliyorButonu.setLoadingState(false)
                            viewModel.gonderileriGetir(targetId)
                            val profile = (profileViewModel.fullProfileState.value as? UiState.Success)?.data?.profile

                            val userId = profile?.id.orEmpty().ifBlank { targetId }
                            val kullaniciAdi = profile?.kullaniciAdi.orEmpty()
                            val fotoUrl = profile?.fotoUrl.orEmpty()

                            ProfileEventBus.emitEvent(
                                ProfileEvent.FollowUser(
                                    userId = userId,
                                    kullaniciAdi = kullaniciAdi,
                                    fotoUrl = fotoUrl,
                                    operatorUserId = myUserId
                                )
                            )
                        },
                        onFailure = {
                            takipEdiliyorButonu.setLoadingState(false)
                            progressFollow.visibility = View.GONE
                        }
                    )
                }
            }
        }
        takipciSayisiLayout.setOnClickListener {
            it.bounceAndHaptic()
            targetUserId?.let { userId ->
                val args = bundleOf(
                    "yukleyenID" to userId,
                    "startPage" to 0,
                    "kullaniciAdi" to KullaniciAdi.text.toString(),
                )

                SmartNavigationEngine.navigateTo(
                    Screen.FOLLOWERS,
                    args,
                    "${userId}_0"
                )
            }
        }

        takipEdilenSayisiLayout.setOnClickListener {
            it.bounceAndHaptic()
            targetUserId?.let { userId ->
                val args = bundleOf(
                    "yukleyenID" to userId,
                    "startPage" to 1,
                    "kullaniciAdi" to KullaniciAdi.text.toString(),
                )

                SmartNavigationEngine.navigateTo(
                    Screen.FOLLOWERS,
                    args,
                    "${userId}_1"
                )
            }
        }

        takipEdiliyorButonu.setOnClickListener {
            it.bounceAndHaptic()
            recyclerView.visibility= View.GONE
            postSectionHeader.visibility = View.GONE
            takipEtButonu.setLoadingState(true)
            targetUserId?.let { targetId ->
                if (targetId != myUserId) {
                    followViewModel.takiptenCikar(
                        takiptenCiktiginId = targetId,
                        currentUserId = myUserId,
                        onSuccess = {
                            takipEtButonu.setLoadingState(false)
                            viewModel.setAccessDenied(true)
                            val profile = (profileViewModel.fullProfileState.value as? UiState.Success)?.data?.profile
                            val userId = profile?.id.orEmpty().ifBlank { targetId }

                            ProfileEventBus.emitEvent(
                                ProfileEvent.UnFollowUser(
                                    userId = userId,
                                    operatorUserId = myUserId
                                )
                            )
                        },
                        onFailure = {
                            takipEtButonu.setLoadingState(false)
                            progressFollow.visibility = View.GONE
                        }
                    )
                }
            }
        }

        engelKaldirButton.setOnClickListener {
            it.bounceAndHaptic()
            engelKaldirButton.visibility= View.GONE
            takipEtButonu.visibility = View.VISIBLE
            showEngelKaldirDialog()
        }
        sohbetButon.setOnClickListener { }

        profiliDuzenleTiklandi.setOnClickListener {
            it.bounceAndHaptic()
            SmartNavigationEngine.navigateTo(Screen.EDIT_PROFILE)
        }
        ppMenuButton.setOnClickListener { anchorView ->
            showOptionMenu(anchorView)
        }
    }
    private fun showOptionMenu(view: View) {
        val popupMenu = PopupMenu(requireContext(), view)
        popupMenu.menuInflater.inflate(R.menu.profil_uc_nokta_menu, popupMenu.menu)

        val isMyFollower = followViewModel.followUiState.value.isFollowed
        val isBlocked = userBlockViewModel.benimEngellediklerim.value.any { it.id == targetUserId }

        val removeFollowerItem = popupMenu.menu.findItem(R.id.profiltakipciCikar)
        removeFollowerItem?.isVisible = isMyFollower

        val blockItem = popupMenu.menu.findItem(R.id.profilmenu_engelle)
        val unblockItem = popupMenu.menu.findItem(R.id.profilmenu_engeliKaldir)

        blockItem?.isVisible = !isBlocked
        unblockItem?.isVisible = isBlocked

        popupMenu.setOnMenuItemClickListener { menuItem ->
            when (menuItem.itemId) {
                R.id.profiltakipciCikar -> {
                    showTakipcidenCikarDialog()
                    true
                }
                R.id.profilmenu_engelle -> {
                    showEngelleDialog()
                    true
                }
                R.id.profilmenu_engeliKaldir -> {
                    showEngelKaldirDialog()
                    true
                }
                else -> false
            }
        }
        popupMenu.show()
    }
    private fun showTakipcidenCikarDialog() {
        val currentProfile = (profileViewModel.fullProfileState.value as? UiState.Success)?.data?.profile

        ProfileDialogHelper.showTakipcidenCikarDialog(
            context = requireContext(),
            kullaniciAdi = currentProfile?.kullaniciAdi,
            onConfirm = {
                targetUserId?.let { id ->
                    followViewModel.takipcidenCikar(
                        takipciId = id,
                        currentUserId = myUserId,
                        onSuccess = {
                            val userId = currentProfile?.id.orEmpty().ifBlank { targetUserId }
                            userId?.let {
                                ProfileEventBus.emitEvent(
                                    ProfileEvent.UnFollowerUser(
                                        userId = it,
                                        operatorUserId = myUserId
                                    )
                                )
                            }
                        },
                        onFailure = {
                            progressFollow.visibility = View.GONE
                        }
                    )
                }
            }
        )
    }

    private fun showEngelleDialog() {
        val currentProfile = (profileViewModel.fullProfileState.value as? UiState.Success)?.data?.profile

        ProfileDialogHelper.showEngelleDialog(
            context = requireContext(),
            kullaniciAdi = currentProfile?.kullaniciAdi,
            onConfirm = {
                currentProfile?.let { engellenecekKullanici ->
                    userBlockViewModel.engelle(
                        engellenecekKullanici = engellenecekKullanici,
                        kisiId = myUserId,
                        onResult = { isSuccess ->
                            if (isSuccess) {
                                snapUiBlocked()
                            }
                        }
                    )
                }
            }
        )
    }
    private fun showEngelKaldirDialog() {
        val currentProfile = (profileViewModel.fullProfileState.value as? UiState.Success)?.data?.profile

        ProfileDialogHelper.showEngelKaldirDialog(
            context = requireContext(),
            kullaniciAdi = currentProfile?.kullaniciAdi,
            onConfirm = {
                engelKaldirButton.visibility= View.GONE
                takipEtButonu.visibility = View.VISIBLE
                targetUserId?.let { engellenenId ->
                    userBlockViewModel.engelKaldir(
                        engellenenKullaniciId = engellenenId,
                        kisiId = myUserId,
                        onResult = { isSuccess ->
                            if (isSuccess) {

                            }
                        }
                    )
                }
            }
        )
    }
    private fun snapUiBlocked(){
        recyclerView.visibility = View.GONE
        postSectionHeader.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressFollow.visibility = View.GONE

        tvEmpty.text = "🚫 Bu kullanıcıyı engellediniz."
        tvEmpty.visibility = View.VISIBLE

        profiliDuzenleTiklandi.visibility = View.GONE
        takipEtButonu.visibility = View.GONE
        takipEdiliyorVeMesajLayout.visibility = View.GONE
        engelKaldirButton.visibility = View.VISIBLE
    }
    private fun setupBlockedByMeUi(kullanici: Kullanici?) {
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false

        myConstraintLayout.visibility = View.VISIBLE
        blockedUserLayout.visibility = View.GONE

        kullanici?.let {
            KullaniciAdi.text = it.kullaniciAdi.orEmpty()
            tvAd.text = it.ad.orEmpty().trim()
            bioTextView.text = it.biyografi.orEmpty()

            Glide.with(requireContext())
                .load(it.fotoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(profilFotoImageView)
        }

        recyclerView.visibility = View.GONE
        postSectionHeader.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressFollow.visibility = View.GONE

        profiliDuzenleTiklandi.visibility = View.GONE
        takipEtButonu.visibility = View.GONE
        takipEdiliyorVeMesajLayout.visibility = View.GONE
        engelKaldirButton.visibility = View.VISIBLE

        tvEmpty.text = "🚫 Bu kullanıcıyı engellediniz."
        tvEmpty.visibility = View.VISIBLE
    }

    private fun yukleVerileri(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            profileLoaded = false
        }
        targetUserId?.let { userId ->
            profileViewModel.tumProfilVerileriniYukle(targetUserId = userId, forceRefresh = forceRefresh)
        } ?: run {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    userBlockViewModel.blockActionState.collect { state ->
                        when (state) {
                            is BlockActionState.Loading -> {
                                uyariMesaji.YuklemeDurum(state.message)
                            }
                            is BlockActionState.Success -> {
                                uyariMesaji.BasariliDurum(state.message, 1500)

                            }
                            is BlockActionState.Error -> {
                                uyariMesaji.BasarisizDurum(state.message, 1500)
                            }
                            is BlockActionState.Idle -> {
                                // Herhangi bir işlem yok
                            }
                        }
                    }
                }
                launch {
                    profileViewModel.fullProfileState.collect { state ->
                        when (state) {
                            is UiState.Loading -> {
                                Log.d("SHIMMER", "Loading - Shimmer Başlatıldı")
                                showShimmerLoading()
                            }

                            is UiState.Success -> {
                                Log.d("SHIMMER", "Success - Veriler Bağlanıyor")
                                val fullData = state.data
                                val targetId = targetUserId ?: UserSession.userId.orEmpty()

                                if (!profileLoaded) {
                                    Log.d("IFF","if")
                                    profileLoaded = true
                                    viewModel.setupFromFullProfile(
                                        userId = targetId,
                                        initialPosts = fullData.posts,
                                        lastDoc = fullData.lastDocument,
                                        isLast = fullData.isLastPage,
                                        isAccessDenied = fullData.isAccessDenied
                                    )
                                }

                                followViewModel.setupFromFullProfile(
                                    followerCount = fullData.followerCount,
                                    followingCount = fullData.followingCount,
                                    isSelf = fullData.isSelfProfile,
                                    isFollowing = fullData.isFollowing,
                                    isFollowed = fullData.isFollowed,
                                )

                                bindUserProfileData(fullData.profile)
                                swipeRefreshLayout.isRefreshing = false
                                hideShimmerLoading()
                            }
                            is UiState.BlockedBy -> {
                                shimmerLayout.stopShimmer()
                                shimmerLayout.visibility = View.GONE

                                myConstraintLayout.visibility = View.GONE
                                blockedUserLayout.visibility = View.VISIBLE
                                swipeRefreshLayout.isRefreshing = false

                            }
                            is UiState.Blocked -> {
                                setupBlockedByMeUi(state.profile)
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

                launch {
                    ProfileEventBus.profileEvent.collect { event ->
                        when (event) {
                            is ProfileEvent.ProfileUpdated -> {
                                val guncelKullanici = event.updatedUser
                                guncelKullanici?.let { bindUserProfileData(it) }
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
                launch {
                    viewModel.uiState.collect { state ->
                            loadingFooterAdapter.setLoading(state.isMoreLoading)
                            if (state.isLoading) {
                                recyclerView.visibility = View.GONE
                                if (!swipeRefreshLayout.isRefreshing && shimmerLayout.visibility != View.VISIBLE) {
                                    progressBar.visibility = View.VISIBLE
                                }
                                return@collect
                            }

                            progressBar.visibility = View.GONE
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
                                }else {
                                    tvEmpty.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE
                                    gonderiAdapter.submitList(state.posts) {
                                        recyclerView.scheduleLayoutAnimation()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindUserProfileData(kullanici: Kullanici) {
        KullaniciAdi.text = kullanici.kullaniciAdi.orEmpty()

        val tamAd = kullanici.ad.trim()
        tvAd.text = tamAd

        bioTextView.text = kullanici.biyografi

        gonderiSayisiTextView.text = (kullanici.gonderiSayisi ?: 0L).toString()

        Glide.with(requireContext())
            .load(kullanici.fotoUrl)
            .placeholder(R.drawable.kullanici)
            .error(R.drawable.kullanici)
            .into(profilFotoImageView)
    }

    override fun onPause() {
        shimmerLayout.stopShimmer()
        super.onPause()
    }

    private fun renderProfileButtons(state: FollowUiState) {

        if (state.isBlocked) {
            profiliDuzenleTiklandi.visibility = View.GONE
            takipEtButonu.visibility = View.GONE
            takipEdiliyorVeMesajLayout.visibility = View.GONE
            engelKaldirButton.visibility = View.VISIBLE
            progressFollow.visibility = View.GONE
            return
        }
        if (state.isSelfProfile == null) {
            profiliDuzenleTiklandi.visibility = View.GONE
            takipEtButonu.visibility = View.GONE
            takipEdiliyorVeMesajLayout.visibility = View.GONE
            progressFollow.visibility = View.VISIBLE
            return
        }

        progressFollow.visibility = View.GONE

        if (state.isSelfProfile == true) {

            profiliDuzenleTiklandi.visibility = View.VISIBLE
            takipEtButonu.visibility = View.GONE
            takipEdiliyorVeMesajLayout.visibility = View.GONE

        } else {

            profiliDuzenleTiklandi.visibility = View.GONE

            if (state.isFollowing) {
                takipEdiliyorVeMesajLayout.visibility = View.VISIBLE
                takipEtButonu.visibility = View.GONE

            } else if (state.isFollowed) {
                takipEtButonu.text = "Sen de takip et"
                takipEtButonu.visibility = View.VISIBLE
                takipEdiliyorVeMesajLayout.visibility = View.GONE

            } else {
                takipEtButonu.text = "Takip et"
                takipEtButonu.visibility = View.VISIBLE
                takipEdiliyorVeMesajLayout.visibility = View.GONE
            }
        }
    }

    private fun onGonderiTiklandi(gonderi: Gonderi) {
        val args = bundleOf(
            "kediid" to gonderi.kediID,
            "yukleyenId" to targetUserId
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