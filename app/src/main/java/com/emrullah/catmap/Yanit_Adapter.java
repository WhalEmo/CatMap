package com.emrullah.catmap;

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

    String sbt="@beyzoo ";
    @Override
    public void onBindViewHolder(@NonNull Yanit_Adapter.YanitViewHolder holder, int position) {
        Yanit_Model yanit=yanitListe.get(position);
        holder.kullaniciAditext.setText(yanit.getAdi());
        holder.yanitText.setText(yanit.getYaniticerik());
        holder.yanitTarihiText.setText(yanit.getTarih().toString());

    }

    @Override
    public int getItemCount() {

        return yanitListe.size();
    }
    public static class YanitViewHolder extends RecyclerView.ViewHolder{
        TextView kullaniciAditext;
        TextView yanitText;
        TextView yanitTarihiText;
        LinearLayout yanitlaricinLayout;
        TextView yanitlamayiGetir;
        EditText yazdigimyanit;
        Button gonderbuton;
        public YanitViewHolder(@NonNull View itemView) {
            super(itemView);
            kullaniciAditext=itemView.findViewById(R.id.kullaniciAdiTextView);
            yanitText=itemView.findViewById(R.id.yorumTextView);
            yanitTarihiText=itemView.findViewById(R.id.tarihTextView);
            yanitlaricinLayout=itemView.findViewById(R.id.yanitEkleLayout);
            yanitlamayiGetir=itemView.findViewById(R.id.yanitGosterTextView);
            yazdigimyanit=itemView.findViewById(R.id.yanitEditText);
            gonderbuton=itemView.findViewById(R.id.yntyorumgonder);
        }
    }
}

