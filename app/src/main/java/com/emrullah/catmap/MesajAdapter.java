package com.emrullah.catmap;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class
MesajAdapter extends RecyclerView.Adapter<MesajAdapter.MesajViewHolder> {

    private ArrayList<Mesaj> mesajArrayList;
    private Context context;

    public ArrayList<Mesaj> getMesajArrayList() {
        return mesajArrayList;
    }

    public MesajAdapter(ArrayList<Mesaj> mesajArrayList, Context context) {
        this.mesajArrayList = mesajArrayList;
        this.context = context;
    }

    @NonNull
    @Override
    public MesajViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.mesaj,parent,false);
        return new MesajAdapter.MesajViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MesajViewHolder holder, int position) {
        Mesaj mesaj = mesajArrayList.get(position);
        if(mesaj.getGonderici().equals(MainActivity.kullanici.getID())){
            holder.solMesajLayout.setVisibility(View.GONE);
            holder.sagMesajLayout.setVisibility(View.VISIBLE);
            holder.sagMesajText.setText(mesaj.getMesaj().trim());
            holder.sagZaman.setText(mesaj.getZaman());
        }
        else{
            holder.solMesajLayout.setVisibility(View.VISIBLE);
            holder.sagMesajLayout.setVisibility(View.GONE);
            holder.solMesajText.setText(mesaj.getMesaj().trim());
            holder.solZaman.setText(mesaj.getZaman());
            if(mesaj.isGoruldu()){
                holder.gorulmeIkon.setImageResource(R.drawable.patidolu);
            }
            else{
                holder.gorulmeIkon.setImageResource(R.drawable.patibos);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mesajArrayList.size();
    }

    public static class MesajViewHolder extends RecyclerView.ViewHolder{
        LinearLayout solMesajLayout;
        TextView solMesajText;
        TextView solZaman;

        ImageView gorulmeIkon;

        LinearLayout sagMesajLayout;
        TextView sagMesajText;
        TextView sagZaman;

        public MesajViewHolder(@NonNull View itemView) {
            super(itemView);
            solMesajLayout = itemView.findViewById(R.id.solMesajLayout);
            solMesajText = itemView.findViewById(R.id.solMesajText);
            solZaman = itemView.findViewById(R.id.solZaman);

            gorulmeIkon = itemView.findViewById(R.id.gorulmeIkon);

            sagMesajLayout = itemView.findViewById(R.id.sagMesajLayout);
            sagMesajText = itemView.findViewById(R.id.sagMesajText);
            sagZaman = itemView.findViewById(R.id.sagZaman);
        }
    }
}
