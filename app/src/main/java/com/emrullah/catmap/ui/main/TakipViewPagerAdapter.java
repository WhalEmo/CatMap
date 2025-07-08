package com.emrullah.catmap.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TakipViewPagerAdapter extends FragmentStateAdapter {
    private final String profilID;
    public TakipViewPagerAdapter(@NonNull FragmentActivity fragmentActivity, String profilID) {
        super(fragmentActivity);
        this.profilID = profilID;
    }

    public TakipViewPagerAdapter(@NonNull Fragment fragment,String profilID) {
        super(fragment);
        this.profilID = profilID;
    }

    public TakipViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle, String profilID) {
        super(fragmentManager, lifecycle);
        this.profilID = profilID;
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        if (position == 0) {
            return TakipcilerFragment.newInstance(profilID);
        } else {
            return TakipEdilenlerFragment.newInstance(profilID);
        }
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
