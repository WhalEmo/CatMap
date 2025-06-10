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
    int gosterilenYanitSayisi;

    @Override
    public void onBindViewHolder(@NonNull YorumViewHolder holder, int position) {

        Yorum_Model yorum=yorumList.get(position);
        holder.kullaniciAditext.setText(yorum.getKullaniciAdi());
        holder.yorumText.setText(yorum.getYorumicerik());
        holder.yorumTarihiText.setText(yorum.duzenlenmisTarih());

        holder.yanitlariGor.setOnClickListener(v -> {
            if (holder.recyclerViewyanitlar.getVisibility() == View.GONE) {
                holder.recyclerViewyanitlar.setVisibility(View.VISIBLE);
                holder.yanitlariGor.setText("Yanıtları Gizle");


                ArrayList<Yanit_Model> yanitlar = yorum.getYanitlar();
                if (yanitlar == null) {
                    yanitlar = new ArrayList<>();
                }
                // Veritabanından yeni yanıtları çek (opsiyonel)
                Yanitlari_Cekme yanitcek = new Yanitlari_Cekme();
                yanitcek.yanitlariCek(yorum,yanitlar);

                Yanit_Adapter yntadapter = new Yanit_Adapter(yanitlar, context);
                holder.recyclerViewyanitlar.setLayoutManager(new LinearLayoutManager(context));
                holder.recyclerViewyanitlar.setAdapter(yntadapter);



/*


                // "Daha Fazla" butonu ayarı
                if (gosterilenYanitSayisi < toplamyanitsayisi) {
                    holder.dahafazla.setVisibility(View.VISIBLE);
                } else {
                    holder.dahafazla.setVisibility(View.GONE);
                }

                holder.dahafazla.setOnClickListener(dahafz -> {
                    int yenigosterilen = gosterilenYanitSayisi + 5;
                    if (yenigosterilen > toplamyanitsayisi) {
                        yenigosterilen = toplamyanitsayisi;
                    }
                    gosterilenYanitSayilari.put(position, yenigosterilen);
                    notifyItemChanged(position);
                });

*/
            } else {
                holder.recyclerViewyanitlar.setVisibility(View.GONE);
                holder.yanitlariGor.setText("Yanıtları Gör");
            }
        });

        if(pozisyon==position){
            holder.yanitlaricinLayout.setVisibility(View.VISIBLE);
            MapsActivity.yorumicin.setVisibility(View.GONE);
        }else{
            holder.yanitlaricinLayout.setVisibility(View.GONE);
        }

       holder.yanitlamayiGetir.setOnClickListener(cvp->{
           if(pozisyon==position){
               MapsActivity.yorumicin.setVisibility(View.VISIBLE);
               pozisyon=-1;
           }else {
               pozisyon=position;
               yorumindeks=position;
           }
           notifyDataSetChanged();
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
        RecyclerView recyclerViewyanitlar;
        TextView dahafazla;

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
            recyclerViewyanitlar=itemView.findViewById(R.id.yanitlarRecyclerView);
            dahafazla=itemView.findViewById(R.id.dahaFazlaYanitText);
        }
    }


}
