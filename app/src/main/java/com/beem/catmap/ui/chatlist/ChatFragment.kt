package com.beem.catmap.ui.chatlist

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.beem.catmap.databinding.SohbetlerBinding
import com.beem.catmap.ui.navigation.NavigationHelper
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class ChatFragment : Fragment() {

    private var _binding: SohbetlerBinding? = null
    private val binding get() = _binding!!

    private val viewModel: RecentChatsViewModel by viewModels()
    private lateinit var adapter: RecentChatsAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SohbetlerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleBackPressWithEngine()

        setupRecyclerView()
        observeUiState()
    }

    private fun setupRecyclerView() {
        adapter = RecentChatsAdapter { selectedChat ->
            Log.d("RecentChatDebug", "receiver user id: ${selectedChat.otherUserId}")
            NavigationHelper.navigateToChat(selectedChat.otherUserId)
        }
        binding.kisilerRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@ChatFragment.adapter
        }
    }

    private fun observeUiState() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collectLatest { state ->
                    adapter.submitList(state.chats)
                    binding.shimmerLayout.isVisible = state.isLoading
                    if (state.isLoading) {
                        binding.shimmerLayout.startShimmer()
                    } else {
                        binding.shimmerLayout.stopShimmer()
                    }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}