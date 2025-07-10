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

import com.emrullah.catmap.R;

import java.util.ArrayList;
import java.util.List;

public class MesajFotoAdapter extends RecyclerView.Adapter<MesajFotoAdapter.FotoViewHolder> {
    private ArrayList<Bitmap> fotoListesi;
    private Context context;
    private int ADET;

    public MesajFotoAdapter(Context context, ArrayList<Bitmap> fotoListesi) {
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
        Bitmap bitmap = fotoListesi.get(position);
        holder.imageView.setImageBitmap(bitmap);
        holder.fotoSayaci.setText(String.valueOf((position + 1) + " / " + this.ADET));
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
    public void addFoto(Bitmap bitmap){
        fotoListesi.add(bitmap);
        notifyItemInserted(fotoListesi.size()-1);
    }

    public void setADET(int ADET) {
        this.ADET = ADET;
    }

    public int getADET() {
        return ADET;
    }
}
