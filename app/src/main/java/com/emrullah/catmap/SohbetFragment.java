package com.emrullah.catmap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

public class SohbetFragment extends Fragment {

    private RecyclerView kisilerRecyclerView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.sohbetler, container, false);

        kisilerRecyclerView = view.findViewById(R.id.kisilerRecyclerView);

        return view;
    }
}
