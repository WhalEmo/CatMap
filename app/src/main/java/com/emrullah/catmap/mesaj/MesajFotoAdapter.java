package com.emrullah.catmap.mesaj;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.emrullah.catmap.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class MesajFotoAdapter extends RecyclerView.Adapter<MesajFotoAdapter.FotoViewHolder> {
    private ArrayList<String> fotoListesi = new ArrayList<>();
    private Context context;
    private HashMap<String, Bitmap> mesajlasmaFotolari = new HashMap<>();

    public MesajFotoAdapter(Context context,ArrayList<String> fotoListesi) {
        this.context = context;
        this.fotoListesi = fotoListesi;
    }

    @NonNull
    @Override
    public FotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.mesajlasma_foto_itemleri, parent, false);
        return new FotoViewHolder(view);
    }


    @Override
    public void onBindViewHolder(@NonNull FotoViewHolder holder, int position) {
        String url = fotoListesi.get(position);
        if(mesajlasmaFotolari.containsKey(url)) {
            holder.imageView.setImageBitmap(mesajlasmaFotolari.get(url));
        }
        holder.fotoSayaci.setText(String.valueOf((position + 1) + " / " + this.fotoListesi.size()));
    }

    @Override
    public int getItemCount() {
        return fotoListesi.size();
    }

    class FotoViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        TextView fotoSayaci;

        public FotoViewHolder(@NonNull View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageFullScreen);
            fotoSayaci = itemView.findViewById(R.id.fotoSayaci);
        }
    }
    public void addFoto(String url ,Bitmap bitmap){
        mesajlasmaFotolari.put(url,bitmap);
        notifyDataSetChanged();
    }


}
