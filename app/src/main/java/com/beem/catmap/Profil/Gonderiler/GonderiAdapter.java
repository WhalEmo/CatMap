package com.beem.catmap.Profil.Gonderiler;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.R;
import com.beem.catmap.ui.navigation.Screen;
import com.beem.catmap.ui.navigation.SmartNavigationEngine;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class GonderiAdapter extends RecyclerView.Adapter<GonderiAdapter.GonderiViewHolder> {

    private ArrayList<Gonderi> gonderiler;
    public Boolean gerigitti=true;

    public GonderiAdapter(ArrayList<Gonderi> gonderiler, Boolean gerigitti) {
        this.gonderiler = gonderiler;
        this.gerigitti=gerigitti;
    }

    public void guncelleList(ArrayList<Gonderi> yeniListe) {
        this.gonderiler = yeniListe;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public GonderiViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.gonderigriditem, parent, false);
        return new GonderiViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GonderiViewHolder holder, int position) {
        Gonderi gonderi = gonderiler.get(position);

        holder.itemView.setOnClickListener(v -> {
            gerigitti=true;


            Bundle args = GonderiDetayFragment.newBundle(
                    new ArrayList<>(gonderi.getFotoUrlListesi()),
                    gonderi.getKediAdi(),
                    gonderi.getAciklama(),
                    gonderi.getBegeniSayisi(),
                    gonderi.getKediID()
            );
            SmartNavigationEngine.navigateTo(Screen.POST, args, gonderi.getKediID());

        });

        Picasso.get()
                .load(gonderi.getFotoUrlListesi().get(0))
                .fit()
                .centerCrop()
                .placeholder(R.drawable.kullanici)
                .into(holder.gonderiResmi);
    }

    @Override
    public int getItemCount() {
        return gonderiler.size();
    }

    static class GonderiViewHolder extends RecyclerView.ViewHolder {
        ImageView gonderiResmi;
        public GonderiViewHolder(@NonNull View itemView) {
            super(itemView);
            gonderiResmi = itemView.findViewById(R.id.gonderiResmi);
        }
    }
}
