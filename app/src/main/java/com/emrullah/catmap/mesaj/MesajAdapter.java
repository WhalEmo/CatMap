package com.emrullah.catmap.mesaj;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;

import java.util.ArrayList;

public class
MesajAdapter extends RecyclerView.Adapter<MesajAdapter.MesajViewHolder> {

    private ArrayList<Mesaj> mesajArrayList;
    private Context context;
    private PopupWindow popupWindow;
    private ImageButton btnMesajGuncelle;
    private ImageButton btnMesajSil;
    private MesajDuzenlePopup mesajDuzenlePopup;

    public ArrayList<Mesaj> getMesajArrayList() {
        return mesajArrayList;
    }

    public MesajAdapter(ArrayList<Mesaj> mesajArrayList, Context context) {
        this.mesajArrayList = mesajArrayList;
        this.context = context;
        mesajDuzenlePopup = new MesajDuzenlePopup();
        mesajDuzenlePopup.Olustur(context);
        MesajSecenkMenu();
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
            if(mesaj.isGoruldu()){
                holder.gorulmeIkon.setImageResource(R.drawable.patidolu);
            }
            else{
                holder.gorulmeIkon.setImageResource(R.drawable.patibos);
            }
        }
        else{
            holder.solMesajLayout.setVisibility(View.VISIBLE);
            holder.sagMesajLayout.setVisibility(View.GONE);
            holder.solMesajText.setText(mesaj.getMesaj().trim());
            holder.solZaman.setText(mesaj.getZaman());
        }
        holder.sagMesajLayout.setOnLongClickListener(v -> {
            SilmeGuncellemeGoster(v);
            Sil(mesaj);
            Guncelle(mesaj,v);

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return mesajArrayList.size();
    }

    private void SilmeGuncellemeGoster(View item){
        popupWindow.showAsDropDown(item, -50, -item.getHeight() - 20);
    }

    private void MesajSecenkMenu(){
        View secenekMenu = LayoutInflater.from(context)
                .inflate(R.layout.mesaj_secenek_menu,null);
        btnMesajGuncelle = secenekMenu.findViewById(R.id.btnMesajGuncelle);
        btnMesajSil = secenekMenu.findViewById(R.id.btnMesajSil);
        popupWindow = new PopupWindow(
                secenekMenu,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                true
        );
        popupWindow.setOutsideTouchable(true);
        popupWindow.setElevation(10);
    }
    public void Guncelle(Mesaj mesaj, View item){
        btnMesajGuncelle.setOnClickListener(v ->{
            popupWindow.dismiss();
            mesajDuzenlePopup.Goster(item,mesaj); // --> mesajlasma yoneticisi bu nesnenin içinde işlemler yapıyor
            notifyItemChanged(mesajArrayList.indexOf(mesaj));
        });
    }
    public void Sil(Mesaj mesaj){
        btnMesajSil.setOnClickListener(v ->{
            MesajlasmaYonetici.getInstance().MesajSil(mesaj.getMesajID());
            popupWindow.dismiss();
    });
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
