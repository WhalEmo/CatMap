package com.emrullah.catmap.sohbet;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

import androidx.activity.OnBackPressedCallback;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.facebook.shimmer.ShimmerFrameLayout;

import java.util.ArrayList;

public class SohbetFragment extends Fragment {

    private RecyclerView kisilerRecyclerView;
    private SohbetAdapter adapter;
    private ArrayList<Sohbet> sohbetler;
    private Runnable MesajFragment;
    private SohbetYonetici sohbetYonetici = SohbetYonetici.getInstance();
    private ShimmerFrameLayout shimmerLayout;


    public SohbetFragment(Runnable MesajFragment) {
        this.MesajFragment = MesajFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sohbetler, container, false);
        sohbetler = new ArrayList<>();
        adapter = new SohbetAdapter(sohbetler, getActivity(),MesajFragment);
        shimmerLayout = view.findViewById(R.id.shimmerLayout);
        kisilerRecyclerView = view.findViewById(R.id.kisilerRecyclerView);
        kisilerRecyclerView.setAdapter(adapter);
        sohbetYonetici.SohbetleriCek(sohbetler, ()->{
            adapter.notifyDataSetChanged();
            shimmerLayout.setVisibility(View.GONE);
        });

        requireActivity().getOnBackPressedDispatcher().addCallback(getViewLifecycleOwner(), new OnBackPressedCallback(true){
            @Override
            public void handleOnBackPressed() {
                if(requireActivity() instanceof MainActivity){
                    ConstraintLayout ustCubuk = requireActivity().findViewById(R.id.ustCubuk);
                    ustCubuk.setVisibility(View.VISIBLE);
                }
                requireActivity().getSupportFragmentManager().popBackStack();
            }
        });

        return view;
    }

}
