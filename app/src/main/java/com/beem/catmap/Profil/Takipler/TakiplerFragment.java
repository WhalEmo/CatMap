package com.beem.catmap.Profil.Takipler;

import static com.beem.catmap.ui.navigation.NavigationExtensionsKt.handleBackPressWithEngine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.beem.catmap.MainActivity;
import com.beem.catmap.R;
import com.beem.catmap.data.repository.UserRepository;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TakiplerFragment extends Fragment {
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_takipler, container, false);
        TabLayout tabLayout=view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager2=view.findViewById(R.id.viewPager);

        String yukleyenID = null;
        int startPage = 0;

        if (getArguments() != null) {
            yukleyenID = getArguments().getString("yukleyenID");
            startPage = getArguments().getInt("startPage", 0);
        }

        UserRepository userRepository = UserRepository.Companion.getInstance(requireContext());

        if (yukleyenID == null || yukleyenID.isEmpty()) {
            yukleyenID = userRepository.getCurrentUserId();
        }

        TakipViewPagerAdapter takipViewPagerAdapter=new TakipViewPagerAdapter(this,yukleyenID);
        viewPager2.setAdapter(takipViewPagerAdapter);

        viewPager2.setSaveEnabled(false);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {//secim yapılınca kaydırmayı ve gecisi saglar
            tab.setText(position == 0 ? "Takipçiler" : "Takipler");//tablnun baslıkları ayarlanır
        }).attach();

        final int finalStartPage = startPage;
        viewPager2.post(() -> viewPager2.setCurrentItem(finalStartPage, false));

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleBackPressWithEngine(this);
    }
}
