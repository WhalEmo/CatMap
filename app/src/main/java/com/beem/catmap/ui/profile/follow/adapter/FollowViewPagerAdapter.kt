package com.beem.catmap.ui.profile.follow.adapter

import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.beem.catmap.ui.profile.follow.fragment.FollowersFragment
import com.beem.catmap.ui.profile.follow.fragment.FollowingFragment

class FollowViewPagerAdapter(
    fragment: Fragment,
    private val profilID: String
) : FragmentStateAdapter(fragment) {

    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return if (position == 0) {
            FollowersFragment.Companion.newInstance(profilID)
        } else {
            FollowingFragment.Companion.newInstance(profilID)
        }
    }
}