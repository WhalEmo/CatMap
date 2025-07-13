package com.emrullah.catmap.ui.main;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.FotoYuklemeListener;
import com.emrullah.catmap.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class FotoAdapter extends RecyclerView.Adapter<FotoAdapter.FotoViewHolder> {

    private ArrayList<String> fotoUrlListesi;
    private FotoYuklemeListener listener;
    private int yuklenenFotoSayisi = 0;

    public FotoAdapter(ArrayList<String> fotoUrlListesi, FotoYuklemeListener listener) {
        this.fotoUrlListesi = fotoUrlListesi;
        this.listener = listener;
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
                .into(holder.imageView, new com.squareup.picasso.Callback() {
                    @Override
                    public void onSuccess() {
                        yuklenenFotoSayisi++;
                        if (yuklenenFotoSayisi == fotoUrlListesi.size()) {
                            if (listener != null) {
                                listener.onTumFotograflarYuklendi();
                            }
                        }
                    }

                    @Override
                    public void onError(Exception e) {
                        yuklenenFotoSayisi++;
                        if (yuklenenFotoSayisi == fotoUrlListesi.size()) {
                            if (listener != null) {
                                listener.onTumFotograflarYuklendi();
                            }
                        }
                    }
                });
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


