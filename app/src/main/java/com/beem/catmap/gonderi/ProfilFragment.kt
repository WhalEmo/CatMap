package com.beem.catmap.Profil

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
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
import com.beem.catmap.gonderi.PostViewModel
import com.beem.catmap.gonderi.UiState
import com.beem.catmap.models.Gonderi
import com.beem.catmap.ui.navigation.Screen
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import kotlinx.coroutines.launch

class ProfilFragment : Fragment() {

    private val viewModel: PostViewModel by viewModels()

    // XML ID Bağlantıları
    private lateinit var recyclerView: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var tvGonderiSayisi: TextView
    private lateinit var tvEmpty: TextView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var btnBack: ImageButton

    private lateinit var gonderiAdapter: GonderiAdapter
    private var currentUserId: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            currentUserId = it.getString(ARG_USER_ID)
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

        recyclerView = view.findViewById(R.id.gonderiRecyclerView)
        progressBar = view.findViewById(R.id.progressBarr)
        tvGonderiSayisi = view.findViewById(R.id.gonderiSayisiTextView)
        tvEmpty = view.findViewById(R.id.emptyTextView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        btnBack = view.findViewById(R.id.btnBack)

        setupRecyclerView()
        setupListeners()
        observeViewModel()

        yukleVerileri()
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
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        swipeRefreshLayout.setOnRefreshListener {
            yukleVerileri()
        }
    }

    private fun yukleVerileri() {
        currentUserId?.let { userId ->
            viewModel.gonderileriGetir(userId)
        } ?: run {
            swipeRefreshLayout.isRefreshing = false
        }
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

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
                                    tvEmpty.visibility = View.GONE
                                    recyclerView.visibility = View.VISIBLE
                                    gonderiAdapter.submitList(state.data)
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

                launch {
                    viewModel.gonderiSayisi.collect { sayi ->
                        tvGonderiSayisi.text = sayi.toString()
                    }
                }
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