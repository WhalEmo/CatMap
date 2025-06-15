package com.emrullah.catmap;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import com.emrullah.catmap.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;


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
    public ArrayList<Yorum_Model> getYorumList() {
        return yorumList;
    }

    private Context context;
    private final Handler zamanHandler = new Handler();
    private final Runnable zamanRunnable = new Runnable() {
        @Override
        public void run() {
            for (int i = 0; i < yorumList.size(); i++) {
                Yorum_Model yorum = yorumList.get(i);
                if (yorum.getTarih() == null) continue;
                long fark = System.currentTimeMillis() - yorum.getTarih().getTime();

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

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String aktifKullaniciId = currentUser.getUid();

        if (MainActivity.kullanici.getID().equals(aktifKullaniciId)) {
            holder.menuButonu.setVisibility(View.VISIBLE);
        } else {
            holder.menuButonu.setVisibility(View.GONE);
        }

        if (yorum.isYanitlarGorunuyor()) {
            holder.container.setVisibility(View.VISIBLE);
            holder.yanitlariGor.setText("Yanıtları Gizle");


            ArrayList<Yanit_Model> yanitlar = yorum.getYanitlar();
            if (yanitlar == null) {
                yanitlar = new ArrayList<>();
                yorum.setYanitlar(yanitlar);
            }

            // ✨ SADECE BİR KERE ADAPTER OLUŞTUR
            if (yorum.getYanitAdapter() == null) {
                Yanit_Adapter yntadapter = new Yanit_Adapter(yanitlar, context);
                yorum.setYanitAdapter(yntadapter);
                yntadapter.baslatZamanlayici();
            }

            // Adapter'i bağla
            holder.recyclerViewyanitlar.setLayoutManager(new LinearLayoutManager(context));
            holder.recyclerViewyanitlar.setAdapter(yorum.getYanitAdapter());

            if (yorum.isYanitYokMu()) {
                holder.dahafazla.setVisibility(View.GONE);
                if (yorum.getYanitlar().size() > 0) {
                    holder.yanityoksa.setVisibility(View.GONE);
                } else {
                    holder.yanityoksa.setVisibility(View.VISIBLE);
                }
            } else {
                holder.yanityoksa.setVisibility(View.GONE);
                if (yorum.isDahafazlaGozukuyorMu()) {
                    holder.dahafazla.setVisibility(View.VISIBLE);
                } else {
                    holder.dahafazla.setVisibility(View.GONE);
                }
            }




            // 🔄 SADECE 1 KERE VERİ ÇEK
            if (!yorum.isYanitlarYuklendi()) {
                holder.dahafazla.setVisibility(View.GONE);
                holder.yanitlarYukleniyorLayout.setVisibility(View.VISIBLE);
                Yanitlari_Cekme yanitcek = new Yanitlari_Cekme();
                yanitcek.yanitlariCek(yorum, yanitlar, yorum.getYanitAdapter(), 5, true, new Yanitlari_Cekme.YanitlarCallback() {
                            @Override
                            public void onComplete() {
                                yorum.setYanitlarYuklendi(true);
                               yorum.setYanitYokMu(yorum.getYanitlar().isEmpty());
                                holder.yanitlarYukleniyorLayout.setVisibility(View.GONE);
                                notifyDataSetChanged();
                            }
                        });
            }

            ArrayList<Yanit_Model> finalYanitlar = yanitlar;
            holder.dahafazla.setOnClickListener(dahafz -> {
                holder.yanitlarYukleniyorLayout.setVisibility(View.VISIBLE);
                Yanitlari_Cekme yanitcek = new Yanitlari_Cekme();
                yanitcek.yanitlariCek(yorum, finalYanitlar, yorum.getYanitAdapter(), 5, false, new Yanitlari_Cekme.YanitlarCallback() {
                    @Override
                    public void onComplete() {
                        yorum.getYanitAdapter().notifyDataSetChanged(); // yeni gelen veriler için
                        holder.yanitlarYukleniyorLayout.setVisibility(View.GONE);
                        notifyItemChanged(position);
                    }
                });
            });

        } else {
            holder.container.setVisibility(View.GONE);
            holder.yanitlariGor.setText("Yanıtları Gör");
            holder.dahafazla.setVisibility(View.GONE);
        }

        holder.yanitlariGor.setOnClickListener(v -> {
            boolean gorunuyorMu = yorum.isYanitlarGorunuyor();
            yorum.setYanitlarGorunuyor(!gorunuyorMu);
            notifyItemChanged(position); // 🔄 Bu onBindViewHolder'ı tetikler
        });


        holder.yanitlamayiGetir.setOnClickListener(cvp -> {
            MapsActivity.textt.setText("@"+yorum.getKullaniciAdi());
            MapsActivity.textt.setSelection(MapsActivity.textt.getText().length());
            MapsActivity.kimeyanit.setHint(yorum.getKullaniciAdi() + " 'e yanıt veriyorsun");
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
                yorumindeks = position;
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
            notifyItemChanged(position);
            yorumList.get(position).setYanitlarGorunuyor(true);

        });


        holder.menuButonu.setOnClickListener(menu->{
            Yorum_Silme_Guncelleme islem=new Yorum_Silme_Guncelleme();
            PopupMenu popupmenu=new PopupMenu(context,holder.menuButonu);
            popupmenu.getMenuInflater().inflate(R.menu.uc_nokta_menu,popupmenu.getMenu());

            popupmenu.setOnMenuItemClickListener(item->{
                int id = item.getItemId();
                if (id == R.id.menu_guncelle) {
                    islem.yorumGuncelleme(yorum, context);
                    notifyItemChanged(position);
                    return true;
                } else if (id == R.id.menu_sil) {
                    islem.yorumSil(yorum.getYorumID(), position, yorumList);
                    notifyItemRemoved(position);
                    return true;
                }
                return false;
            });
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
        TextView yanityoksa;
        LinearLayout yanitlarYukleniyorLayout;
        ImageView menuButonu;

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
            yanityoksa=itemView.findViewById(R.id.yanityok);
            yanitlarYukleniyorLayout = itemView.findViewById(R.id.yanitlarYukleniyorLayout);
            menuButonu=itemView.findViewById(R.id.menuButton);

        }
    }


}