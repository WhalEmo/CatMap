package com.beem.catmap.sohbet;

import static com.beem.catmap.ui.navigation.NavigationExtensionsKt.handleBackPressWithEngine;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.Maps.MapsActivity;
import com.beem.catmap.R;
import com.beem.catmap.ui.navigation.NavigationHelper;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;

public class SohbetFragment extends Fragment {

    private RecyclerView kisilerRecyclerView;
    private SohbetAdapter adapter;
    private ArrayList<Sohbet> sohbetler;
    private SohbetYonetici sohbetYonetici = SohbetYonetici.getInstance();
    private ShimmerFrameLayout shimmerLayout;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.sohbetler, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        handleBackPressWithEngine(this);

        sohbetler = new ArrayList<>();
        adapter = new SohbetAdapter(sohbetler, getActivity());
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        kisilerRecyclerView = view.findViewById(R.id.kisilerRecyclerView);
        kisilerRecyclerView.setAdapter(adapter);
        sohbetYonetici.SohbetleriCek(sohbetler, ()->{
            adapter.notifyDataSetChanged();
            shimmerLayout.setVisibility(View.GONE);
        });

        adapter.setOnSohbetClickListener(sohbet ->{
            NavigationHelper.navigateToChat(sohbet.getAlici().getID());
            /*
            if (getActivity() instanceof MapsActivity) {
                SmartNavigationEngine.navigateTo(
                        Screen.MESSAGE,
                        null,
                        sohbet.getSohbetID()
                );
            }

             */
        });
    }
}
