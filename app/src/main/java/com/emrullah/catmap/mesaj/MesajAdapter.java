package com.emrullah.catmap.mesaj;

import android.content.Context;
import android.view.FocusFinder;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.emrullah.catmap.MainActivity;
import com.emrullah.catmap.R;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;

public class
MesajAdapter extends RecyclerView.Adapter<MesajAdapter.MesajViewHolder> {

    private ArrayList<Mesaj> mesajArrayList;
    private Context context;
    private PopupWindow popupWindow;
    private ImageButton btnMesajGuncelle;
    private ImageButton btnMesajSil;
    private RecyclerView recyclerView;
    private MesajDuzenlePopup mesajDuzenlePopup;
    private Runnable goster;
    Animation anim;

    public ArrayList<Mesaj> getMesajArrayList() {
        return mesajArrayList;
    }

    public MesajAdapter(ArrayList<Mesaj> mesajArrayList, Context context, RecyclerView recyclerView) {
        this.mesajArrayList = mesajArrayList;
        this.context = context;
        mesajDuzenlePopup = new MesajDuzenlePopup();
        mesajDuzenlePopup.Olustur(context);
        this.recyclerView = recyclerView;
        MesajSecenkMenu();
        anim = AnimationUtils.loadAnimation(context, R.anim.mesaj_anim);
    }

    @NonNull
    @Override
    public MesajViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.mesaj,parent,false);
        return new MesajAdapter.MesajViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MesajViewHolder holder, int position) {
        holder.solMesajLayout.setVisibility(View.GONE);
        holder.sagMesajLayout.setVisibility(View.GONE);
        holder.solFotoLayout.setVisibility(View.GONE);
        holder.sagFotoLayout.setVisibility(View.GONE);
        holder.sagMesajText.setVisibility(View.GONE);
        holder.solMesajText.setVisibility(View.GONE);
        holder.SolcevapKutusu.setVisibility(View.GONE);
        holder.cevapKutusu.setVisibility(View.GONE);


        Mesaj mesaj = mesajArrayList.get(position);

        if(mesaj.isYaniyorMu()){
            holder.itemView.startAnimation(anim);
            mesaj.setYaniyorMu(false);
        }

        if(mesaj instanceof YanitMesaj){
            YanitMesaj yanitMesaj = (YanitMesaj) mesaj;
            if(yanitMesaj.getGonderici().equals(MainActivity.kullanici.getID())){
                holder.cevapKutusu.setVisibility(View.VISIBLE);
                holder.cevapMetni.setText(yanitMesaj.getYanitlananMesaj().getMesaj());
                holder.cevapKutusu.setOnClickListener(v->{
                    Scroll(yanitMesaj);
                });
            }
            else{
                holder.SolcevapKutusu.setVisibility(View.VISIBLE);
                holder.SolcevapMetni.setText(yanitMesaj.getYanitlananMesaj().getMesaj());
                holder.SolcevapKutusu.setOnClickListener(v->{
                    Scroll(yanitMesaj);
                });
            }
        }
        if(!mesaj.getTur().equals("foto")) {
            if (mesaj.getGonderici().equals(MainActivity.kullanici.getID())) {
                holder.solMesajLayout.setVisibility(View.GONE);
                holder.sagMesajLayout.setVisibility(View.VISIBLE);
                holder.sagMesajText.setVisibility(View.VISIBLE);
                holder.sagMesajText.setText(mesaj.getMesaj().trim());
                holder.sagZaman.setText(mesaj.getStringZaman());
                if (mesaj.isGoruldu()) {
                    holder.gorulmeIkon.setImageResource(R.drawable.patidolu);
                } else {
                    holder.gorulmeIkon.setImageResource(R.drawable.patibos);
                }
            } else {
                holder.solMesajLayout.setVisibility(View.VISIBLE);
                holder.sagMesajLayout.setVisibility(View.GONE);
                holder.solMesajText.setVisibility(View.VISIBLE);
                holder.solMesajText.setText(mesaj.getMesaj().trim());
                holder.solZaman.setText(mesaj.getStringZaman());
            }
        }
        else {
            // burası fotograf mesajları için
            if (mesaj.getGonderici().equals(MainActivity.kullanici.getID())) {
                holder.sagMesajLayout.setVisibility(View.VISIBLE);
                holder.solMesajLayout.setVisibility(View.GONE);
                holder.sagFotoLayout.setVisibility(View.VISIBLE);
                holder.sagZaman.setText(mesaj.getStringZaman());
                if (mesaj.isGoruldu()) {
                    holder.gorulmeIkon.setImageResource(R.drawable.patidolu);
                } else {
                    holder.gorulmeIkon.setImageResource(R.drawable.patibos);
                }
                if(mesaj.isYuklendiMi()){
                    holder.sagGeciciFoto.setVisibility(View.GONE);
                    MesajFotoGonderYonetici.getInstance().FotoMesaj(true,holder,mesaj,context,goster);
                }
            }
            else {
                holder.sagMesajLayout.setVisibility(View.GONE);
                holder.solMesajLayout.setVisibility(View.VISIBLE);
                holder.solZaman.setText(mesaj.getStringZaman());
                MesajFotoGonderYonetici.getInstance().FotoMesaj(false,holder,mesaj,context,goster);
            }

        }
        holder.sagMesajLayout.setOnLongClickListener(v -> {
            SilmeGuncellemeGoster(v);
            Sil(mesaj);
            Guncelle(mesaj,v);

            return true;
        });
        holder.sagFotoLayout.setOnClickListener(v ->{

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
            if (mesaj.getTur().equals("foto")) return;
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

        GridLayout solFotoLayout;
        GridLayout sagFotoLayout;

        ImageView gorulmeIkon;

        LinearLayout sagMesajLayout;
        TextView sagMesajText;
        TextView sagZaman;

        LinearLayout cevapKutusu;
        TextView cevapMetni;
        LinearLayout SolcevapKutusu;
        TextView SolcevapMetni;

        ImageView sagGeciciFoto;

        public MesajViewHolder(@NonNull View itemView) {
            super(itemView);
            solMesajLayout = itemView.findViewById(R.id.solMesajLayout);
            solMesajText = itemView.findViewById(R.id.solMesajText);
            solZaman = itemView.findViewById(R.id.solZaman);

            solFotoLayout = itemView.findViewById(R.id.solFotoLayout);
            sagFotoLayout = itemView.findViewById(R.id.sagFotoLayout);

            gorulmeIkon = itemView.findViewById(R.id.gorulmeIkon);

            sagMesajLayout = itemView.findViewById(R.id.sagMesajLayout);
            sagMesajText = itemView.findViewById(R.id.sagMesajText);
            sagZaman = itemView.findViewById(R.id.sagZaman);

            cevapKutusu = itemView.findViewById(R.id.cevapKutusu);
            cevapMetni = itemView.findViewById(R.id.cevapMetni);
            SolcevapKutusu = itemView.findViewById(R.id.SolcevapKutusu);
            SolcevapMetni = itemView.findViewById(R.id.SolcevapMetni);

            sagGeciciFoto = itemView.findViewById(R.id.sagplaceholder);

        }
    }
    public void Scroll(YanitMesaj yanitMesaj){
        for(int i=0; i<mesajArrayList.size(); i++){
            if(mesajArrayList.get(i).getMesajID().equals(yanitMesaj.getYanitlananMesaj().getMesajID())){
                mesajArrayList.get(i).setYaniyorMu(true);
                notifyItemChanged(i);
                recyclerView.scrollToPosition(i);
                break;
            }
        }
    }

    public void setGoster(Runnable goster) {
        this.goster = goster;
    }
}
