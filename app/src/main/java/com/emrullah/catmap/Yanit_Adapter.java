package com.emrullah.catmap;

import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;

public class Yanit_Adapter extends RecyclerView.Adapter<Yanit_Adapter.YanitViewHolder> {
    private ArrayList<Yanit_Model>yanitListe;
    private Context context;
    private final Handler zamanHandler = new Handler();
    private final Runnable zamanRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < yanitListe.size(); i++) {
                Yanit_Model yanit = yanitListe.get(i);
                if (yanit.getTarih() == null) continue;
                long fark = System.currentTimeMillis() - yanit.getTarih().getTime();

                // Sadece 1 saatten küçük yorumlar için yenileme yap
                if (fark < 3600000 && fark >= 60000) {
                    notifyItemChanged(i);
                }
            }
            zamanHandler.postDelayed(this, 60000);
        }

    };
    public void baslatZamanlayici() {
        zamanHandler.post(zamanRunnable);
    }

    public void durdurZamanlayici() {
        zamanHandler.removeCallbacks(zamanRunnable);
    }

    public Yanit_Adapter(ArrayList<Yanit_Model> yanitListe, Context context) {
        this.yanitListe = yanitListe;
        this.context = context;
    }
    @NonNull
    @Override
    public Yanit_Adapter.YanitViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.herbi_yanit_icin,parent,false);
        return new Yanit_Adapter.YanitViewHolder(view);
    }
    int pozisyon=-1;


    @Override
    public void onBindViewHolder(@NonNull Yanit_Adapter.YanitViewHolder holder, int position) {
        Yanit_Model yanit=yanitListe.get(position);
        holder.kullaniciAditext.setText(yanit.getAdi());
        holder.yanitText.setText(yanit.getYaniticerik());
        holder.yanitTarihiText.setText(yanit.duzenlenmisTarih());

        holder.yanitlabutonu.setOnClickListener(cvp->{
            MapsActivity.textt.setText("@"+yanit.getAdi());
            MapsActivity.textt.setSelection(MapsActivity.textt.getText().length());
            MapsActivity.kimeyanit.setHint(yanit.getAdi() + " 'e yanıt veriyorsun");
            Klavye klavye=new Klavye(context);
            int eskiPozisyon = pozisyon;
            if (pozisyon == position) {
                // Aynı butona tıklandıysa: kapat
                pozisyon = -1;
                MapsActivity.yorumicin.setVisibility(View.GONE);
                MapsActivity.carpiicin.setVisibility(View.VISIBLE);
                MapsActivity.ynticin.setVisibility(View.VISIBLE);
            } else {
                // Yeni yorum seçildi: göster
                pozisyon = position;
                Yorum_Adapter.yorumindeks=position;
                MapsActivity.yorumicin.setVisibility(View.GONE);
                MapsActivity.carpiicin.setVisibility(View.VISIBLE);
                MapsActivity.ynticin.setVisibility(View.VISIBLE);
                new Handler().postDelayed(() -> {
                    klavye.klavyeAc(MapsActivity.textt);
                }, 250);
            }

            // notifyItemChanged pozisyonları güncelle
            if (eskiPozisyon != -1) {
                notifyItemChanged(eskiPozisyon);
            }
            if (position >= 0 && position < getItemCount()) {
                notifyItemChanged(position);
            }
        });
    }

    @Override
    public int getItemCount() {

        return yanitListe.size();
    }
    public static class YanitViewHolder extends RecyclerView.ViewHolder{
        TextView kullaniciAditext;
        TextView yanitText;
        TextView yanitTarihiText;
        TextView yanitlamayiGetir;
        TextView yanitlabutonu;
        public YanitViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAditext=itemView.findViewById(R.id.kullaniciAdiTextViewynt);
            yanitText=itemView.findViewById(R.id.yanittTextView);
            yanitTarihiText=itemView.findViewById(R.id.tarihTextView);
            yanitlamayiGetir=itemView.findViewById(R.id.yanitGosterTextView);
            yanitlabutonu=itemView.findViewById(R.id.yanitlayazisiynt);
        }
    }
}

