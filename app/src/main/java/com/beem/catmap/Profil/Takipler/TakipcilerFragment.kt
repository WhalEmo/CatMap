package com.beem.catmap.Profil.Takipler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ConcatAdapter
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.navigation.NavigationHelper

import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.launch

class TakipcilerFragment : Fragment() {

    private val viewModel: TakiplerViewModel by viewModels()
    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerContainer: ShimmerFrameLayout
    private lateinit var swipeRefresh: SwipeRefreshLayout

    private lateinit var userAdapter: KullanicilarAdapter
    private lateinit var footerAdapter: FooterAdapter

    private var targetId: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_takipciler, container, false)
        shimmerContainer = view.findViewById(R.id.shimmerViewContainer)
        recyclerView = view.findViewById(R.id.recyclerViewTakipciler)
        swipeRefresh = view.findViewById(R.id.swipeRefreshTakipciler)

        setupRecyclerView()
        setupSwipeRefresh()
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        targetId = arguments?.getString(ARG_PROFIL_ID)
            ?: arguments?.getString("yukleyenID")
                    ?: CurrentUserManager.getInstance(requireContext()).getCurrentUserId()

        observeState()
        viewModel.fetchTakipciler(targetId)
    }

    private fun setupSwipeRefresh() {
        swipeRefresh.setOnRefreshListener {
            viewModel.fetchTakipciler(targetId, isRefresh = true)
        }
    }

    private fun setupRecyclerView() {
        userAdapter = KullanicilarAdapter { kullaniciId ->
            kullaniciId?.let {
                val myUserId = UserSession.userId
                val isMyFollower = (targetId == myUserId)
                NavigationHelper.navigateToProfile(kullaniciId,isFollowed = isMyFollower)
            }
        }

        footerAdapter = FooterAdapter()
        val concatAdapter = ConcatAdapter(userAdapter, footerAdapter)

        val layoutManager = LinearLayoutManager(context)
        recyclerView.layoutManager = layoutManager
        recyclerView.adapter = concatAdapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                if (dy > 0) {
                    val visibleItemCount = layoutManager.childCount
                    val totalItemCount = layoutManager.itemCount
                    val pastVisibleItems = layoutManager.findFirstVisibleItemPosition()

                    if ((visibleItemCount + pastVisibleItems) >= totalItemCount - 3) {
                        viewModel.fetchTakipciler(targetId, isNextPage = true)
                    }
                }
            }
        })
    }

    private fun observeState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    viewModel.takipcilerState.collect { state ->
                        when (state) {
                            is TakiplerUiState.Loading -> {
                                shimmerContainer.isVisible = true
                                shimmerContainer.startShimmer()
                                recyclerView.isVisible = false
                            }

                            is TakiplerUiState.Success -> {
                                // DÜZELTME 2: Refresh çemberi kapatılıyor
                                swipeRefresh.isRefreshing = false

                                shimmerContainer.stopShimmer()
                                shimmerContainer.isVisible = false
                                recyclerView.isVisible = true

                                userAdapter.submitList(state.kullanicilar)
                                footerAdapter.setLoading(state.isLoadingMore)
                            }

                            is TakiplerUiState.Error -> {
                                swipeRefresh.isRefreshing = false

                                shimmerContainer.stopShimmer()
                                shimmerContainer.isVisible = false
                                recyclerView.isVisible = true
                                footerAdapter.setLoading(false)
                            }

                            else -> {}
                        }
                    }
                }

            }
        }
    }

    companion object {
        private const val ARG_PROFIL_ID = "profilID"

        @JvmStatic
        fun newInstance(profilID: String) = TakipcilerFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_PROFIL_ID, profilID)
            }
        }
    }
}