package com.beem.catmap.ui.profile.common

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
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
import com.beem.catmap.data.model.UserModel
import com.beem.catmap.ui.profile.post.PostAdapter
import com.beem.catmap.R
import com.beem.catmap.WarningMessage
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.model.BadgeTier
import com.beem.catmap.data.model.EquippedBadgeModel
import com.beem.catmap.data.model.Post
import com.beem.catmap.managers.OnlinePresenceManager
import com.beem.catmap.ui.profile.block.BlockActionState
import com.beem.catmap.ui.profile.follow.state.FollowState
import com.beem.catmap.ui.profile.follow.viewmodel.FollowActionViewModel
import com.beem.catmap.ui.profile.post.LoadingFooterAdapter
import com.beem.catmap.ui.profile.post.PostViewModel
import com.beem.catmap.ui.auth.exceptions.IsBlockedByException
import com.beem.catmap.ui.badge.BadgeStoryBottomSheet
import com.beem.catmap.ui.components.CatMapDialog
import com.beem.catmap.ui.components.CatMapPopupMenu
import com.beem.catmap.ui.extensions.bounceAndHaptic
import com.beem.catmap.ui.extensions.setLoadingState
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.manager.UiMessageManager
import com.beem.catmap.ui.manager.UiMessageState
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.beem.catmap.ui.report.ReportType
import com.beem.catmap.ui.viewmodel.UserBlockViewModel
import com.beem.catmap.utils.withPossessiveSuffix
import com.bumptech.glide.Glide
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.material.card.MaterialCardView
import com.google.android.material.imageview.ShapeableImageView
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlin.jvm.javaClass
import kotlin.toString

class ProfileFragment : Fragment() {
    private val viewModel: PostViewModel by viewModels()
    private val followActionViewModel: FollowActionViewModel by viewModels()
    private val userBlockViewModel: UserBlockViewModel by activityViewModels()
    private val profileViewModel: ProfileViewModel by viewModels()
    private lateinit var blockedUserLayout: View
    private lateinit var btnBackBlock: ImageButton
    private lateinit var usernameBlock: TextView
    private lateinit var shimmerLayout: ShimmerFrameLayout
    private lateinit var myConstraintLayout: ConstraintLayout
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var progressFollow: ProgressBar
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton
    private lateinit var profiliDuzenleTiklandi: Button
    private lateinit var followButton: Button // takıp et butonu
    private lateinit var ppMenuButton: ImageView
    private lateinit var menuButtonEngel: ImageView
    private lateinit var followingAndMessage: LinearLayout
    private lateinit var followingButton: Button //takıpedılıyor butonu
    private lateinit var unblockButton: Button
    private lateinit var chatButton: Button
    private lateinit var followersCountTextView: TextView //takıpcı syaısı
    private lateinit var followersCountLayout: LinearLayout
    private lateinit var followingCountLayout: LinearLayout
    private lateinit var postSectionHeader: LinearLayout
    private lateinit var followingCountTextView: TextView
    private lateinit var postCountTextView: TextView
    private lateinit var bioTextView: TextView
    private lateinit var usernameTextView: TextView
    private lateinit var tvAd: TextView
    private lateinit var profilePhotoImageView: CircleImageView
    private lateinit var profilePhotoBlock: ShapeableImageView
    private lateinit var cardEquippedBadge: MaterialCardView
    private lateinit var imgEquippedBadge: ImageView
    private lateinit var tvEquippedBadgeTitle: TextView
    private lateinit var postAdapter: PostAdapter
    private val loadingFooterAdapter = LoadingFooterAdapter()
    private lateinit var concatAdapter: ConcatAdapter
    private var profileLoaded = false
    val myUserId = UserSession.userId
    private var targetUserId: String? = null
    private var initialIsFollowed: Boolean = false
    private lateinit var WarningMessage: WarningMessage
    private var loadedUserModel: UserModel? = null

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
        WarningMessage = WarningMessage(requireContext(), true)
        initViews(view)
        setupRecyclerView()
        setupListeners()
        observeViewModel()

        if (initialIsFollowed) {
            followActionViewModel.setInitialFollowedState(isFollowed = true)
        }

        yukleVerileri(forceRefresh = false)
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
        followButton = view.findViewById(R.id.takipEtButonu)
        unblockButton = view.findViewById(R.id.EngelKaldirButton)
        followingAndMessage = view.findViewById(R.id.takipEdiliyorVeMesajLayout)
        followingButton = view.findViewById(R.id.takipEdiliyorButonu)
        chatButton = view.findViewById(R.id.sohbetButon)
        followersCountTextView = view.findViewById(R.id.takipciSayisiTextView)
        followersCountLayout = view.findViewById(R.id.takipciSayisiLayout)
        followingCountLayout = view.findViewById(R.id.takipEdilenSayisiLayout)
        followingCountTextView = view.findViewById(R.id.takipEdilenSayisiTextView)
        postCountTextView = view.findViewById(R.id.gonderiSayisiTextView)
        bioTextView = view.findViewById(R.id.bioTextView)
        usernameTextView = view.findViewById(R.id.KullaniciAdi)
        tvAd = view.findViewById(R.id.tvAdSoyad)
        profilePhotoImageView = view.findViewById(R.id.profilFotoImageView)
        profilePhotoBlock = view.findViewById(R.id.imgProfilFotoEngel)
        cardEquippedBadge = view.findViewById(R.id.cardEquippedBadge)
        imgEquippedBadge = view.findViewById(R.id.imgEquippedBadge)
        tvEquippedBadgeTitle = view.findViewById(R.id.tvEquippedBadgeTitle)
        postSectionHeader = view.findViewById(R.id.postSectionHeader)
        blockedUserLayout = view.findViewById(R.id.blockedUserLayout)
        btnBackBlock = view.findViewById(R.id.btnBackEngel)
        usernameBlock = view.findViewById(R.id.KullaniciAdiEngel)
        ppMenuButton = view.findViewById(R.id.ppMenuButton)
        menuButtonEngel = view.findViewById(R.id.menuButtonEngel)
    }

    override fun onDestroyView() {
        WarningMessage.kapat()
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
        postAdapter = PostAdapter { gonderi ->
            onGonderiTiklandi(gonderi)
        }

        concatAdapter = ConcatAdapter(postAdapter, loadingFooterAdapter)

        val gridLayoutManager = GridLayoutManager(requireContext(), 3)

        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return if (position >= postAdapter.itemCount) {
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
                                viewModel.getMorePosts()
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
        btnBackBlock.setOnClickListener {
            it.bounceAndHaptic()
            SmartNavigationEngine.navigateBack()
        }

        swipeRefreshLayout.setOnRefreshListener {
            yukleVerileri(forceRefresh = true)
        }

        followButton.setOnClickListener {
            it.bounceAndHaptic()
            followingButton.setLoadingState(true)
            targetUserId?.let { targetId ->
                if (targetId != myUserId) {
                    viewModel.setPostLoadingState()
                    Log.d("FOLLOW_DEBUG", "0. Fragment: Takip Et Butonuna Basıldı! TargetId: $targetId")
                    followActionViewModel.takipEt(
                        takipEttiginId = targetId,
                        onSuccess = {
                            Log.d("FOLLOW_DEBUG", "8. Fragment: onSuccess Tetiklendi.")
                            followingButton.setLoadingState(false)
                            viewModel.getPosts(targetId)
                            val profile = (profileViewModel.fullProfileState.value as? UiState.Success)?.data?.profile

                            val userId = profile?.id.orEmpty().ifBlank { targetId }
                            val kullaniciAdi = profile?.username.orEmpty()
                            val fotoUrl = profile?.photoUrl.orEmpty()

                            ProfileEventBus.emitEvent(
                                ProfileEvent.FollowUser(
                                    userId = userId,
                                    kullaniciAdi = kullaniciAdi,
                                    fotoUrl = fotoUrl,
                                    operatorUserId = myUserId
                                )
                            )
                            profileViewModel.resetProfileState()
                        },
                        onFailure = { exception ->
                            Log.e("FOLLOW_DEBUG", "8. Fragment: onFailure Tetiklendi! Exception Class: ${exception.javaClass.name}")
                            followButton.setLoadingState(false)
                            progressFollow.visibility = View.GONE

                            if (exception is IsBlockedByException) {
                                Log.d("FOLLOW_DEBUG", "9. Fragment: IsBlockedByException Doğrulandı! UI Engellendi Moduna Alınıyor. loadedKullanici: ${loadedUserModel?.username}")
                                profileViewModel.setBlockedState(loadedUserModel)
                                followActionViewModel.setBlockedByState(true)
                                setupBlockedByUi(loadedUserModel)
                            } else {
                                Log.d("FOLLOW_DEBUG", "9. Fragment: Genel Hata Bildirimi Gösteriliyor.")
                                UiMessageManager.emitMessage(UiMessageState.Error("İşlem gerçekleştirilemedi.",1500))
                            }
                        }
                    )
                }
            }
        }
        followersCountLayout.setOnClickListener {
            it.bounceAndHaptic()

            val followState = followActionViewModel.followState.value
            val canNavigate = followState.isSelfProfile == true || (followState.isFollowing && !followState.isBlocked)

            if (canNavigate) {
                targetUserId?.let { userId ->
                    val args = bundleOf(
                        "yukleyenID" to userId,
                        "startPage" to 0,
                        "kullaniciAdi" to usernameTextView.text.toString(),
                    )

                    SmartNavigationEngine.navigateTo(
                        Screen.FOLLOWERS,
                        args,
                        "${userId}_0"
                    )
                }
            }
        }

        followingCountLayout.setOnClickListener {
            it.bounceAndHaptic()

            val followState = followActionViewModel.followState.value
            val canNavigate = followState.isSelfProfile == true || (followState.isFollowing && !followState.isBlocked)

            if (canNavigate) {
                targetUserId?.let { userId ->
                    val args = bundleOf(
                        "yukleyenID" to userId,
                        "startPage" to 1,
                        "kullaniciAdi" to usernameTextView.text.toString(),
                    )

                    SmartNavigationEngine.navigateTo(
                        Screen.FOLLOWERS,
                        args,
                        "${userId}_1"
                    )
                }
            }
        }

        followingCountLayout.setOnClickListener {
            it.bounceAndHaptic()
            val followState = followActionViewModel.followState.value
            val canNavigate = followState.isSelfProfile == true || (followState.isFollowing && !followState.isBlocked)
            if(canNavigate) {
                targetUserId?.let { userId ->
                    val args = bundleOf(
                        "yukleyenID" to userId,
                        "startPage" to 1,
                        "kullaniciAdi" to usernameTextView.text.toString(),
                    )

                    SmartNavigationEngine.navigateTo(
                        Screen.FOLLOWERS,
                        args,
                        "${userId}_1"
                    )
                }
            }
        }

        followingButton.setOnClickListener {
            it.bounceAndHaptic()
            recyclerView.visibility= View.GONE
            postSectionHeader.visibility = View.GONE
            followButton.setLoadingState(true)
            targetUserId?.let { targetId ->
                if (targetId != myUserId) {
                    followActionViewModel.takiptenCikar(
                        takiptenCiktiginId = targetId,
                        onSuccess = {
                            followButton.setLoadingState(false)
                            viewModel.setAccessDenied(true)
                            val profile = (profileViewModel.fullProfileState.value as? UiState.Success)?.data?.profile
                            val userId = profile?.id.orEmpty().ifBlank { targetId }

                            ProfileEventBus.emitEvent(
                                ProfileEvent.UnFollowUser(
                                    userId = userId,
                                    operatorUserId = myUserId
                                )
                            )
                            profileViewModel.resetProfileState()
                        },
                        onFailure = {
                            followButton.setLoadingState(false)
                            progressFollow.visibility = View.GONE
                        }
                    )
                }
            }
        }

        unblockButton.setOnClickListener {
            it.bounceAndHaptic()
            showEngelKaldirDialog()
        }
        chatButton.setOnClickListener {
            targetUserId?.let {
                NavigationHelper.navigateToChat(it)
            }
        }

        profiliDuzenleTiklandi.setOnClickListener {
            it.bounceAndHaptic()
            SmartNavigationEngine.navigateTo(Screen.EDIT_PROFILE)
        }
        ppMenuButton.setOnClickListener { anchorView ->
            showOptionMenu(anchorView)
        }
        menuButtonEngel.setOnClickListener { anchorView ->
            showOptionMenu(anchorView)
        }
    }

    private fun showOptionMenu(view: View) {
        val isMyFollower = followActionViewModel.followState.value.isFollowed
        val isBlocked = userBlockViewModel.benimEngellediklerim.value.any { it.id == targetUserId }

        val redColor = ContextCompat.getColor(requireContext(), R.color.catmap_error)
        val textPrimaryColor = ContextCompat.getColor(requireContext(), R.color.catmap_text_primary)

        CatMapPopupMenu.Builder(requireContext())
            .addItem(
                id = R.id.profiltakipciCikar,
                title = "Takipçiden Çıkar",
                iconRes = R.drawable.ic_unfollow_user,
                textColor = textPrimaryColor,
                iconTint = textPrimaryColor,
                isVisible = isMyFollower && targetUserId != UserSession.userId
            ) {
                showTakipcidenCikarDialog()
            }
            .addItem(
                id = if (isBlocked) R.id.profilmenu_engeliKaldir else R.id.profilmenu_engelle,
                title = if (isBlocked) "Engeli Kaldır" else "Kullanıcıyı Engelle",
                iconRes = if (isBlocked) R.drawable.ic_lock_open else R.drawable.ic_lock,
                textColor = if (isBlocked) textPrimaryColor else redColor,
                iconTint = if (isBlocked) textPrimaryColor else redColor,
                isVisible = targetUserId != UserSession.userId
            ) {
                if (isBlocked) {
                    showEngelKaldirDialog()
                } else {
                    showEngelleDialog()
                }
            }
            .addItem(
                id = R.id.signOut,
                title = "Çıkış Yap",
                iconRes = R.drawable.logout,
                textColor = textPrimaryColor,
                iconTint = textPrimaryColor,
                isVisible = targetUserId == UserSession.userId
            ) {
                if (childFragmentManager.findFragmentByTag("SignOutDialog") == null) {
                    CatMapDialog.Companion.build()
                        .setTitle("Maceraya Mola mı?")
                        .setMessage("Dostlarımız haritada seni bekliyor olacak! Yine bekleriz, çıkış yapmak istediğine emin misin?")
                        .setPositiveButton("Evet, Çıkış Yap") {
                            logout()
                        }
                        .setNegativeButton("Kalıyorum")
                        .show(childFragmentManager, "SignOutDialog")
                }
            }.addItem(
                id = R.id.engellenenlerGetir,
                title = "Engellenenler",
                iconRes = R.drawable.exit,
                textColor = textPrimaryColor,
                iconTint = textPrimaryColor,
                isVisible = targetUserId == UserSession.userId
            ) {
                SmartNavigationEngine.navigateTo(Screen.BLOCKED_USERS)
            }
            .addItem(
                id = 3,
                title = "Profili Bildir",
                iconRes = R.drawable.ic_error_outline,
                textColor = redColor, iconTint = redColor,
                isVisible = targetUserId != UserSession.userId
            ) {
                targetUserId?.let {
                    NavigationHelper.showReportBottomSheet(
                        childFragmentManager,
                        targetId = it,
                        reportType = ReportType.PROFILE
                    )
                }
            }
            .addItem(
                id = 4,
                title = "Rozetler",
                iconRes = R.drawable.catmap_badge_tier_05,
                isVisible = targetUserId == UserSession.userId
            ){
                SmartNavigationEngine.navigateTo(
                    targetScreen = Screen.BADGE
                )
            }
            .build()
            .show(anchorView = view)
    }

    private fun showTakipcidenCikarDialog() {
        val currentProfile = loadedUserModel

        ProfileDialogHelper.showUnfollowerDialog(
            fragmentManager = childFragmentManager,
            kullaniciAdi = currentProfile?.username,
            onConfirm = {
                targetUserId?.let { id ->
                    followActionViewModel.takipcidenCikar(
                        takipciId = id,
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
        val currentProfile = loadedUserModel

        ProfileDialogHelper.showBlockDialog(
            fragmentManager = childFragmentManager,
            kullaniciAdi = currentProfile?.username,
            onConfirm = {
                currentProfile?.let { engellenecekKullanici ->

                    val wasIWasFollowing = followActionViewModel.followState.value.isFollowing
                    val wasHeWasFollowing = followActionViewModel.followState.value.isFollowed

                    userBlockViewModel.engelle(
                        engellenecekUserModel = engellenecekKullanici,
                        kisiId = myUserId,
                        onResult = { isSuccess ->
                            if (isSuccess) {
                                profileViewModel.setBlockedState(engellenecekKullanici)
                                followActionViewModel.applyBlockToTargetCounts(
                                    wasIWasFollowing = wasIWasFollowing,
                                    wasHeWasFollowing = wasHeWasFollowing
                                )
                                ProfileEventBus.emitEvent(
                                    ProfileEvent.BlockedUser(
                                        userId = currentProfile.id,
                                        kullaniciAdi = currentProfile.username,
                                        fotoUrl = currentProfile.photoUrl,
                                        operatorUserId = myUserId
                                    )
                                )
                            }
                        }
                    )
                }
            }
        )
    }
    private fun showEngelKaldirDialog() {
        val currentProfile = loadedUserModel

        ProfileDialogHelper.showUnblockDialog(
            fragmentManager = childFragmentManager,
            kullaniciAdi = currentProfile?.username,
            onConfirm = {
                targetUserId?.let { engellenenId ->
                    userBlockViewModel.engelKaldir(
                        engellenenKullaniciId = engellenenId,
                        kisiId = myUserId,
                        onResult = { isSuccess ->
                            if (isSuccess) {
                                profileViewModel.resetProfileState()
                                currentProfile?.let {
                                    ProfileEventBus.emitEvent(
                                        ProfileEvent.UnblockedUser(
                                            userId = it.id,
                                            operatorUserId = myUserId
                                        )
                                    )
                                }
                                followActionViewModel.setBlockedState(false)
                                viewModel.resetAndSetBlocked()
                            }
                        }
                    )
                }
            }
        )
    }
    private fun snapUiBlocked() {

        recyclerView.visibility = View.GONE
        postSectionHeader.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressFollow.visibility = View.GONE

        tvEmpty.text = "🚫 Bu kullanıcıyı engellediniz."
        tvEmpty.visibility = View.VISIBLE

        profiliDuzenleTiklandi.visibility = View.GONE
        followButton.visibility = View.GONE
        followingAndMessage.visibility = View.GONE
        unblockButton.visibility = View.VISIBLE
    }

    private fun setupBlockedByUi(userModel: UserModel?){
        this.loadedUserModel = userModel
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE

        myConstraintLayout.visibility = View.GONE
        blockedUserLayout.visibility = View.VISIBLE
        swipeRefreshLayout.isRefreshing = false
        usernameBlock.text = userModel?.username

        Glide.with(requireContext())
            .load(userModel?.photoUrl)
            .placeholder(R.drawable.kullanici)
            .error(R.drawable.kullanici)
            .into(profilePhotoBlock)


    }
    private fun setupBlockedByMeUi(userModel: UserModel?) {
        this.loadedUserModel = userModel
        shimmerLayout.stopShimmer()
        shimmerLayout.visibility = View.GONE
        swipeRefreshLayout.isRefreshing = false

        myConstraintLayout.visibility = View.VISIBLE
        blockedUserLayout.visibility = View.GONE

        userModel?.let {
            usernameTextView.text = it.username.orEmpty()
            tvAd.text = it.name.orEmpty().trim()
            bioTextView.text = it.bio.orEmpty()

            Glide.with(requireContext())
                .load(it.photoUrl)
                .placeholder(R.drawable.kullanici)
                .error(R.drawable.kullanici)
                .into(profilePhotoImageView)
        }

        recyclerView.visibility = View.GONE
        postSectionHeader.visibility = View.GONE
        progressBar.visibility = View.GONE
        progressFollow.visibility = View.GONE

        profiliDuzenleTiklandi.visibility = View.GONE
        followButton.visibility = View.GONE
        followingAndMessage.visibility = View.GONE
        unblockButton.visibility = View.VISIBLE

        tvEmpty.text = "🚫 Bu kullanıcıyı engellediniz."
        tvEmpty.visibility = View.VISIBLE
    }

    private fun yukleVerileri(forceRefresh: Boolean = false) {
        if (forceRefresh) {
            profileLoaded = false
        }
        targetUserId?.let { userId ->
            profileViewModel.allProfileLoad(targetUserId = userId, forceRefresh = forceRefresh)
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
                                WarningMessage.YuklemeDurum(state.message)
                            }
                            is BlockActionState.Success -> {
                                WarningMessage.BasariliDurum(state.message, 1500)

                            }
                            is BlockActionState.Error -> {
                                WarningMessage.BasarisizDurum(state.message, 1500)
                            }
                            is BlockActionState.Idle -> {

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

                                followActionViewModel.setupFromFullProfile(
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
                                setupBlockedByUi(state.profile)
                            }
                            is UiState.Blocked -> {
                                followActionViewModel.setBlockedState(true)
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
                                val guncelKullanici = event.updatedUserModel
                                guncelKullanici?.let { bindUserProfileData(it) }
                            }
                            is ProfileEvent.UnblockedUser -> {
                                if (event.userId == targetUserId) {
                                    profileViewModel.resetProfileState()
                                    followActionViewModel.setBlockedState(false)
                                    viewModel.resetAndSetBlocked()
                                }
                            }
                            else -> {}
                        }
                    }
                }
                launch {
                    followActionViewModel.followState.collectLatest { state ->
                        renderProfileButtons(state)
                    }
                }
                launch {
                    if (targetUserId == myUserId) {
                        profileViewModel.profileState.collect { profileState ->
                            followersCountTextView.text = profileState.followersCount.toString()
                            followingCountTextView.text = profileState.followingCount.toString()
                            postCountTextView.text = profileState.postCount.toString()
                        }
                    } else {
                        launch {
                            followActionViewModel.targetUserFollowersCount.collect { sayi ->
                                followersCountTextView.text = sayi.toString()
                            }
                        }
                        launch {
                            followActionViewModel.targetUserFollowingCount.collect { sayi ->
                                followingCountTextView.text = sayi.toString()
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

                        val currentProfileState = profileViewModel.fullProfileState.value

                        if (currentProfileState is UiState.Blocked || currentProfileState is UiState.BlockedBy) {
                            snapUiBlocked()
                            return@collect
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
                                }else {
                                    tvEmpty.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE
                                    postAdapter.submitList(state.posts) {
                                        recyclerView.scheduleLayoutAnimation()
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private fun bindUserProfileData(userModel: UserModel) {
        this.loadedUserModel = userModel
        usernameTextView.text = userModel.username

        val tamAd = userModel.name.trim()
        tvAd.text = tamAd

        bioTextView.text = userModel.bio

        postCountTextView.text = (userModel.postCount ?: 0L).toString()

        Glide.with(requireContext())
            .load(userModel.photoUrl)
            .placeholder(R.drawable.kullanici)
            .error(R.drawable.kullanici)
            .into(profilePhotoImageView)

        bindEquippedBadge(
            userModel.equippedBadge
        )
    }

    override fun onPause() {
        shimmerLayout.stopShimmer()
        super.onPause()
    }


    /**
     * şuan bu metodu öylesine oluşturdum sevgilim
     * çıkış işleminde bunlarda olması gerekiyor
     * aklımızda bulunsun diye şimdilik bu metodu yazdım
     * OnlinePresenceManager objesi çevrimiçi durumunu yönetiyor. <3<3<3
     */
    private fun logout() {
        // 💡 1. Coroutine Scope ile güvenli çıkış işlemi
        lifecycleScope.launch {
            // Varsa bir Loading Banner/Pill açabilirsin: "Oturum kapatılıyor..."

            try {
                OnlinePresenceManager.setUserOffline()
                UserSession.logout()

            } catch (e: Exception) {
                Log.e("LOGOUT_ERROR", "Çıkış esnasında temizlik hatası: ${e.localizedMessage}")
            } finally {
                navigateToAuthAndClearHistory()
            }
        }
    }
    private fun navigateToAuthAndClearHistory() {
        if (!isAdded || isStateSaved) return

        SmartNavigationEngine.resetEngineForLogout(
            navScreen = Screen.AUTH,
        )
    }


    private fun bindEquippedBadge(
        equippedBadge: EquippedBadgeModel?
    ) {
        if (
            equippedBadge == null ||
            !equippedBadge.isValid
        ) {
            cardEquippedBadge.visibility =
                View.GONE

            return
        }

        val tier = equippedBadge.tier

        if (tier == null) {
            cardEquippedBadge.visibility =
                View.GONE

            return
        }

        val context = requireContext()

        val accentColor =
            ContextCompat.getColor(
                context,
                tier.accentColorRes
            )

        val surfaceColor =
            ContextCompat.getColor(
                context,
                tier.pillBgColorRes
            )

        // =========================================================
        // GÖRÜNÜRLÜK
        // =========================================================

        cardEquippedBadge.visibility =
            View.VISIBLE

        // =========================================================
        // BADGE ICON
        // =========================================================

        imgEquippedBadge.setImageResource(
            tier.iconResId
        )

        // =========================================================
        // TITLE
        //
        // Yazır'ın Pati Koruyucusu
        // =========================================================

        val tierTitle =
            getString(
                tier.titleResId
            )

        tvEquippedBadgeTitle.text =
            if (equippedBadge.neighborhood.isNotBlank()) {

                "${equippedBadge.neighborhood.withPossessiveSuffix()} $tierTitle"

            } else {

                tierTitle
            }

        // =========================================================
        // NORMAL TIER TEMASI
        // =========================================================

        if (tier != BadgeTier.TIER_08) {

            cardEquippedBadge.setCardBackgroundColor(
                surfaceColor
            )

            cardEquippedBadge.strokeColor =
                accentColor

            tvEquippedBadgeTitle.setTextColor(
                accentColor
            )

        } else {

            // =====================================================
            // TIER 08 · GECE ALTINI
            // =====================================================

            val nightBackground =
                ContextCompat.getColor(
                    context,
                    R.color.badge_tier_08_detail_chip_bg
                )

            val goldStroke =
                ContextCompat.getColor(
                    context,
                    R.color.badge_tier_08_detail_chip_stroke
                )

            val goldText =
                ContextCompat.getColor(
                    context,
                    R.color.badge_tier_08_detail_chip_text
                )

            cardEquippedBadge.setCardBackgroundColor(
                nightBackground
            )

            cardEquippedBadge.strokeColor =
                goldStroke

            tvEquippedBadgeTitle.setTextColor(
                goldText
            )
        }

        // =========================================================
        // CLICK
        // =========================================================

        cardEquippedBadge.setOnClickListener { view ->

            view.bounceAndHaptic()

            BadgeStoryBottomSheet.show(
                fragmentManager =
                    childFragmentManager,

                equippedBadge =
                    equippedBadge
            )
        }
    }


    private fun renderProfileButtons(state: FollowState) {

        if (state.isBlocked) {
            profiliDuzenleTiklandi.visibility = View.GONE
            followButton.visibility = View.GONE
            followingAndMessage.visibility = View.GONE
            unblockButton.visibility = View.VISIBLE
            progressFollow.visibility = View.GONE
            return
        } else {
            unblockButton.visibility = View.GONE
        }

        if (state.isSelfProfile == null) {
            profiliDuzenleTiklandi.visibility = View.GONE
            followButton.visibility = View.GONE
            followingAndMessage.visibility = View.GONE
            progressFollow.visibility = View.VISIBLE
            return
        }

        progressFollow.visibility = View.GONE

        if (state.isSelfProfile == true) {
            profiliDuzenleTiklandi.visibility = View.VISIBLE
            followButton.visibility = View.GONE
            followingAndMessage.visibility = View.GONE
        } else {
            profiliDuzenleTiklandi.visibility = View.GONE

            if (state.isFollowing) {
                followingAndMessage.visibility = View.VISIBLE
                followButton.visibility = View.GONE

            } else if (state.isFollowed) {
                followButton.text = "Sen de takip et"
                followButton.visibility = View.VISIBLE
                followingAndMessage.visibility = View.GONE

            } else {
                followButton.text = "Takip et"
                followButton.visibility = View.VISIBLE
                followingAndMessage.visibility = View.GONE
            }
        }
    }
    private fun onGonderiTiklandi(post: Post) {
        val args = bundleOf(
            "kediid" to post.catId,
            "yukleyenId" to targetUserId
        )
        SmartNavigationEngine.navigateTo(Screen.POST, args, post.catId)
    }
    companion object {
        private const val ARG_USER_ID = "USER_ID"

        @JvmStatic
        fun newArgs(userId: String): Bundle {
            return bundleOf(ARG_USER_ID to userId)
        }
    }
}