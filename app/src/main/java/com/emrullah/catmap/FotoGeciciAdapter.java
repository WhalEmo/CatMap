package com.emrullah.catmap;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class FotoGeciciAdapter extends RecyclerView.Adapter<FotoGeciciAdapter.FotoHolder> {
    private Context baglanti;
    private ArrayList<Uri> fotolar;
    private FotografYukleyiciYonetici yukleyici;

    public FotoGeciciAdapter(Context baglanti, ArrayList<Uri> fotolar,FotografYukleyiciYonetici yukleyici) {
        this.baglanti = baglanti;
        this.fotolar = fotolar;
        this.yukleyici = yukleyici;
    }

    public void setFotolar(ArrayList<Uri> fotolar) {
        this.fotolar = fotolar;
    }

    @NonNull
    @Override
    public FotoHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View foto = LayoutInflater.from(baglanti).inflate(R.layout.foto, parent, false);
        return new FotoHolder(foto);
    }

    @Override
    public void onBindViewHolder(@NonNull FotoHolder holder, int position) {
        if(yukleyici==null){
            Uri fotoUri = fotolar.get(position);
            holder.foto.setImageURI(fotoUri);
        }
        else {
            Uri fotoUri = fotolar.get(position);
            yukleyici.Yukleyici(fotoUri, holder.foto, holder.yukleniyorOverlay, holder.yukleniyorProgressBar);
        }
        holder.foto.setOnLongClickListener(v->{
            if(yukleyici==null) {
                if (holder.sil.getVisibility() == View.GONE) {
                    holder.sil.setVisibility(View.VISIBLE);
                } else {
                    holder.sil.setVisibility(View.GONE);
                }
            }
            return true;
        });

        holder.sil.setOnClickListener(v ->{
            fotolar.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, fotolar.size());
            holder.sil.setVisibility(View.GONE);
            if(fotolar.size()==0){
                holder.foto.setImageResource(R.drawable.yuklemefotosu);
            }
        });
    }

    @Override
    public int getItemCount() {
        return fotolar.size();
    }

    public static class FotoHolder extends RecyclerView.ViewHolder{
        ImageView foto;
        Button sil;
        View yukleniyorOverlay;
        ProgressBar yukleniyorProgressBar;
        public FotoHolder(@NonNull View itemView) {
            super(itemView);
            foto = itemView.findViewById(R.id.fotoView);
            sil = itemView.findViewById(R.id.silButton);
            yukleniyorOverlay = itemView.findViewById(R.id.loadingOverlay);
            yukleniyorProgressBar = itemView.findViewById(R.id.loadingProgressBar);
        }
    }
}
