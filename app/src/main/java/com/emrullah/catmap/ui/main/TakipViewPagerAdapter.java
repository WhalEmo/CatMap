package com.emrullah.catmap.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class TakipViewPagerAdapter extends FragmentStateAdapter {
    public TakipViewPagerAdapter(@NonNull FragmentActivity fragmentActivity) {
        super(fragmentActivity);
    }

    public TakipViewPagerAdapter(@NonNull Fragment fragment) {
        super(fragment);
    }

    public TakipViewPagerAdapter(@NonNull FragmentManager fragmentManager, @NonNull Lifecycle lifecycle) {
        super(fragmentManager, lifecycle);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        return position == 0 ? new TakipcilerFragment() : new TakipEdilenlerFragment();
    }

    @Override
    public int getItemCount() {
        return 2;
    }
}
