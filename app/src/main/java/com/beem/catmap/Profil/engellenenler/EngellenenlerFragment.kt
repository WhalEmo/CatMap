package com.beem.catmap.Profil.engellenenler

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.gonderi.ProfileDialogHelper
import com.beem.catmap.ui.extensions.bounceAndHaptic
import com.beem.catmap.ui.manager.ProfileEvent
import com.beem.catmap.ui.manager.ProfileEventBus
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.viewmodel.UserBlockViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EngellenenlerFragment : Fragment() {

    private val viewModel: UserBlockViewModel by activityViewModels()
    private lateinit var adapter: EngellenenlerAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton
    private lateinit var tvEmpty: TextView

    private val currentUserId: String = UserSession.userId

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.engellenenler, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        initViews(view)
        setupListeners()
        setupRecyclerView()
        observeViewModel()

        showShimmer()
        viewModel.benimEngellediklerimiGetir(currentUserId)
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.engellenenRecyclerView)
        shimmerFrameLayout = view.findViewById(R.id.engellenenShimmer)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        btnBack = view.findViewById(R.id.btnBack)
        tvEmpty = view.findViewById(R.id.tvEmpty)
    }

    private fun setupListeners() {
        btnBack.setOnClickListener {
            it.bounceAndHaptic()
            SmartNavigationEngine.navigateBack()
        }

        swipeRefreshLayout.setOnRefreshListener {
            viewModel.benimEngellediklerimiGetir(currentUserId)
        }
    }

    private fun showShimmer() {
        if (!swipeRefreshLayout.isRefreshing) {
            shimmerFrameLayout.visibility = View.VISIBLE
            shimmerFrameLayout.startShimmer()
            recyclerView.visibility = View.GONE
        }
    }

    private fun hideShimmer() {
        shimmerFrameLayout.stopShimmer()
        shimmerFrameLayout.visibility = View.GONE
    }

    private fun setupRecyclerView() {
        adapter = EngellenenlerAdapter(
            onEngelClick = { kullanici ->
                kullanici.id?.let { engellenenId ->
                    ProfileDialogHelper.showEngelKaldirDialog(
                        fragmentManager = childFragmentManager,
                        kullaniciAdi = kullanici.kullaniciAdi,
                        onConfirm = {
                            viewModel.engelKaldir(
                                engellenenKullaniciId = engellenenId,
                                kisiId = currentUserId,
                                onResult = { isSuccess ->
                                    if (isSuccess) {
                                        ProfileEventBus.emitEvent(
                                            ProfileEvent.UnblockedUser(
                                                userId = engellenenId,
                                                operatorUserId = currentUserId
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    )
                }
            },
            onKullaniciClick = { kullaniciId ->
                SmartNavigationEngine.navigateTo(
                    Screen.PROFILE,
                    bundleOf("ARG_USER_ID" to kullaniciId)
                )
            }
        )

        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val visibleItemCount = layoutManager.childCount
                val totalItemCount = layoutManager.itemCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()

                if (!viewModel.isLoadingMore.value && !viewModel.isLastPage.value) {
                    if ((visibleItemCount + firstVisibleItemPosition) >= totalItemCount
                        && firstVisibleItemPosition >= 0
                    ) {
                        viewModel.dahaFazlaEngellenenGetir(currentUserId)
                    }
                }
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.benimEngellediklerim.collectLatest { blockedList ->
                    hideShimmer()
                    swipeRefreshLayout.isRefreshing = false

                    adapter.submitList(blockedList) {
                        if (blockedList.isEmpty()) {
                            recyclerView.visibility = View.GONE
                            tvEmpty.visibility = View.VISIBLE
                        } else {
                            recyclerView.visibility = View.VISIBLE
                            tvEmpty.visibility = View.GONE
                        }
                    }
                }
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.isLoading.collectLatest { isLoading ->
                    if (!isLoading) {
                        swipeRefreshLayout.isRefreshing = false
                        hideShimmer()
                    }
                }
            }
        }
    }
}