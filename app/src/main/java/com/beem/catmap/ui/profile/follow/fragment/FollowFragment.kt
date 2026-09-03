package com.beem.catmap.ui.profile.follow.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import com.beem.catmap.R
import com.beem.catmap.data.session.CurrentUserManager
import com.beem.catmap.ui.extensions.bounceAndHaptic
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.beem.catmap.ui.profile.follow.adapter.FollowViewPagerAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class FollowFragment : Fragment() {

    private var startPage = 0
    private lateinit var viewPager2: ViewPager2

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_takipler, container, false)
        val tabLayout: TabLayout = view.findViewById(R.id.tabLayout)
        viewPager2 = view.findViewById(R.id.viewPager)
        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val tvHeaderTitle: TextView = view.findViewById(R.id.tvHeaderTitle)

        val currentUserManager = CurrentUserManager.getInstance(requireContext())

        val yukleyenID = arguments?.getString(ARG_USER_ID) ?: currentUserManager.getCurrentUserId()
         startPage = arguments?.getInt(ARG_START_PAGE, 0) ?: 0
        val kullaniciAdi = arguments?.getString(ARG_USERNAME)

        // Başlığı ayarla
        if (!kullaniciAdi.isNullOrEmpty()) {
            tvHeaderTitle.text = kullaniciAdi
        } else {
            tvHeaderTitle.text = "Kullanıcı"
        }

        viewPager2.adapter = FollowViewPagerAdapter(this, yukleyenID.orEmpty())
        viewPager2.isSaveEnabled = false

        // Tab bağlama
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = if (position == 0) "Takipçiler" else "Takip Edilenler"
        }.attach()

        viewPager2.post {
            viewPager2.setCurrentItem(startPage, false)
        }

        btnBack.setOnClickListener {
            it.bounceAndHaptic()
            SmartNavigationEngine.navigateBack()
        }

        return view
    }
    override fun onResume() {
        super.onResume()

        startPage = arguments?.getInt(ARG_START_PAGE, 0) ?: 0
        viewPager2.setCurrentItem(startPage, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleBackPressWithEngine()
    }

    companion object {
        private const val ARG_USER_ID = "ARG_USER_ID"
        private const val ARG_USERNAME = "ARG_USERNAME"
        private const val ARG_START_PAGE = "ARG_START_PAGE"

        const val PAGE_FOLLOWERS = 0
        const val PAGE_FOLLOWING = 1

        fun newArgs(
            userId: String? = null,
            username: String? = null,
            startPage: Int = PAGE_FOLLOWERS
        ): Bundle = androidx.core.os.bundleOf(
            ARG_USER_ID to userId,
            ARG_USERNAME to username,
            ARG_START_PAGE to startPage
        )

        fun newInstance(
            userId: String? = null,
            username: String? = null,
            startPage: Int = PAGE_FOLLOWERS
        ): FollowFragment = FollowFragment().apply {
            arguments = newArgs(userId, username, startPage)
        }
    }

}