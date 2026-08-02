package com.beem.catmap.Profil.Takipler

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
import com.beem.catmap.ui.navigation.SmartNavigationEngine
import com.beem.catmap.ui.navigation.handleBackPressWithEngine
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator

class TakiplerFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_takipler, container, false)
        val tabLayout: TabLayout = view.findViewById(R.id.tabLayout)
        val viewPager2: ViewPager2 = view.findViewById(R.id.viewPager)
        val btnBack: ImageView = view.findViewById(R.id.btnBack)
        val tvHeaderTitle: TextView = view.findViewById(R.id.tvHeaderTitle)

        val currentUserManager = CurrentUserManager.getInstance(requireContext())

        val yukleyenID = arguments?.getString("yukleyenID") ?: currentUserManager.getCurrentUserId()
        val startPage = arguments?.getInt("startPage", 0) ?: 0
        val kullaniciAdi = arguments?.getString("kullaniciAdi")

        // Başlığı ayarla (Gelen kullanıcı adı boş değilse yazdır, boşsa varsayılan yap)
        if (!kullaniciAdi.isNullOrEmpty()) {
            tvHeaderTitle.text = kullaniciAdi
        } else {
            tvHeaderTitle.text = "Kullanıcı"
        }

        viewPager2.adapter = TakipViewPagerAdapter(this, yukleyenID.orEmpty())
        viewPager2.isSaveEnabled = false

        // Tab bağlama
        TabLayoutMediator(tabLayout, viewPager2) { tab, position ->
            tab.text = if (position == 0) "Takipçiler" else "Takip Edilenler"
        }.attach()

        viewPager2.setCurrentItem(startPage, false)

        // Geri butonu dinleyicisi
        btnBack.setOnClickListener {
            SmartNavigationEngine.navigateBack()
        }

        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        handleBackPressWithEngine()
    }
}