package com.emrullah.catmap.ui.main;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.viewpager.widget.ViewPager;
import androidx.viewpager2.widget.ViewPager2;

import com.emrullah.catmap.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

public class TakiplerFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_takipler, container, false);
        TabLayout tabLayout=view.findViewById(R.id.tabLayout);
        ViewPager2 viewPager2=view.findViewById(R.id.viewPager);

        String yukleyenID = null;
        if (getArguments() != null) {
            yukleyenID = getArguments().getString("yukleyenID");
        }

        TakipViewPagerAdapter takipViewPagerAdapter=new TakipViewPagerAdapter(this,yukleyenID);
        viewPager2.setAdapter(takipViewPagerAdapter);

        new TabLayoutMediator(tabLayout, viewPager2, (tab, position) -> {//secim yapılınca kaydırmayı ve gecisi saglar
            tab.setText(position == 0 ? "Takipçiler" : "Takipler");//tablnun baslıkları ayarlanır
        }).attach();

        //başlangıç sekmesini ayarlıyoruz:
        if (getArguments() != null) {
            int startPage = getArguments().getInt("startPage", 0);
            viewPager2.setCurrentItem(startPage, false);
        }

        return view;
    }
}
