package com.beem.catmap.Profil.engellenenler
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.beem.catmap.R
import com.beem.catmap.data.local.UserSession
import com.beem.catmap.ui.viewmodel.UserBlockViewModel
import com.facebook.shimmer.ShimmerFrameLayout
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class EngellenenlerFragment : Fragment() {

    private val viewModel: UserBlockViewModel by activityViewModels()
    private lateinit var adapter: EngellenenlerAdapter

    private lateinit var recyclerView: RecyclerView
    private lateinit var shimmerFrameLayout: ShimmerFrameLayout

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
        setupRecyclerView()
        observeViewModel()


        viewModel.benimEngellediklerimiGetir(currentUserId)
    }

    private fun initViews(view: View) {
        recyclerView = view.findViewById(R.id.engellenenRecyclerView)
        shimmerFrameLayout = view.findViewById(R.id.engellenenShimmer)
    }

    private fun setupRecyclerView() {
        adapter = EngellenenlerAdapter(
            onEngelClick = { kullanici ->
                kullanici.id?.let { engellenenId ->
                    viewModel.engelKaldir(
                        engellenenKullaniciId = engellenenId,
                        kisiId = currentUserId,
                        onResult = { isSuccess ->
                            if (isSuccess) {
                                // Liste zaten ViewModel içindeki `_benimEngellediklerim` state'inden
                                // güncellendiği için adapter listen otomatik yenilenecektir.
                            }
                        }
                    )
                }
            },
            onKullaniciClick = { kullaniciId ->
                // Kullanıcı profiline gitme işlemleri
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
                    adapter.submitList(blockedList)
                    shimmerFrameLayout.stopShimmer()
                    shimmerFrameLayout.visibility = View.GONE
                    recyclerView.visibility = View.VISIBLE
                }
            }
        }
    }
}