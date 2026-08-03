package com.beem.catmap.Profil.Takipler

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter

class TakipViewPagerAdapter(
    fragment: Fragment,
    private val profilID: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            TakipcilerFragment.newInstance(profilID)
        } else {
            TakipEdilenlerFragment.newInstance(profilID)
        }
    }
}