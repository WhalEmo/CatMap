package com.emrullah.catmap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Locale;

public class Yorum_Adapter extends RecyclerView.Adapter<Yorum_Adapter.YorumViewHolder>  {
    private ArrayList<Yorum_Model>yorumList;
    private Context context;

    public Yorum_Adapter(ArrayList<Yorum_Model> yorumList, Context context) {
        this.yorumList = yorumList;
        this.context = context;
    }

    @NonNull
    @Override
    public YorumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yorum_icin,parent,false);
        return new YorumViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull YorumViewHolder holder, int position) {
        Yorum_Model yorum=yorumList.get(position);

        holder.kullaniciAditext.setText(yorum.getKullaniciAdi());
        holder.yorumText.setText(yorum.getYorumicerik());

        holder.yorumTarihiText.setText(yorum.duzenlenmisTarih());

        holder.yanitlariGor.setOnClickListener(yant->{
          if(holder.recyclerView.getVisibility()==View.GONE){
              holder.recyclerView.setVisibility(View.VISIBLE);
          }
        });
        holder.yanitlamayiGetir.setOnClickListener(cvp->{
            if(holder.yanitlaricinLayout.getVisibility()==View.GONE){
                holder.yanitlaricinLayout.setVisibility(View.VISIBLE);

            }
        });

    }

    @Override
    public int getItemCount() {
        return yorumList.size();
    }

    public static class YorumViewHolder extends RecyclerView.ViewHolder{
        TextView kullaniciAditext;
        TextView yorumText;
        TextView yorumTarihiText;
        TextView yanitlariGor;
        LinearLayout yanitlaricinLayout;
        TextView yanitlamayiGetir;
        EditText yazdigimyanit;
        RecyclerView recyclerView;

        public YorumViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAditext=itemView.findViewById(R.id.kullaniciAdiTextView);
            yorumText=itemView.findViewById(R.id.yorumTextView);
            yorumTarihiText=itemView.findViewById(R.id.tarihTextView);
            yanitlariGor=itemView.findViewById(R.id.yanitlariGorTextView);
            yanitlaricinLayout=itemView.findViewById(R.id.yanitEkleLayout);
            yanitlamayiGetir=itemView.findViewById(R.id.yanitGosterTextView);
            yazdigimyanit=itemView.findViewById(R.id.yanitEditText);
            recyclerView=itemView.findViewById(R.id.yanitlarRecyclerView);
        }
    }


}
