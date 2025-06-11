package com.emrullah.catmap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;



import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;


public class Yorum_Adapter extends RecyclerView.Adapter<Yorum_Adapter.YorumViewHolder>  {
    private ArrayList<Yorum_Model>yorumList;
    private Context context;
    public Yorum_Adapter(ArrayList<Yorum_Model> yorumList, Context context) {
        this.yorumList = yorumList;
        this.context = context;
    }
    public static int yorumindeks;
    @NonNull
    @Override
    public YorumViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yorum_icin,parent,false);
        return new YorumViewHolder(view);
    }
    int pozisyon=-1;

    @Override
    public void onBindViewHolder(@NonNull YorumViewHolder holder, int position) {

        Yorum_Model yorum=yorumList.get(position);
        holder.kullaniciAditext.setText(yorum.getKullaniciAdi());
        holder.yorumText.setText(yorum.getYorumicerik());
        holder.yorumTarihiText.setText(yorum.duzenlenmisTarih());

        if (yorum.isYanitlarGorunuyor()) {
            holder.container.setVisibility(View.VISIBLE);
            holder.yanitlariGor.setText("Yanıtları Gizle");
        } else {
            holder.container.setVisibility(View.GONE);
            holder.yanitlariGor.setText("Yanıtları Gör");
        }

        holder.yanitlariGor.setOnClickListener(v -> {
            if (!yorum.isYanitlarGorunuyor()) {
                yorum.setYanitlarGorunuyor(true);
                holder.container.setVisibility(View.VISIBLE);
                holder.yanitlariGor.setText("Yanıtları Gizle");


                ArrayList<Yanit_Model> yanitlar = yorum.getYanitlar();
                if (yanitlar == null) {
                    yanitlar = new ArrayList<>();
                    yorum.setYanitlar(yanitlar);
                }

                Yanit_Adapter yntadapter = new Yanit_Adapter(yanitlar, context,yorum.getKullaniciAdi());
                holder.recyclerViewyanitlar.setLayoutManager(new LinearLayoutManager(context));
                holder.recyclerViewyanitlar.setAdapter(yntadapter);

                // Veritabanından yeni yanıtları çek (opsiyonel)
                Yanitlari_Cekme yanitcek = new Yanitlari_Cekme();
                yanitcek.yanitlariCek(yorum,yanitlar,yntadapter,5,true);
                holder.dahafazla.setVisibility(View.VISIBLE);

                ArrayList<Yanit_Model> finalYanitlar = yanitlar;
                holder.dahafazla.setOnClickListener(dahafz -> {
                    yanitcek.yanitlariCek(yorum, finalYanitlar,yntadapter,5,false);
                });


            } else {
                yorum.setYanitlarGorunuyor(false);
                holder.container.setVisibility(View.GONE);
                holder.yanitlariGor.setText("Yanıtları Gör");
                holder.dahafazla.setVisibility(View.GONE);
            }
        });



        holder.yanitlamayiGetir.setOnClickListener(cvp -> {
            int eskiPozisyon = pozisyon;

            if (pozisyon == position) {
                // Aynı butona tıklandıysa: kapat
                pozisyon = -1;
                if(MapsActivity.yorumicin.getVisibility()==View.GONE) {
                    MapsActivity.yorumicin.setVisibility(View.VISIBLE);
                }
                MapsActivity.ynticin.setVisibility(View.GONE);
            } else {
                // Yeni yorum seçildi: göster
                pozisyon = position;
                yorumindeks = position;
                if(MapsActivity.yorumicin.getVisibility()==View.VISIBLE) {
                    MapsActivity.yorumicin.setVisibility(View.GONE);
                }
                MapsActivity.ynticin.setVisibility(View.VISIBLE);
            }

            // notifyItemChanged pozisyonları güncelle
            if (eskiPozisyon != -1) {
                notifyItemChanged(eskiPozisyon);
            }
            notifyItemChanged(position);
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
        TextView yanitlamayiGetir;
        RecyclerView recyclerViewyanitlar;
        TextView dahafazla;
        LinearLayout container;

        public YorumViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAditext=itemView.findViewById(R.id.kullaniciAdiTextView);
            yorumText=itemView.findViewById(R.id.yorumTextView);
            yorumTarihiText=itemView.findViewById(R.id.tarihTextView);
            yanitlariGor=itemView.findViewById(R.id.yanitlariGorTextView);
            yanitlamayiGetir=itemView.findViewById(R.id.yanitGosterTextView);
            recyclerViewyanitlar=itemView.findViewById(R.id.yanitlarRecyclerView);
            dahafazla=itemView.findViewById(R.id.dahaFazlaYanitText);
            container=itemView.findViewById(R.id.yanitlarContainer);
        }
    }


}
