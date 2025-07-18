package com.beem.catmap.sohbet;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.beem.catmap.mesaj.MesajlasmaYonetici;
import com.beem.catmap.R;

import java.util.ArrayList;

public class SohbetAdapter extends RecyclerView.Adapter<SohbetAdapter.SohbetViewHolder> {
    private ArrayList<Sohbet> sohbetArrayList;
    private Context context;
    private Runnable MesajFragment;


    public SohbetAdapter(ArrayList<Sohbet> sohbetArrayList, Context context, Runnable MesajFragment) {
        this.sohbetArrayList = sohbetArrayList;
        this.context = context;
        this.MesajFragment = MesajFragment;
    }

    @NonNull
    @Override
    public SohbetAdapter.SohbetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.sohbet_kutusu,parent,false);
        return new SohbetAdapter.SohbetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SohbetAdapter.SohbetViewHolder holder, int position) {
        Sohbet sohbet = sohbetArrayList.get(position);
        holder.kisi_adi.setText(sohbet.getAlici().getKullaniciAdi());
        if(sohbet.getMesaj() != null){
            holder.son_mesaj.setText(sohbet.getMesaj().getMesaj());
            holder.mesaj_saat.setText(sohbet.getMesaj().getStringZaman());
        }
        if(sohbet.getAlici().getFotoBitmap() != null && !sohbet.isEngelliSohbetMi()){
            holder.kisi_foto.setImageBitmap(sohbet.getAlici().getFotoBitmap());
        }
        else {
            holder.kisi_foto.setImageResource(R.drawable.kullanici);
        }
        holder.sohbet_kutu.setOnClickListener(v->{
            MesajlasmaYonetici.getInstance().setAlici(sohbet.getAlici());
            MesajlasmaYonetici.getInstance().setSohbetID(sohbet.getSohbetID());
            MesajlasmaYonetici.getInstance().DinleyiciKaldir();
            SohbetYonetici.getInstance().DinleyicileriKaldir(sohbetArrayList);
            this.MesajFragment.run();
        });

        if(sohbet.getOkunmamisMesajSayisi() != 0){
            holder.okunmamis_sayac.setVisibility(View.VISIBLE);
            if(sohbet.getOkunmamisMesajSayisi()>=99){
                holder.okunmamis_sayac.setText("99+");
            }
            else {
                holder.okunmamis_sayac.setText(String.valueOf(sohbet.getOkunmamisMesajSayisi()));
            }
            holder.son_mesaj.setTypeface(null, Typeface.BOLD);
            holder.son_mesaj.setTextColor(Color.parseColor("#13216E"));


            holder.mesaj_saat.setTextColor(Color.parseColor("#555555"));
        }
        else{
            holder.okunmamis_sayac.setVisibility(View.GONE);

            holder.son_mesaj.setTypeface(null, Typeface.NORMAL);
            holder.son_mesaj.setTextColor(Color.parseColor("#555555"));

            holder.mesaj_saat.setTextColor(Color.parseColor("#888888"));
        }
    }

    @Override
    public int getItemCount() {
        return sohbetArrayList.size();
    }

    public static class SohbetViewHolder extends RecyclerView.ViewHolder{
        ImageView kisi_foto;
        TextView son_mesaj;
        TextView mesaj_saat;
        TextView kisi_adi;
        TextView okunmamis_sayac;
        ConstraintLayout sohbet_kutu;

        public SohbetViewHolder(@NonNull View itemView) {
            super(itemView);
            kisi_foto = itemView.findViewById(R.id.kisi_foto);
            son_mesaj = itemView.findViewById(R.id.son_mesaj);
            mesaj_saat = itemView.findViewById(R.id.mesaj_saat);
            kisi_adi = itemView.findViewById(R.id.kisi_adi);
            sohbet_kutu = itemView.findViewById(R.id.sohbet_kutu);
            okunmamis_sayac = itemView.findViewById(R.id.okunmamis_sayac);
        }
    }
}
