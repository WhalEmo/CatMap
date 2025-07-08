package com.emrullah.catmap.ui.main;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.emrullah.catmap.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class GonderiAdapter extends RecyclerView.Adapter<GonderiAdapter.GonderiViewHolder> {

    private ArrayList<Gonderi> gonderiler;
    private FragmentManager fragmentManager;
    public Boolean gerigitti=true;

    public GonderiAdapter(ArrayList<Gonderi> gonderiler,FragmentManager fragmentManager,Boolean gerigitti) {
        this.gonderiler = gonderiler;
        this.fragmentManager = fragmentManager;
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
            FragmentManager fm = fragmentManager;
            FragmentTransaction transaction = fm.beginTransaction();

            Fragment mevcutFragment = fm.findFragmentById(R.id.container);
            if (mevcutFragment != null) {
                transaction.hide(mevcutFragment);
            }

            Fragment fragment = GonderiDetayFragment.newInstance(
                    new ArrayList<>(gonderi.getFotoUrlListesi()),
                    gonderi.getKediAdi(),
                    gonderi.getAciklama()
            );

            transaction
                    .add(R.id.container, fragment)
                    .addToBackStack(null)
                    .commit();
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
