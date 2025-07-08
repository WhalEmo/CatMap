package com.emrullah.catmap.ui.main;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FotoAdapter extends RecyclerView.Adapter<FotoAdapter.FotoViewHolder> {

    private ArrayList<String> fotoUrlListesi;

    public FotoAdapter(ArrayList<String> fotoUrlListesi) {
        this.fotoUrlListesi = fotoUrlListesi;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.foto_item, parent, false);
        return new FotoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        String url = fotoUrlListesi.get(position);
        Picasso.get()
                .load(url)
                .fit()
                .centerCrop()
                .placeholder(R.drawable.kullanici)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return fotoUrlListesi.size();
    }

    static class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;

        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.fotoImageView);
        }
    }
}


