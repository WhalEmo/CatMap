package com.emrullah.catmap.ui.main;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.Kullanici;
import com.emrullah.catmap.R;
import com.squareup.picasso.Picasso;

import java.util.List;

public class Kullanicilar_adapter extends RecyclerView.Adapter<Kullanicilar_adapter.ViewHolder> {

    private Context context;
    private List<Kullanici> kullaniciList; // Örnek olarak String liste, kendi modelin olabilir

    public Kullanicilar_adapter(Context context, List<Kullanici> kullaniciList) {
        this.context = context;
        this.kullaniciList = kullaniciList;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView recyclerFotoImageView;
        TextView RecyclerkullaniciAdi;
        Button takippet;
        Button takipediyosa;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            recyclerFotoImageView=itemView.findViewById(R.id.recyclerFotoImageView);
            RecyclerkullaniciAdi=itemView.findViewById(R.id.RecyclerkullaniciAdi);
            takippet=itemView.findViewById(R.id.takippet);
            takipediyosa=itemView.findViewById(R.id.takipediyosa);
        }
    }

    @NonNull
    @Override
    public Kullanicilar_adapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_profil_icin, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Kullanicilar_adapter.ViewHolder holder, int position) {
        Kullanici kullanici = kullaniciList.get(position);
        holder.RecyclerkullaniciAdi.setText(kullanici.getKullaniciAdi());
        Picasso.get()
                .load(kullanici.getFotoUrl())
                .fit()
                .centerCrop()
                .placeholder(R.drawable.kullanici)
                .into(holder.recyclerFotoImageView);
    }

    @Override
    public int getItemCount() {
        return kullaniciList.size();
    }
}
